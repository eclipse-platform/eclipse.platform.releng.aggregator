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
package org.eclipse.test;

import java.io.*;
import java.util.*;
import junit.framework.*;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.optional.junit.JUnitResultFormatter;
import org.apache.tools.ant.taskdefs.optional.junit.JUnitTest;
import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.util.ManifestElement;
import org.osgi.framework.*;

/**
 * A TestRunner for JUnit that supports Ant JUnitResultFormatters
 * and running tests inside Eclipse.
 * Example call: EclipseTestRunner -classname junit.samples.SimpleTest formatter=org.apache.tools.ant.taskdefs.optional.junit.XMLJUnitResultFormatter
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
    public static final int SUCCESS= 0;
    /**
     * Some tests failed.
     */
    public static final int FAILURES= 1;
    /**
     * An error occured.
     */
    public static final int ERRORS= 2;
    
    protected static final String SUITE_METHODNAME= "suite";	
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
    private Test testClass;
    /**
     * Formatters from the command line.
     */
	private static Vector fgFromCmdLine= new Vector();
	/**
     * Holds the registered formatters.
     */
    private Vector formatters= new Vector();
    /**
     * Do we stop on errors.
     */
    private boolean fHaltOnError= false;
    /**
     * Do we stop on test failures.
     */
    private boolean fHaltOnFailure= false;
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
    private int fRetCode= SUCCESS;	
    
	/** 
	 * The main entry point (the parameters are not yet consistent with
	 * the Ant JUnitTestRunner, but eventually they should be).
	 * Parameters<pre>
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
		String className= null;
		String testPluginName= null;
		
        boolean haltError = false;
        boolean haltFail = false;
        
        Properties props = new Properties();
		
		int startArgs= 0;
		if (args.length > 0) {
			// support the JUnit task commandline syntax where
			// the first argument is the name of the test class
			if (!args[0].startsWith("-")) {
				className= args[0];
				startArgs++;
			}
		} 
		for (int i= startArgs; i < args.length; i++) {
			if (args[i].toLowerCase().equals("-classname")) {
				if (i < args.length-1)
					className= args[i+1]; 
				i++;	
			} else if (args[i].toLowerCase().equals("-testpluginname")) {
				if (i < args.length-1)
					testPluginName= args[i+1]; 
				i++;	
			} else if (args[i].startsWith("haltOnError=")) {
                haltError= Project.toBoolean(args[i].substring(12));
            } else if (args[i].startsWith("haltOnFailure=")) {
                haltFail = Project.toBoolean(args[i].substring(14));
            } else if (args[i].startsWith("formatter=")) {
                try {
                    createAndStoreFormatter(args[i].substring(10));
                } catch (BuildException be) {
                    System.err.println(be.getMessage());
                    return ERRORS;
                }
            } else if (args[i].startsWith("propsfile=")) {
                FileInputStream in = new FileInputStream(args[i].substring(10));
                props.load(in);
                in.close();
            } else if (args[i].equals("-testlistener")) {
            	System.err.println("The -testlistener option is no longer supported\nuse the formatter= option instead");
            	return ERRORS;
			}
        }
			
		if (className == null)
			throw new IllegalArgumentException("Test class name not specified");
		
        JUnitTest t= new JUnitTest(className);

        // Add/overlay system properties on the properties from the Ant project
        Hashtable p= System.getProperties();
        for (Enumeration _enum = p.keys(); _enum.hasMoreElements(); ) {
            Object key = _enum.nextElement();
            props.put(key, p.get(key));
        }
        t.setProperties(props);
	
	    EclipseTestRunner runner= new EclipseTestRunner(t, testPluginName, haltError, haltFail);
        transferFormatters(runner);
        runner.run();
        return runner.getRetCode();
	}

    public EclipseTestRunner(JUnitTest test, String testPluginName, boolean haltOnError, boolean haltOnFailure) {
        fJunitTest= test;
        fTestPluginName= testPluginName;
        fHaltOnError= haltOnError;
        fHaltOnFailure= haltOnFailure;
         
        try {
            testClass= getTest(test.getName());
        } catch(Exception e) {
            fRetCode = ERRORS;
            fException = e;
        }
    }
	
	/**
	 * Returns the Test corresponding to the given suite. 
	 */
	protected Test getTest(String suiteClassName) throws TestFailedException {
		Bundle bundle = Platform.getBundle("org.junit");
		if (bundle == null)
			return null;
		String bundleVersion = (String) bundle.getHeaders().get(Constants.BUNDLE_VERSION);
		if (bundleVersion == null)
			return null;
		Version version;
		try {
			version = Version.parseVersion(bundleVersion);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			return null;
		}
		switch (version.getMajor()) {
			case 3 :
				return EclipseJunit3TestHelper.getTest(this, suiteClassName);
			default : //assume '4'
				return EclipseJunit4TestHelper.getTest(this, suiteClassName);
		}
	}
	
	protected void runFailed(String message) throws TestFailedException {
		System.err.println(message);
		throw new TestFailedException(message);
	}

    protected void runFailed(Throwable e) throws TestFailedException {
      e.printStackTrace();
      throw new TestFailedException(e);
    }
    
    /*
	 * Returns the Bundle specified by testPluginName
	 */
    private Bundle getTestBundle() {
		if (fTestPluginName == null)
			return null;
        Bundle bundle = Platform.getBundle(fTestPluginName);
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
				throw new RuntimeException("Could not find host for fragment:" + fTestPluginName,e);
			}
			Bundle host = Platform.getBundle(hostElement[0].getValue());
			//we really want to get the host not the fragment
			bundle = host;
		} 

        return bundle;
    }

	/**
	 * Loads the class either with the system class loader or a
	 * plugin class loader if a plugin name was specified
	 */
	protected Class loadSuiteClass(String suiteClassName) throws ClassNotFoundException {
		if (fTestPluginName == null)
			return Class.forName(suiteClassName);
        Bundle bundle = getTestBundle();
        if (bundle == null) {
            throw new ClassNotFoundException(suiteClassName, new Exception("Could not find plugin \""
                    + fTestPluginName + "\""));
        }
        return bundle.loadClass(suiteClassName);
	}
	
	public void run() {
//		IPerformanceMonitor pm = PerfMsrCorePlugin.getPerformanceMonitor(true);
		
        fTestResult= new TestResult();
        fTestResult.addListener(this);
        for (int i= 0; i < formatters.size(); i++) {
            fTestResult.addListener((TestListener)formatters.elementAt(i));
        }

        long start= System.currentTimeMillis();
        fireStartTestSuite();
        
        if (fException != null) { // had an exception in the constructor
            for (int i= 0; i < formatters.size(); i++) {
                ((TestListener)formatters.elementAt(i)).addError(null, fException);
            }
            fJunitTest.setCounts(1, 0, 1);
            fJunitTest.setRunTime(0);
        } else {
            ByteArrayOutputStream errStrm = new ByteArrayOutputStream();
            fSystemError= new PrintStream(errStrm);
            
            ByteArrayOutputStream outStrm = new ByteArrayOutputStream();
            fSystemOut= new PrintStream(outStrm);

            try {
//            	pm.snapshot(1); // before
            	testClass.run(fTestResult);
            } finally {
 //           	pm.snapshot(2); // after  	
                fSystemError.close();
                fSystemError= null;
                fSystemOut.close();
                fSystemOut= null;
                sendOutAndErr(new String(outStrm.toByteArray()), new String(errStrm.toByteArray()));
                fJunitTest.setCounts(fTestResult.runCount(), fTestResult.failureCount(), fTestResult.errorCount());
                fJunitTest.setRunTime(System.currentTimeMillis() - start);
            }
        }
        fireEndTestSuite();

        if (fRetCode != SUCCESS || fTestResult.errorCount() != 0) {
            fRetCode = ERRORS;
        } else if (fTestResult.failureCount() != 0) {
            fRetCode = FAILURES;
        }
        
//        pm.upload(getClass().getName());
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
    public void startTest(Test t) {}

    /*
     * @see TestListener.addFailure
     */
    public void endTest(Test test) {}

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
		for (int i= 0; i < formatters.size(); i++) {
            ((JUnitResultFormatter)formatters.elementAt(i)).startTestSuite(fJunitTest);
        }
    }

    private void fireEndTestSuite() {
        for (int i= 0; i < formatters.size(); i++) {
            ((JUnitResultFormatter)formatters.elementAt(i)).endTestSuite(fJunitTest);
        }
    }

    public void addFormatter(JUnitResultFormatter f) {
        formatters.addElement(f);
    }

    /**
     * Line format is: formatter=<classname>(,<pathname>)?
     */
    private static void createAndStoreFormatter(String line) throws BuildException {
        String formatterClassName= null;
        File formatterFile= null;
        
        int pos = line.indexOf(',');
        if (pos == -1) {
            formatterClassName= line;
        } else {
            formatterClassName= line.substring(0, pos);
            formatterFile= new File(line.substring(pos + 1)); // the method is package visible
        }
        fgFromCmdLine.addElement(createFormatter(formatterClassName, formatterFile));
    }

    private static void transferFormatters(EclipseTestRunner runner) {
        for (int i= 0; i < fgFromCmdLine.size(); i++) {
            runner.addFormatter((JUnitResultFormatter)fgFromCmdLine.elementAt(i));
        }
    }

	/*
	 * DUPLICATED from FormatterElement, since it is package visible only
	 */
    private static JUnitResultFormatter createFormatter(String classname, File outfile) throws BuildException {
    	OutputStream out= System.out;

        if (classname == null) {
            throw new BuildException("you must specify type or classname");
        }
        Class f = null;
        try {
            f= EclipseTestRunner.class.getClassLoader().loadClass(classname);
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
            throw new BuildException(classname+" is not a JUnitResultFormatter");
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
        for (int i=0; i<formatters.size(); i++) {
            JUnitResultFormatter formatter = 
                ((JUnitResultFormatter)formatters.elementAt(i));
            
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
}	
