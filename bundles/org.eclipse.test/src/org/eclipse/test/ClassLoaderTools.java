/*******************************************************************************
 * Copyright (c) 2018, 2022 Red Hat Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Lucas Bullen (Red Hat Inc.) - initial API and implementation
 *******************************************************************************/
package org.eclipse.test;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleWiring;

class ClassLoaderTools {

	public static ClassLoader getPluginClassLoader(Bundle bundle, ClassLoader fallback) {
		// The fallback classloader is used in order to make ServiceLoader capable of
		// loading
		// services defined in JUnit bundles but not visible in the test bundle
		// classloader
		// (which will then be used by SuiteLauncher to lookup other JUnit engines the
		// test
		// is not aware of).
		return new ClassLoader(bundle.adapt(BundleWiring.class).getClassLoader()) {
			@Override
			public Enumeration<URL> getResources(String name) throws IOException {
				Enumeration<URL> supr = super.getResources(name);
				return supr != null && supr.hasMoreElements() ? supr
						: fallback.getResources(name);
			}

			@Override
			public Class<?> loadClass(String name) throws ClassNotFoundException {
				try {
					return super.loadClass(name);
				} catch (ClassNotFoundException ex) {
					return fallback.loadClass(name);
				}
			}
		};
	}

	static Bundle getTestBundle(String pluginName, String className) {
		if (pluginName != null) {
			Bundle bundle = Platform.getBundle(pluginName);
			if (bundle != null) {
				return bundle;
			}
		} else if (className != null) {
			for (int index = className.lastIndexOf('.'); index != -1; index = className.lastIndexOf('.', index - 1)) {
				String symbolicName = className.substring(0, index);
				Bundle bundle = Platform.getBundle(symbolicName);
				if (bundle != null) {
					return bundle;
				}
			}
		}
		throw new IllegalArgumentException("Bundle \"" + pluginName
				+ "\" not found. Possible causes include missing dependencies, too restrictive version ranges, or a non-matching required execution environment."); //$NON-NLS-1$
	}

	static ClassLoader getJUnit5Classloader(List<Bundle> platformEngineBundles) {
		return new MultiBundleClassLoader(platformEngineBundles);
	}


	private static class MultiBundleClassLoader extends ClassLoader {
		private final List<Bundle> bundleList;

		public MultiBundleClassLoader(List<Bundle> platformEngineBundles) {
			this.bundleList = platformEngineBundles;
		}

		@Override
		protected Class<?> findClass(String name) throws ClassNotFoundException {
			for (Bundle bundle : bundleList) {
				try {
					Class<?> c = bundle.loadClass(name);
					if (c != null) {
						return c;
					}
				} catch (ClassNotFoundException e) {
				}
			}
			return null;
		}

		@Override
		protected URL findResource(String name) {
			for (Bundle bundle : bundleList) {
				URL url = bundle.getResource(name);
				if (url != null) {
					return url;
				}
			}
			return null;
		}

		@Override
		protected Enumeration<URL> findResources(String name) throws IOException {
			List<URL> resources = new ArrayList<>();
			for (Bundle bundle : bundleList) {
				Enumeration<URL> bundleResources = bundle.getResources(name);
				while (bundleResources != null && bundleResources.hasMoreElements()) {
					resources.add(bundleResources.nextElement());
				}
			}
			return Collections.enumeration(resources);
		}
	}
}
