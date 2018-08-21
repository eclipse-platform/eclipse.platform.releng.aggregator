/*******************************************************************************
 * Copyright (c) 2018 Red Hat Inc. and others.
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
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.internal.framework.EquinoxBundle;
import org.osgi.framework.Bundle;

@SuppressWarnings("restriction")
class ClassLoaderTools {
	public static ClassLoader getPluginClassLoader(String getfTestPluginName, ClassLoader currentTCCL) {
		Bundle bundle = Platform.getBundle(getfTestPluginName);
		if (bundle == null) {
			throw new IllegalArgumentException("Bundle \"" + getfTestPluginName + "\" not found. Possible causes include missing dependencies, too restrictive version ranges, or a non-matching required execution environment."); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return new TestBundleClassLoader(bundle, currentTCCL);
	}

	public static String getClassPlugin(String className) {
		int index = className.lastIndexOf('.');
		String plugin = null;
		while (index != -1) {
			plugin = className.substring(0, index);
			if(Platform.getBundle(plugin) != null) {
				break;
			}
			index = className.lastIndexOf('.', index-1);
		}
		return plugin;
	}

	public static ClassLoader getJUnit5Classloader(List<String> platformEngine) {
		List<Bundle> platformEngineBundles = new ArrayList<>();
		for (Iterator<String> iterator = platformEngine.iterator(); iterator.hasNext();) {
			String string = iterator.next();
			Bundle bundle = Platform.getBundle(string);
			platformEngineBundles.add(bundle);
		}
		return new MultiBundleClassLoader(platformEngineBundles);
	}

	static class TestBundleClassLoader extends ClassLoader {
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
			if(url == null) {
				url = currentTCCL.getResource(name);
			}
			return url;
		}

		@SuppressWarnings({"rawtypes", "unchecked"})
		@Override
		protected Enumeration findResources(String name) throws IOException {
			Enumeration enumeration = bundle.getResources(name);
			if(enumeration == null) {
				enumeration = currentTCCL.getResources(name);
			}
			return enumeration;
		}

		@Override
		public Enumeration<URL> getResources(String res) throws IOException {
			Enumeration<URL> urls = currentTCCL.getResources(res);
			if(urls.hasMoreElements())
				return urls;

			List<URL> resources = new ArrayList<>(6);
			String location = null;
			URL url = null;
			if (bundle instanceof EquinoxBundle) {
				location = ((EquinoxBundle) bundle).getLocation();
			}
			if (location != null && location.startsWith("reference:")) { //$NON-NLS-1$
				location = location.substring(10, location.length());
				URI uri = URI.create(location);
				String newPath =( uri.getPath() == null ? "" : uri.getPath()) + "bin" + '/' + res; //$NON-NLS-1$
				URI newUri = uri.resolve(newPath).normalize();
				if(newUri.isAbsolute())
					url = newUri.toURL();
			}
			if (url != null) {
				File f = new File(url.getFile());
				if (f.exists())
					resources.add(url);
			}
			else
				return Collections.emptyEnumeration();

			return Collections.enumeration(resources);
		}
	}

	static class MultiBundleClassLoader extends ClassLoader {
		private List<Bundle> bundleList;

		public MultiBundleClassLoader(List<Bundle> platformEngineBundles) {
			this.bundleList = platformEngineBundles;

		}
		public Class<?> findClasss(String name) throws ClassNotFoundException {
			return findClass(name);
		}
		@Override
		protected Class<?> findClass(String name) throws ClassNotFoundException {
			Class<?> c = null;
			for (Bundle temp : bundleList) {
				try {
					c = temp.loadClass(name);
					if (c != null)
						return c;
				} catch (ClassNotFoundException e) {
				}
			}
			return c;
		}

		@Override
		protected URL findResource(String name) {
			URL url = null;
			for (Bundle temp : bundleList) {
				url = temp.getResource(name);
				if (url != null)
					return url;
			}
			return url;
		}

		@SuppressWarnings({"rawtypes", "unchecked"})
		@Override
		protected Enumeration findResources(String name) throws IOException {
			Enumeration enumFinal = null;
			for (int i = 0; i < bundleList.size(); i++) {
				if (i == 0) {
					enumFinal = bundleList.get(i).getResources(name);
					continue;
				}
				Enumeration e2 = bundleList.get(i).getResources(name);
				Vector temp = new Vector();
				while (enumFinal != null && enumFinal.hasMoreElements()) {
					temp.add(enumFinal.nextElement());
				}
				while (e2 != null && e2.hasMoreElements()) {
					temp.add(e2.nextElement());
				}
				enumFinal = temp.elements();
			}
			return enumFinal;
		}
	}
}
