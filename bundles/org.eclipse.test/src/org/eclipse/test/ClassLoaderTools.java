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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.internal.framework.EquinoxBundle;
import org.osgi.framework.Bundle;

@SuppressWarnings("restriction")
class ClassLoaderTools {
	public static ClassLoader getPluginClassLoader(String getfTestPluginName, ClassLoader currentTCCL) {
		Bundle bundle = Platform.getBundle(getfTestPluginName);
		if (bundle == null) {
			throw new IllegalArgumentException("Bundle \"" + getfTestPluginName //$NON-NLS-1$
					+ "\" not found. Possible causes include missing dependencies, too restrictive version ranges, or a non-matching required execution environment."); //$NON-NLS-1$
		}
		return new TestBundleClassLoader(bundle, currentTCCL);
	}

	public static String getClassPlugin(String className) {
		int index = className.lastIndexOf('.');
		String plugin = null;
		while (index != -1) {
			plugin = className.substring(0, index);
			if (Platform.getBundle(plugin) != null) {
				break;
			}
			index = className.lastIndexOf('.', index - 1);
		}
		return plugin;
	}

	public static ClassLoader getJUnit5Classloader(List<String> platformEngine) {
		List<Bundle> platformEngineBundles = new ArrayList<>();
		for (String string : platformEngine) {
			Bundle bundle = Platform.getBundle(string);
			platformEngineBundles.add(bundle);
		}
		return new MultiBundleClassLoader(platformEngineBundles);
	}

	private static class TestBundleClassLoader extends ClassLoader {
		protected Bundle bundle;
		protected ClassLoader currentTCCL;

		public TestBundleClassLoader(Bundle target, ClassLoader currentTCCL) {
			this.bundle = target;
			this.currentTCCL = currentTCCL;
		}

		@Override
		protected Class<?> findClass(String name) throws ClassNotFoundException {
			try {
				return bundle.loadClass(name);
			} catch (ClassNotFoundException e) {
				return currentTCCL.loadClass(name);
			}
		}

		@Override
		protected URL findResource(String name) {
			URL url = bundle.getResource(name);
			if (url == null) {
				url = currentTCCL.getResource(name);
			}
			return url;
		}

		@Override
		protected Enumeration<URL> findResources(String name) throws IOException {
			Enumeration<URL> enumeration = bundle.getResources(name);
			if (enumeration == null) {
				enumeration = currentTCCL.getResources(name);
			}
			return enumeration;
		}

		@Override
		public Enumeration<URL> getResources(String res) throws IOException {
			Enumeration<URL> urls = currentTCCL.getResources(res);
			if (urls.hasMoreElements()) {
				return urls;
			}
			String location = null;
			if (bundle instanceof EquinoxBundle) {
				location = ((EquinoxBundle) bundle).getLocation();
			}
			URL url = null;
			if (location != null && location.startsWith("reference:")) { //$NON-NLS-1$
				location = location.substring(10, location.length());
				URI uri = URI.create(location);
				String newPath = (uri.getPath() == null ? "" : uri.getPath()) + "bin" + '/' + res; //$NON-NLS-1$
				URI newUri = uri.resolve(newPath).normalize();
				if (newUri.isAbsolute()) {
					url = newUri.toURL();
				}
			}
			List<URL> resources = new ArrayList<>(6);
			if (url != null) {
				if (new File(url.getFile()).exists()) {
					resources.add(url);
				}
			} else {
				return Collections.emptyEnumeration();
			}
			return Collections.enumeration(resources);
		}
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
