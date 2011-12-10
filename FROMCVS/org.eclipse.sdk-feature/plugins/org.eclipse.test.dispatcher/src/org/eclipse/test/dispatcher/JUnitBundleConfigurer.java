/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.test.dispatcher;

import java.lang.reflect.Method;
import java.util.Dictionary;
import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.util.ManifestElement;
import org.osgi.framework.*;
import org.osgi.service.packageadmin.PackageAdmin;

/**
 * A utility used by the automated test framework to determine which JUnit bundle should
 * be loaded to run a given plugin's tests. 
 */
public class JUnitBundleConfigurer {
	
	private static final String IMPORT_PACKAGE_IDENTIFIER = "version";
	private static final String REQUIRE_BUNDLE_IDENTIFIER = "bundle-version";
	/**
	 * The name of the test plugin
	 */
	private String testPluginName;
	
	/**
	 * Calls to either org.eclipse.test.CoreTestApplication or org.eclipse.test.UITestApplication
	 */
	public static Object run(String className) throws Exception {
		Class clazz = getClass(className);
		String[] args = Platform.getCommandLineArgs();
		
		Class[] parameterType = new Class[1];
		parameterType[0] = Object.class;
		Method runMethod = clazz.getMethod("run", new Class[] {String[].class});
		return runMethod.invoke(clazz.newInstance(), new Object[] {args});
	}
	
	public static void runTests(String className) throws Exception
	{
		Class clazz = getClass(className);
		Class[] parameterType = new Class[1];
		parameterType[0] = Object.class;
		Method runMethod = clazz.getMethod("runTests", null);
		runMethod.invoke(clazz, null);
	}
	
	private static Class getClass(String className) throws ClassNotFoundException
	{
		final String etfBundleName = "org.eclipse.test";
		Bundle etfBundle = getTestBundle(etfBundleName);
        if (etfBundle == null) {
            throw new ClassNotFoundException(className, new Exception("Could not find plugin \""
                    + etfBundleName + "\""));
        }
		return etfBundle.loadClass(className);
	}
	
	public void configureJUnit(String[] args)
	{
		for (int i=0; i < args.length; i++) {
			if (args[i].toLowerCase().equals("-testpluginname")) {
				if (i < args.length-1)
					testPluginName= args[i+1]; 
				i++;	
			}
		}
		
		manageJUnitBundles();
	}
	
	/**
	 * Checks the dependencies and required imports of the bundle whose tests are to be run.
	 * If the test bundle depends on version 4.0.0+ of org.junit, no action is taken.
	 * Otherwise, org.junit version 4.x is uninstalled.
	 */
	private void manageJUnitBundles() {
		// are we running JUnit 4?
		Bundle junitBundle = Platform.getBundle("org.junit");
		if (junitBundle == null)
			return;
		Dictionary headers = junitBundle.getHeaders();
		String bundleVersion = (String) headers.get(Constants.BUNDLE_VERSION);
		if (bundleVersion == null)
			return;
		Version version;
		try {
			version = Version.parseVersion(bundleVersion);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			return;
		}
		if (version.getMajor() < 4)
			return;

		// is there more than one Junit bundle available?
		Bundle[] bundles = Platform.getBundles("org.junit", null);
		if (bundles != null && bundles.length == 1)
			return;
		
		
		Bundle testBundle = getTestBundle(testPluginName);
		if (testBundle == null)
			return;
		
		// does the test bundle specify a version or range?
		if (requiresJUnit4(testBundle, Constants.REQUIRE_BUNDLE) || requiresJUnit4(testBundle, Constants.IMPORT_PACKAGE)) return;
		
		uninstallJUnit4(junitBundle);
	}
	
	/**
	 * Uninstall JUnit4 and refresh
	 */
	private void uninstallJUnit4(Bundle junit4Bundle) {
		BundleContext context = Activator.getBundleContext();
		if (junit4Bundle.getState() != Bundle.UNINSTALLED){
			try{
				junit4Bundle.uninstall();
			} catch (BundleException e) {
				e.printStackTrace();
			}
		}
		
		ServiceReference packageAdminRef = context.getServiceReference(PackageAdmin.class.getName());
		PackageAdmin packageAdmin = null;
		if (packageAdminRef != null) {
			packageAdmin = (PackageAdmin) context.getService(packageAdminRef);
			if (packageAdmin == null)
				return;
		}
		final boolean[] flag = new boolean[] {false};
		FrameworkListener listener = new FrameworkListener() {
			public void frameworkEvent(FrameworkEvent event) {
				if (event.getType() == FrameworkEvent.PACKAGES_REFRESHED)
					synchronized (flag) {
						flag[0] = true;
						flag.notifyAll();
					}
			}
		};
		context.addFrameworkListener(listener);
		packageAdmin.refreshPackages(null);
		
		synchronized (flag) {
			while (!flag[0]) {
				try {
					flag.wait();
				} catch (InterruptedException e) {
				}
			}
		}
		context.removeFrameworkListener(listener);
		context.ungetService(packageAdminRef);	
	}

	/**
	 * Returns the Bundle specified by testPluginName
	 */
	private static Bundle getTestBundle(String bundleName) {
		if (bundleName == null)
			return null;
        Bundle bundle = Platform.getBundle(bundleName);
        if (bundle == null)
        	return null;
        
        //is the plugin a fragment?
		Dictionary headers = bundle.getHeaders();
		String hostHeader = (String) headers.get(Constants.FRAGMENT_HOST);
		if (hostHeader != null) {
			// we are a fragment for sure
			// we need to find which is our host
			ManifestElement[] hostElement = null;
			try {
				hostElement = ManifestElement.parseHeader(Constants.FRAGMENT_HOST, hostHeader);
			} catch (BundleException e) {
				throw new RuntimeException("Could not find host for fragment:" + bundleName, e);
			}
			Bundle host = Platform.getBundle(hostElement[0].getValue());
			//we really want to get the host not the fragment
			bundle = host;
		} 

        return bundle;
    }
	
	/**
	 * Helper method for manageJUnitBundles().  Determines whether a bundle requires the JUnit4 bundle or imports
	 * a JUnit4 package
	 */
	private boolean requiresJUnit4(Bundle testBundle, String searchString) {
		String versionIdentifier = new String();
		if (Constants.REQUIRE_BUNDLE.equals(searchString)) versionIdentifier = REQUIRE_BUNDLE_IDENTIFIER;
		else if (Constants.IMPORT_PACKAGE.equals(searchString)) versionIdentifier = IMPORT_PACKAGE_IDENTIFIER;
		else return false;
		
		Dictionary testBundleHeaders = testBundle.getHeaders();
		ManifestElement[] manifestElements = null;
		try {
			manifestElements = ManifestElement.parseHeader(searchString, 
				((String) testBundleHeaders.get(searchString)));
		}catch(BundleException e) {
			return false;
		}
		ManifestElement junitManifestElement = null;
		if (manifestElements == null) return false;
		for (int i=0; i<manifestElements.length; i++) {
			if (manifestElements[i].getValue().startsWith("org.junit") || 
					manifestElements[i].getValue().startsWith("junit")) {
				junitManifestElement = manifestElements[i];
			}
		}
		
		if (junitManifestElement == null) return false;
		String version = junitManifestElement.getAttribute(versionIdentifier);
		if (version == null) return false;
		
		int indexOfComma = version.indexOf(',');
		if (indexOfComma == -1) {  //a single version has been specified
			if (version.startsWith("4")) return true;
			return false;
		}
		else {  //the version is a range
			Version lowerBoundVersion = new Version(version.substring(1, indexOfComma));
			if (lowerBoundVersion.getMajor() == 3) return false;
			if (lowerBoundVersion.getMinor() != 0) return true;
			if (lowerBoundVersion.getMicro() != 0) return true;
			//the version is 4.0.0, is it exclusive or inclusive?
			if (version.charAt(0) == '[') return true;
			return false;
		}
	}

}
