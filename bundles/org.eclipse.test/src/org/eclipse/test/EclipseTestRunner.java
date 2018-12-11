/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Anthony Dahanne  <anthony.dahanne@compuware.com> - enhance ETF to be able to launch several tests in several bundles - https://bugs.eclipse.org/330613
 *     Lucas Bullen (Red Hat Inc.) - JUnit 5 support
 *******************************************************************************/
package org.eclipse.test;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.optional.junitlauncher.TestExecutionContext;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.testing.dumps.TimeoutDumpTimer;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.wiring.BundleWiring;

/**
 * A TestRunner for JUnit that supports Ant JUnitResultFormatters and running
 * tests inside Eclipse. Example call: EclipseTestRunner -classname
 * junit.samples.SimpleTest
 * formatter=org.apache.tools.ant.taskdefs.optional.junit
 * .XMLJUnitResultFormatter
 */
public class EclipseTestRunner {
	static class ThreadDump extends Exception {

		private static final long serialVersionUID = 1L;

		ThreadDump(String message) {
			super(message);
		}
	}

	/**
	 * No problems with this test.
	 */
	public static final int SUCCESS = 0;
	/**
	 * Some tests failed.
	 */
	public static final int FAILURES = 1;
	/**
	 * An error occured.
	 */
	public static final int ERRORS = 2;

	/**
	 * The main entry point (the parameters are not yet consistent with the Ant
	 * JUnitTestRunner, but eventually they should be). Parameters
	 *
	 * <pre>
	 * -className=&lt;testSuiteName&gt;
	 * -testPluginName&lt;containingpluginName&gt;
	 * -formatter=&lt;classname&gt;(,&lt;path&gt;)
	 * </pre>
	 * Where &lt;classname&gt; is the formatter classname, currently ignored as only
	 * LegacyXmlResultFormatter is used. The path is either the path to the
	 * result file and should include the file extension (xml) if a single test
	 * is being run or should be the path to the result directory where result
	 * files should be created if multiple tests are being run. If no path is
	 * given, the standard output is used.
	 */
	public static void main(String[] args) throws IOException {
		System.exit(run(args));
	}

	public static int run(String[] args) throws IOException {
		String className = null;
		String classesNames = null;
		String testPluginName = null;
		String testPluginsNames = null;
		String resultPathString = null;
		String timeoutString = null;
		String junitReportOutput = null;

		Properties props = new Properties();

		int startArgs = 0;
		if (args.length > 0) {
			// support the JUnit task commandline syntax where
			// the first argument is the name of the test class
			if (!args[0].startsWith("-")) {
				className = args[0];
				startArgs++;
			}
		}
		for (int i = startArgs; i < args.length; i++) {
			if (args[i].toLowerCase().equals("-classname")) {
				if (i < args.length - 1)
					className = args[i + 1];
				i++;
			} else if (args[i].toLowerCase().equals("-classesnames")) {
				if (i < args.length - 1)
					classesNames = args[i + 1];
				i++;
			} else if (args[i].toLowerCase().equals("-testpluginname")) {
				if (i < args.length - 1)
					testPluginName = args[i + 1];
				i++;
			} else if (args[i].toLowerCase().equals("-testpluginsnames")) {
				if (i < args.length - 1)
					testPluginsNames = args[i + 1];
				i++;
			} else if (args[i].equals("-junitReportOutput")) {
				if (i < args.length - 1)
					junitReportOutput = args[i + 1];
				i++;
			} else if (args[i].startsWith("haltOnError=")) {
				System.err.println("The haltOnError option is no longer supported");
			} else if (args[i].startsWith("haltOnFailure=")) {
				System.err.println("The haltOnFailure option is no longer supported");
			} else if (args[i].startsWith("formatter=")) {
				String formatterString = args[i].substring(10);
				int seperatorIndex = formatterString.indexOf(',');
				resultPathString = seperatorIndex == -1 ? null : formatterString.substring(seperatorIndex + 1);
			} else if (args[i].startsWith("propsfile=")) {
				try (FileInputStream in = new FileInputStream(args[i].substring(10))) {
					props.load(in);
				}
			} else if (args[i].equals("-testlistener")) {
				System.err.println("The testlistener option is no longer supported");
			} else if (args[i].equals("-timeout")) {
				if (i < args.length - 1)
					timeoutString = args[i + 1];
				i++;
			}
		}
		// Add/overlay system properties on the properties from the Ant project
		Hashtable<Object, Object> p = System.getProperties();
		for (Enumeration<Object> _enum = p.keys(); _enum.hasMoreElements();) {
			Object key = _enum.nextElement();
			props.put(key, p.get(key));
		}

		if (timeoutString == null || timeoutString.isEmpty()) {
			System.err.println("INFO: optional timeout was not specified.");
		} else {
			String timeoutScreenOutputDir = null;
			if (junitReportOutput == null || junitReportOutput.isEmpty()) {
				timeoutScreenOutputDir = "timeoutScreens";
			} else {
				timeoutScreenOutputDir = junitReportOutput + "/timeoutScreens";
			}
			System.err.println("INFO: timeoutScreenOutputDir: " + timeoutScreenOutputDir);
			System.err.println("INFO: timeout: " + timeoutString);
			startStackDumpTimeoutTimer(timeoutString, new File(timeoutScreenOutputDir));
		}

		if (testPluginsNames != null && classesNames != null) {
			// we have several plugins to look tests for, let's parse their
			// names
			String[] testPlugins = testPluginsNames.split(",");
			String[] suiteClasses = classesNames.split(",");
			int returnCode = 0;
			int j = 0;
			EclipseTestRunner runner = new EclipseTestRunner();
			for (String oneClassName : suiteClasses) {
				int result = runner.runTests(props, testPlugins[j], oneClassName, resultPathString, true);
				j++;
				if(result != 0) {
					returnCode = result;
				}
			}
			return returnCode;
		}
		if (className == null)
			throw new IllegalArgumentException("Test class name not specified");
		EclipseTestRunner runner = new EclipseTestRunner();
		return runner.runTests(props, testPluginName, className, resultPathString, false);
	}

	private int runTests(Properties props, String testPluginName, String testClassName, String resultPath, boolean multiTest) {
		ClassLoader currentTCCL = Thread.currentThread().getContextClassLoader();
		ExecutionListener executionListener = new ExecutionListener();
		if(testPluginName == null) {
			testPluginName = ClassLoaderTools.getClassPlugin(testClassName);
		}
		if(testPluginName == null)
			throw new IllegalArgumentException("Test class not found");
		LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
				.selectors(selectClass(testClassName))
				.build();

		try {
			Thread.currentThread().setContextClassLoader(ClassLoaderTools.getJUnit5Classloader(getPlatformEngines()));
			final Launcher launcher = LauncherFactory.create();

			Thread.currentThread().setContextClassLoader(ClassLoaderTools.getPluginClassLoader(testPluginName, currentTCCL));
			try(LegacyXmlResultFormatter legacyXmlResultFormatter = new LegacyXmlResultFormatter()){
				try (OutputStream fileOutputStream = getResultOutputStream(resultPath,testClassName,multiTest)){
					legacyXmlResultFormatter.setDestination(fileOutputStream);
					legacyXmlResultFormatter.setContext(new ExecutionContext(props));
					launcher.execute(request, legacyXmlResultFormatter, executionListener);
				}
			} catch (IOException e) {
				e.printStackTrace();
				return ERRORS;
			}
		} finally {
			Thread.currentThread().setContextClassLoader(currentTCCL);
		}
		return executionListener.didExecutionContainedFailures() ? FAILURES : SUCCESS;
	}

	private OutputStream getResultOutputStream(String resultPathString, String testClassName, boolean multiTest) throws IOException {
		if(resultPathString == null || resultPathString.isEmpty())
			return System.out;
		File resultFile;
		if(multiTest) {
			Path resultDirectoryPath = new Path(resultPathString);
			File testDirectory = resultDirectoryPath.toFile();
			if(!testDirectory.exists())
				testDirectory.mkdirs();
			resultFile = resultDirectoryPath.append("TEST-"+testClassName+".xml").toFile();
		}else {
			IPath resultPath = new Path(resultPathString);
			resultFile = resultPath.toFile();
			if(resultFile.isDirectory()) {
				resultFile = resultPath.append("TEST-"+testClassName+".xml").toFile();
			} else {
				File resultDirectory = resultFile.getParentFile();
				if(!resultDirectory.exists())
					resultDirectory.mkdirs();
			}
		}
		if(!resultFile.exists()) {
			resultFile.createNewFile();
		}
		return new FileOutputStream(resultFile);
	}


	private List<String> getPlatformEngines(){
		List<String> platformEngines = new ArrayList<>();
		Bundle bundle = FrameworkUtil.getBundle(getClass());
		Bundle[] bundles = bundle.getBundleContext().getBundles();
		for (Bundle iBundle : bundles) {
			try {
				BundleWiring bundleWiring = Platform.getBundle(iBundle.getSymbolicName()).adapt(BundleWiring.class);
				Collection<String> listResources = bundleWiring.listResources("META-INF/services", "org.junit.platform.engine.TestEngine", BundleWiring.LISTRESOURCES_LOCAL);
				if (!listResources.isEmpty())
					platformEngines.add(iBundle.getSymbolicName());
			} catch (Exception e) {
				// check the next bundle
			}
		}
		return platformEngines;
	}

	private final class ExecutionListener implements TestExecutionListener {
		private boolean executionContainedFailures;

		public ExecutionListener() {
			this.executionContainedFailures = false;
		}

		public boolean didExecutionContainedFailures() {
			return executionContainedFailures;
		}

		@Override
		public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
			if(testExecutionResult.getStatus() == org.junit.platform.engine.TestExecutionResult.Status.FAILED) {
				executionContainedFailures = true;
			}
		}
	}

	private final class ExecutionContext implements TestExecutionContext {

		private final Properties props;

		ExecutionContext(Properties props) {
			this.props = props;
		}

		@Override
		public Properties getProperties() {
			return this.props;
		}

		@Override
		public Optional<Project> getProject() {
			return null;
		}
	}

	/**
	 * Starts a timer that dumps interesting debugging information shortly before
	 * the given timeout expires.
	 *
	 * @param timeoutArg      the -timeout argument from the command line
	 * @param outputDirectory where the test results end up
	 */
	private static void startStackDumpTimeoutTimer(final String timeoutArg, final File outputDirectory) {
		TimeoutDumpTimer.startTimeoutDumpTimer(timeoutArg, outputDirectory);
	}
}
