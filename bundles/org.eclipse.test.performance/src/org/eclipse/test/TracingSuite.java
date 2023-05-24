/*******************************************************************************
 * Copyright (c) 2000, 2023 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.test;

import java.io.PrintStream;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.InaccessibleObjectException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.Runner;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;
import org.junit.runner.notification.StoppedByUserException;
import org.junit.runners.Suite;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.RunnerBuilder;

/**
 * The {@code TracingSuite} runner behaves like a normal {@link Suite}, but additionally logs the
 * start of each atomic test contained in the suite to {@code System.out}, and it tries to collect
 * more information after a timeout.
 * <p>
 * For atomic tests that run longer than 10 minutes, it tries to take a stack trace and a screenshot,
 * and then it tries to throw an IllegalStateException in the "main" thread. The exact behavior can be
 * configured using the {@link TracingOptions} annotation.
 * <p>
 * Usage: Modify an existing JUnit 4 suite class, or create a new one like this:
 * <pre>
{@literal @}RunWith(TracingSuite.class)
{@literal @}SuiteClasses(MyTestClass.class)
{@literal @}TracingOptions(stackDumpTimeoutSeconds = 5)
public class TracingMyTestClass { }
</pre>
 * Directly annotating an existing JUnit 4 class that contains atomic tests doesn't work (JUnit 4 design flaw).
 */
public class TracingSuite extends Suite {

    /**
     * Configuration options for classes annotated with {@code @RunWith(TracingSuite.class)}.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @Inherited
    public @interface TracingOptions {

        /**
         * @return true iff start times of atomic tests should be logged to {@code System.out}
         */
        public boolean logTestStart() default true;

        /**
         * @return the number of seconds after which a thread dump is initiated,
         *         or 0 if no timer should be started
         */
        public long stackDumpTimeoutSeconds() default 10 * 60;

        /**
         * @return true iff the runner should try to throw an
         *         {@link IllegalStateException} in the main thread after
         *         writing a stack dump. This sometimes makes an program proceed
         *         when the main thread was stuck in an endless loop.
         */
        public boolean throwExceptionInMainThread() default true;

        /**
         * @return the maximum number of screenshots that are taken
         */
        public int maxScreenshotCount() default 5;
    }

    private TracingOptions fTracingOptions;

    private class TracingRunNotifier extends RunNotifier {
        private RunNotifier fNotifier;
        private Timer fTimer = new Timer(true);
        private ConcurrentHashMap<Description, TimerTask> fRunningTests = new ConcurrentHashMap<>();

        public TracingRunNotifier(RunNotifier notifier) {
            fNotifier = notifier;
        }

        @Override
        public void addListener(RunListener listener) {
            fNotifier.addListener(listener);
        }

        @Override
        public void removeListener(RunListener listener) {
            fNotifier.removeListener(listener);
        }

        @Override
        public void fireTestRunStarted(Description description) {
            fNotifier.fireTestRunStarted(description);
        }

        @Override
        public void fireTestRunFinished(Result result) {
            fNotifier.fireTestRunFinished(result);
        }

        @Override
        public void fireTestStarted(Description description) throws StoppedByUserException {
            Date start = new Date();
            if (fTracingOptions.logTestStart()) {
                String message = format(start, description);
                System.out.println(message);
            }

            long seconds = fTracingOptions.stackDumpTimeoutSeconds();
            if (seconds != 0) {
                DumpTask task = new DumpTask(description);
                fRunningTests.put(description, task);
                fTimer.schedule(task, seconds * 1000);
            }
            fNotifier.fireTestStarted(description);
        }

        @Override
        public void fireTestFailure(Failure failure) {
            fNotifier.fireTestFailure(failure);
        }

        @Override
        public void fireTestAssumptionFailed(Failure failure) {
            fNotifier.fireTestAssumptionFailed(failure);
        }

        @Override
        public void fireTestIgnored(Description description) {
            fNotifier.fireTestIgnored(description);
        }

        @Override
        public void fireTestFinished(Description description) {
            TimerTask task = fRunningTests.remove(description);
            if (task != null) {
                task.cancel();
            }
            fNotifier.fireTestFinished(description);
        }

        @Override
        public void pleaseStop() {
            fNotifier.pleaseStop();
        }

        @Override
        public void addFirstListener(RunListener listener) {
            fNotifier.addFirstListener(listener);
        }
    }

    private class DumpTask extends TimerTask {
        private volatile int fScreenshotCount;
        private Description fDescription;

        public DumpTask(Description description) {
            fDescription = description;
        }

        @Override
        public void run() {
            // There are situation where a blocked main thread apparently also blocks output to
            // System.err. Try to dump to System.out first. If both dumps get through, the short
            // delay between the traces may even help identify threads that are still running.
            dumpStackTraces(System.out);
            try {
                Thread.sleep(100);
            } catch (InterruptedException e2) {
                // won't happen, continue
            }
            Thread main = dumpStackTraces(System.err);

            if (fScreenshotCount < fTracingOptions.maxScreenshotCount()) {
                String screenshotFile = Screenshots.takeScreenshot(TracingSuite.class, Integer.toString(fScreenshotCount++));
                System.err.println("Timeout screenshot saved to " + screenshotFile);
            }

            if (main != null && fTracingOptions.throwExceptionInMainThread()) {
                Throwable toThrow = new IllegalStateException("main thread killed by " + TracingSuite.class.getSimpleName() + " timeout");
                toThrow.initCause(new RuntimeException(toThrow.getMessage()));
                // Set the stack trace to that of the target thread.
                toThrow.setStackTrace(main.getStackTrace());
                // Thread#stop(Throwable) doesn't work any more in JDK 8. Try stop0:
                try {
                    Method stop0 = Thread.class.getDeclaredMethod("stop0", Object.class);
                    stop0.setAccessible(true);
                    stop0.invoke(main, toThrow);
                } catch (
                        NoSuchMethodException |
                        SecurityException |
                        IllegalAccessException |
                        IllegalArgumentException |
                        InaccessibleObjectException |
                        InvocationTargetException e1) {
                    e1.printStackTrace();
                }
            }
        }

        private Thread dumpStackTraces(PrintStream stream) {
            long seconds = fTracingOptions.stackDumpTimeoutSeconds();
            String message = format(new Date(), fDescription) + " ran for more than " + seconds + " seconds";
            stream.println(message);

            stream.format("totalMemory:           %11d\n", Runtime.getRuntime().totalMemory());
            stream.format("freeMemory (before GC):%11d\n", Runtime.getRuntime().freeMemory());
            System.gc();
            stream.format("freeMemory (after GC): %11d\n", Runtime.getRuntime().freeMemory());

            ThreadMXBean threadStuff = ManagementFactory.getThreadMXBean();
            ThreadInfo[] allThreads = threadStuff.dumpAllThreads(true, true, 200);
            for (ThreadInfo threadInfo : allThreads) {
                stream.print(threadInfo);
            }
            for (Thread t : Thread.getAllStackTraces().keySet()) {
                String name = t.getName();
                if ("main".equals(name)) {
                    return t;
                }
            }
            return null;
        }
    }

    static class ThreadDump extends Exception {
        private static final long serialVersionUID = 1L;
        ThreadDump(String message) {
            super(message);
        }
    }

    private static String format(Date time, Description description) {
        String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.US).format(time);
        String message = "[" + now + "] " + description.getClassName() + "#" + description.getMethodName() + "()";
        return message;
    }

    public TracingSuite(Class<?> klass, RunnerBuilder builder) throws InitializationError {
        super(klass, builder);
        fTracingOptions = klass.getAnnotation(TracingOptions.class);
        if (fTracingOptions == null) {
            @TracingOptions class DefaultTracingOptionsProvider { /* just an annotation holder */ }
            fTracingOptions = DefaultTracingOptionsProvider.class.getAnnotation(TracingOptions.class);
        }
    }

    @Override
    protected void runChild(Runner runner, RunNotifier notifier) {
        super.runChild(runner, new TracingRunNotifier(notifier));
    }
}