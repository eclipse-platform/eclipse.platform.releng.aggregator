/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Anthony Dahanne  <anthony.dahanne@compuware.com> - enhance ETF to be able to launch several tests in several bundles - https://bugs.eclipse.org/330613
 *******************************************************************************/
package org.eclipse.test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import junit.framework.AssertionFailedError;
import junit.framework.Test;
import junit.framework.TestListener;
import junit.framework.TestResult;
import junit.framework.TestSuite;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.optional.junit.JUnitResultFormatter;
import org.apache.tools.ant.taskdefs.optional.junit.JUnitTest;
import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

/**
 * A TestRunner for JUnit that supports Ant JUnitResultFormatters and running
 * tests inside Eclipse. Example call: EclipseTestRunner -classname
 * junit.samples.SimpleTest
 * formatter=org.apache.tools.ant.taskdefs.optional.junit
 * .XMLJUnitResultFormatter
 */
public class EclipseTestRunner implements TestListener {
	class TestFailedException extends Exception {

		private static final long serialVersionUID = 6009335074727417445L;

		TestFailedException(String message) {
			super(message);
		}

		TestFailedException(Throwable e) {
			super(e);
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

	private static final String SUITE_METHODNAME = "suite";
	/**
	 * SECONDS_BEFORE_TIMEOUT_BUFFER is the time we allow ourselves to take
	 * stack traces, get a screen shot, delay "SECONDS_BETWEEN_DUMPS", then do
	 * it again. On current build machine, it takes about 30 seconds to do all
	 * that, so 2 minutes should be suffiencient time allowed for most machines.
	 * Though, should increase, say, if we increase the "time beteen dumps" to a
	 * minute or more.
	 */
	private static final int SECONDS_BEFORE_TIMEOUT_BUFFER = 120;

	/**
	 * SECONDS_BETWEEN_DUMPS is the time we wait from first to second dump of
	 * stack trace and screenshots. In most cases, this should suffice to
	 * determine if still busy doing something, or, hung, or waiting for user
	 * input.
	 */
	private static final int SECONDS_BETWEEN_DUMPS = 5;

	/**
	 * The current test result
	 */
	private TestResult fTestResult;
	/**
	 * The name of the plugin containing the test
	 */
	private String fTestPluginName;
	/**
	 * The corresponding testsuite.
	 */
	private Test fSuite;
	/**
	 * Formatters from the command line.
	 */
	private static Vector<JUnitResultFormatter> fgFromCmdLine = new Vector<JUnitResultFormatter>();
	/**
	 * Holds the registered formatters.
	 */
	private Vector<JUnitResultFormatter> formatters = new Vector<JUnitResultFormatter>();
	/**
	 * Do we stop on errors.
	 */
	private boolean fHaltOnError = false;
	/**
	 * Do we stop on test failures.
	 */
	private boolean fHaltOnFailure = false;
	/**
	 * The TestSuite we are currently running.
	 */
	private JUnitTest fJunitTest;
	/**
	 * output written during the test
	 */
	private PrintStream fSystemError;
	/**
	 * Error output during the test
	 */
	private PrintStream fSystemOut;
	/**
	 * Exception caught in constructor.
	 */
	private Exception fException;
	/**
	 * Returncode
	 */
	private int fRetCode = SUCCESS;

	/**
	 * The main entry point (the parameters are not yet consistent with the Ant
	 * JUnitTestRunner, but eventually they should be). Parameters
	 * 
	 * <pre>
	 * -className: the name of the testSuite
	 * -testPluginName: the name of the containing plugin
	 * haltOnError: halt test on errors?
	 * haltOnFailure: halt test on failures?
	 * -testlistener listenerClass: deprecated
	 * 		print a warning that this option is deprecated
	 * formatter: a JUnitResultFormatter given as classname,filename. 
	 *  	If filename is ommitted, System.out is assumed.
	 * </pre>
	 */
	public static void main(String[] args) throws IOException {
		System.exit(run(args));
	}

	public static int run(String[] args) throws IOException {
		String className = null;
		String classesNames = null;
		String testPluginName = null;
		String testPluginsNames = null;
		String formatterString = null;
		String timeoutString = null;
		String junitReportOutput = null;

		boolean haltError = false;
		boolean haltFail = false;

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
				haltError = Project.toBoolean(args[i].substring(12));
			} else if (args[i].startsWith("haltOnFailure=")) {
				haltFail = Project.toBoolean(args[i].substring(14));
			} else if (args[i].startsWith("formatter=")) {
				formatterString = args[i].substring(10);
			} else if (args[i].startsWith("propsfile=")) {
				FileInputStream in = new FileInputStream(args[i].substring(10));
				props.load(in);
				in.close();
			} else if (args[i].equals("-testlistener")) {
            	System.err.println("The -testlistener option is no longer supported\nuse the formatter= option instead");
				return ERRORS;
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

		String timeoutScreenOutputDir = null;
		if (junitReportOutput == null || junitReportOutput.length() == 0) {
			timeoutScreenOutputDir = "timeoutScreens";
		} else {
			timeoutScreenOutputDir = junitReportOutput + "/timeoutScreens";
		}
		System.err.println("timeoutScreenOutputDir: " + timeoutScreenOutputDir);
		startStackDumpTimoutTimer(timeoutString, new File(
				timeoutScreenOutputDir), className);

		if (testPluginsNames != null && classesNames != null) {
			// we have several plugins to look tests for, let's parse their
			// names
			String[] testPlugins = testPluginsNames.split(",");
			String[] suiteClasses = classesNames.split(",");
			try {
				createAndStoreFormatter(formatterString, suiteClasses);
			} catch (BuildException be) {
				System.err.println(be.getMessage());
				return ERRORS;
			}
			int returnCode = 0;
			int j = 0;
			for (String oneClassName : suiteClasses) {
				JUnitTest t = new JUnitTest(oneClassName);
				t.setProperties(props);
				EclipseTestRunner runner = new EclipseTestRunner(t, testPlugins[j],
						haltError, haltFail);
				transferFormatters(runner, j);
				runner.run();
				j++;
				if (runner.getRetCode() != 0) {
					returnCode = runner.getRetCode();
				}
			}
			return returnCode;
		}
		try {
			createAndStoreFormatter(formatterString);
		} catch (BuildException be) {
			System.err.println(be.getMessage());
			return ERRORS;
		}
		if (className == null)
			throw new IllegalArgumentException("Test class name not specified");

		JUnitTest t = new JUnitTest(className);

		t.setProperties(props);

	    EclipseTestRunner runner= new EclipseTestRunner(t, testPluginName, haltError, haltFail);
		transferFormatters(runner);
		runner.run();
		return runner.getRetCode();
	}

	/**
	 * Starts a timer that dumps interesting debugging information shortly
	 * before the given timeout expires.
	 * 
	 * @param timeoutArg
	 *            the -timeout argument from the command line
	 * @param outputDirectory
	 *            where the test results end up
	 * @param classname
	 *            the class that is running the tests suite
	 */
	private static void startStackDumpTimoutTimer(final String timeoutArg,
			final File outputDirectory, final String classname) {
		try {
			/*
			 * The delay (in ms) is the sum of - the expected time it took for
			 * launching the current VM and reaching this method - the time it
			 * will take to run the garbage collection and dump all the infos
			 * (twice)
			 */
			int delay = SECONDS_BEFORE_TIMEOUT_BUFFER * 1000;

			int timeout = Integer.parseInt(timeoutArg) - delay;
			String time0 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z",
					Locale.US).format(new Date());
			System.err.println("starting EclipseTestRunnerTimer with timeout="
					+ timeout + " at " + time0);
			if (timeout > 0) {
				new Timer("EclipseTestRunnerTimer", true).schedule(
						new TimerTask() {
							@Override
							public void run() {
								dump(0);
								try {
									Thread.sleep(SECONDS_BETWEEN_DUMPS * 1000);
								} catch (InterruptedException e) {
									// continue
								}
								dump(SECONDS_BETWEEN_DUMPS);
							}

							/**
							 * 
							 * @param num 
							 *     num is purely a lable used in naming the screen capture files. 
							 *     By convention, we pass in 0 or "SECONDS_BETWEEN_DUMPS" just as 
							 *     a subtle reminder of how much time as elapsed.  
							 *     Thus, files end up with names similar to 
							 *     <classname>_screen0.png, <classname>_screem5.png
                             *     in a directory named "timeoutScreens" under "results", 
                             *     such as .../results/linux.gtk.x86_64/timeoutScreens/ 
							 */
							private void dump(final int num) {
								// Time elapsed time to do each dump, so we'll
								// know if/when we get too close to the 2
								// minutes we allow
								// Get current time
								long start = System.currentTimeMillis();

								// Dump all stacks:
								System.err
										.println("EclipseTestRunner almost reached timeout '"
												+ timeoutArg + "'.");
								System.err.println("totalMemory:            "
										+ Runtime.getRuntime().totalMemory());
								System.err.println("freeMemory (before GC): "
										+ Runtime.getRuntime().freeMemory());
								System.gc();
								System.err.println("freeMemory (after GC):  "
										+ Runtime.getRuntime().freeMemory());
								String time = new SimpleDateFormat(
										"yyyy-MM-dd HH:mm:ss Z", Locale.US)
										.format(new Date());
								System.err.println("Thread dump " + num
										+ " at " + time + ":");
								Map<Thread, StackTraceElement[]> stackTraces = Thread
										.getAllStackTraces();
								for (Entry<Thread, StackTraceElement[]> entry : stackTraces
										.entrySet()) {
									String name = entry.getKey().getName();
									StackTraceElement[] stack = entry
											.getValue();
									Exception exception = new Exception(name);
									exception.setStackTrace(stack);
									exception.printStackTrace();
								}

								final Display display = getDisplay();
								display.syncExec(new Runnable() {
									public void run() {
										// Dump focus control, parents, and
										// shells:
										Control focusControl = display
												.getFocusControl();
										if (focusControl != null) {
											System.err
													.println("FocusControl: ");
											StringBuilder indent = new StringBuilder(
													"  ");
											do {
												System.err.println(indent
														.toString()
														+ focusControl);
												focusControl = focusControl
														.getParent();
												indent.append("  ");
											} while (focusControl != null);
										}
										Shell[] shells = display.getShells();
										if (shells.length > 0) {
											System.err.println("Shells: ");
											for (int i = 0; i < shells.length; i++) {
												Shell shell = shells[i];
												System.err.println((shell
														.isVisible() ? "  visible: "
														: "  invisible: ")
														+ shell);
											}
										}

										// Take a screenshot:
										if (!outputDirectory.exists())
											outputDirectory.mkdirs();
										GC gc = new GC(display);
										final Image image = new Image(display,
												display.getBounds());
										gc.copyArea(image, 0, 0);
										gc.dispose();

										ImageLoader loader = new ImageLoader();
										loader.data = new ImageData[] { image
												.getImageData() };
										String filename = outputDirectory
												.getAbsolutePath()
												+ "/"
												+ classname
												+ "_screen"
												+ num
												+ ".png";
										loader.save(filename, SWT.IMAGE_PNG);
										System.err
												.println("Screenshot saved to: "
														+ filename);
										image.dispose();
									}
								});
								
								// Elapsed time in milliseconds
								long elapsedTimeMillis = System
										.currentTimeMillis() - start;

								// Print in seconds
								float elapsedTimeSec = elapsedTimeMillis / 1000F;
								System.err.println("INFO: Seconds to do dump "
										+ num + ": " + elapsedTimeSec);
							}
						}, timeout);
			} else {
				System.err
						.println("EclipseTestRunner argument error: '-timeout "
								+ timeoutArg
								+ "' was too short to accommodate time delay required ("
								+ delay + ").");
			}
		} catch (NumberFormatException e) {
			e.printStackTrace();
		}
	}

	/**
     *   
     */
	public EclipseTestRunner(JUnitTest test, String testPluginName,
			boolean haltOnError, boolean haltOnFailure) {
		fJunitTest = test;
		fTestPluginName = testPluginName;
		fHaltOnError = haltOnError;
		fHaltOnFailure = haltOnFailure;

		try {
			fSuite = getTest(test.getName());
		} catch (Exception e) {
			fRetCode = ERRORS;
			fException = e;
		}
	}

	/**
	 * Returns the Test corresponding to the given suite.
	 */
	protected Test getTest(String suiteClassName) throws TestFailedException {
		if (suiteClassName.length() <= 0) {
			clearStatus();
			return null;
		}
		Class<?> testClass = null;
		try {
			testClass = loadSuiteClass(suiteClassName);
		} catch (ClassNotFoundException e) {
			if (e.getCause() != null) {
				runFailed(e.getCause());
			}
			String clazz = e.getMessage();
			if (clazz == null)
				clazz = suiteClassName;
			runFailed("Class not found \"" + clazz + "\"");
			return null;
		} catch (Exception e) {
			runFailed(e);
			return null;
		}
		Method suiteMethod = null;
		try {
			suiteMethod = testClass.getMethod(SUITE_METHODNAME, new Class[0]);
		} catch (Exception e) {
			// try to extract a test suite automatically
			clearStatus();

			Class<?> jUnit4TestAdapterClass = null;
			try {
				jUnit4TestAdapterClass = loadSuiteClass("junit.framework.JUnit4TestAdapter");
			} catch (ClassNotFoundException e1) {
				// JUnit4 is not available
			} catch (UnsupportedClassVersionError e1) {
				// running with a VM < 1.5
			}
			if (jUnit4TestAdapterClass != null) {
				try {
					Constructor<?> jUnit4TestAdapterCtor = jUnit4TestAdapterClass
							.getConstructor(new Class[] { Class.class });
					return (Test) jUnit4TestAdapterCtor
							.newInstance(new Object[] { testClass });
				} catch (Exception e1) {
					runFailed(new InvocationTargetException(e1,
							"Failed to create a JUnit4TestAdapter for \""
									+ suiteClassName + "\":"));
					return null;
				}
			} else { // the JUnit 3 way
				return new TestSuite(testClass);
			}
		}
		if (!Modifier.isStatic(suiteMethod.getModifiers())) {
			runFailed("suite() method must be static");
			return null;
		}
		Test test = null;
		try {
			test = (Test) suiteMethod.invoke(null, (Object[]) new Class[0]); // static
																				// method
			if (test == null)
				return test;
		} catch (InvocationTargetException e) {
			runFailed("Failed to invoke suite():"
					+ e.getTargetException().toString());
			return null;
		} catch (IllegalAccessException e) {
			runFailed("Failed to invoke suite():" + e.toString());
			return null;
		}
		clearStatus();
		return test;
	}

	protected void runFailed(String message) throws TestFailedException {
		System.err.println(message);
		throw new TestFailedException(message);
	}

	protected void runFailed(Throwable e) throws TestFailedException {
		e.printStackTrace();
		throw new TestFailedException(e);
	}

	protected void clearStatus() {
	}

	/**
	 * Loads the class either with the system class loader or a plugin class
	 * loader if a plugin name was specified
	 */
	protected Class<?> loadSuiteClass(String suiteClassName)
			throws ClassNotFoundException {
		if (fTestPluginName == null)
			return Class.forName(suiteClassName);
		Bundle bundle = Platform.getBundle(fTestPluginName);
		if (bundle == null) {
			throw new ClassNotFoundException(suiteClassName, new Exception(
					"Could not find plugin \"" + fTestPluginName + "\""));
		}

		// is the plugin a fragment?
		Dictionary<String, String> headers = bundle.getHeaders();
		String hostHeader = headers.get(Constants.FRAGMENT_HOST);
		if (hostHeader != null) {
			// we are a fragment for sure
			// we need to find which is our host
			ManifestElement[] hostElement = null;
			try {
				hostElement = ManifestElement.parseHeader(
						Constants.FRAGMENT_HOST, hostHeader);
			} catch (BundleException e) {
				throw new RuntimeException("Could not find host for fragment:"
						+ fTestPluginName, e);
			}
			Bundle host = Platform.getBundle(hostElement[0].getValue());
			// we really want to get the host not the fragment
			bundle = host;
		}

		return bundle.loadClass(suiteClassName);
	}

	public void run() {
		// IPerformanceMonitor pm =
		// PerfMsrCorePlugin.getPerformanceMonitor(true);

		fTestResult = new TestResult();
		fTestResult.addListener(this);
		for (int i = 0; i < formatters.size(); i++) {
			fTestResult.addListener(formatters.elementAt(i));
		}

		long start = System.currentTimeMillis();
		fireStartTestSuite();

		if (fException != null) { // had an exception in the constructor
			for (int i = 0; i < formatters.size(); i++) {
				formatters.elementAt(i).addError(null, fException);
			}
			fJunitTest.setCounts(1, 0, 1);
			fJunitTest.setRunTime(0);
		} else {
			ByteArrayOutputStream errStrm = new ByteArrayOutputStream();
			fSystemError = new PrintStream(errStrm);

			ByteArrayOutputStream outStrm = new ByteArrayOutputStream();
			fSystemOut = new PrintStream(outStrm);

			try {
				// pm.snapshot(1); // before
				fSuite.run(fTestResult);
			} finally {
				// pm.snapshot(2); // after
				fSystemError.close();
				fSystemError = null;
				fSystemOut.close();
				fSystemOut = null;
				sendOutAndErr(new String(outStrm.toByteArray()), new String(
						errStrm.toByteArray()));
				fJunitTest.setCounts(fTestResult.runCount(),
						fTestResult.failureCount(), fTestResult.errorCount());
				fJunitTest.setRunTime(System.currentTimeMillis() - start);
			}
		}
		fireEndTestSuite();

		if (fRetCode != SUCCESS || fTestResult.errorCount() != 0) {
			fRetCode = ERRORS;
		} else if (fTestResult.failureCount() != 0) {
			fRetCode = FAILURES;
		}

		// pm.upload(getClass().getName());
	}

	/**
	 * Returns what System.exit() would return in the standalone version.
	 * 
	 * @return 2 if errors occurred, 1 if tests failed else 0.
	 */
	public int getRetCode() {
		return fRetCode;
	}

	/*
	 * @see TestListener.addFailure
	 */
	public void startTest(Test t) {
	}

	/*
	 * @see TestListener.addFailure
	 */
	public void endTest(Test test) {
	}

	/*
	 * @see TestListener.addFailure
	 */
	public void addFailure(Test test, AssertionFailedError t) {
		if (fHaltOnFailure) {
			fTestResult.stop();
		}
	}

	/*
	 * @see TestListener.addError
	 */
	public void addError(Test test, Throwable t) {
		if (fHaltOnError) {
			fTestResult.stop();
		}
	}

	private void fireStartTestSuite() {
		for (int i = 0; i < formatters.size(); i++) {
			formatters.elementAt(i).startTestSuite(fJunitTest);
		}
	}

	private void fireEndTestSuite() {
		for (int i = 0; i < formatters.size(); i++) {
			formatters.elementAt(i).endTestSuite(fJunitTest);
		}
	}

	public void addFormatter(JUnitResultFormatter f) {
		formatters.addElement(f);
	}

	/**
	 * Line format is: formatter=<classname>(,<pathname>)?
	 */
	private static void createAndStoreFormatter(String line)
			throws BuildException {
		String formatterClassName = null;
		File formatterFile = null;

		int pos = line.indexOf(',');
		if (pos == -1) {
			formatterClassName = line;
		} else {
			formatterClassName = line.substring(0, pos);
			formatterFile = new File(line.substring(pos + 1)); // the method is
																// package
																// visible
		}
		fgFromCmdLine.addElement(createFormatter(formatterClassName,
				formatterFile));
	}

	/**
	 * Line format is: formatter=<pathname>
	 */
	private static void createAndStoreFormatter(String line,
			String... suiteClassesNames) throws BuildException {
		String formatterClassName = null;
		File formatterFile = null;

		int pos = line.indexOf(',');
		if (pos == -1) {
			formatterClassName = line;
		} else {
			formatterClassName = line.substring(0, pos);
		}
		File outputDirectory = new File(line.substring(pos + 1));
		outputDirectory.mkdir();
		for (String suiteClassName : suiteClassesNames) {

			String pathname = "TEST-" + suiteClassName + ".xml";
			if (outputDirectory.exists()) {
				pathname = outputDirectory.getAbsolutePath() + "/" + pathname;
			}
			formatterFile = new File(pathname);
			fgFromCmdLine.addElement(createFormatter(formatterClassName,
					formatterFile));
		}

	}

	private static void transferFormatters(EclipseTestRunner runner, int j) {
		runner.addFormatter(fgFromCmdLine.elementAt(j));
	}

	private static void transferFormatters(EclipseTestRunner runner) {
		for (int i = 0; i < fgFromCmdLine.size(); i++) {
			runner.addFormatter(fgFromCmdLine.elementAt(i));
		}
	}

	/*
	 * DUPLICATED from FormatterElement, since it is package visible only
	 */
	private static JUnitResultFormatter createFormatter(String classname,
			File outfile) throws BuildException {
		OutputStream out = System.out;

		if (classname == null) {
			throw new BuildException("you must specify type or classname");
		}
		Class<?> f = null;
		try {
			f = EclipseTestRunner.class.getClassLoader().loadClass(classname);
		} catch (ClassNotFoundException e) {
			throw new BuildException(e);
		}

		Object o = null;
		try {
			o = f.newInstance();
		} catch (InstantiationException e) {
			throw new BuildException(e);
		} catch (IllegalAccessException e) {
			throw new BuildException(e);
		}

		if (!(o instanceof JUnitResultFormatter)) {
			throw new BuildException(classname
					+ " is not a JUnitResultFormatter");
		}

		JUnitResultFormatter r = (JUnitResultFormatter) o;

		if (outfile != null) {
			try {
				out = new FileOutputStream(outfile);
			} catch (java.io.IOException e) {
				throw new BuildException(e);
			}
		}
		r.setOutput(out);
		return r;
	}

	private void sendOutAndErr(String out, String err) {
		for (int i = 0; i < formatters.size(); i++) {
			JUnitResultFormatter formatter = formatters.elementAt(i);

			formatter.setSystemOutput(out);
			formatter.setSystemError(err);
		}
	}

	protected void handleOutput(String line) {
		if (fSystemOut != null) {
			fSystemOut.println(line);
		}
	}

	protected void handleErrorOutput(String line) {
		if (fSystemError != null) {
			fSystemError.println(line);
		}
	}
	
	private static Display getDisplay() {
		Display display = Display.getCurrent();
		if (display == null) {
			System.err
					.println("Display.getCurrent() returned null so this is either a core test with no UI, or this test is running in a non-UI thread, so we will use Display.getDefault() to be safe.");
			display = Display.getDefault();
		}
		return display;
	}
}
