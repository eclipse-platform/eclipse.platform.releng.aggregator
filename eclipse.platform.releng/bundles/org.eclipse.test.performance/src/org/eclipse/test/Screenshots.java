/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
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

import java.io.File;

import org.eclipse.core.runtime.Platform;
import org.eclipse.test.internal.AwtScreenshot;

/**
 * Helper class to take screenshots from running tests.
 *
 * @since 3.13
 */
public final class Screenshots {

    /**
     * Takes a screenshot and writes the path to the generated image file to System.out.
     * <p>
     * Workaround for missing {@link junit.framework.TestCase#getName()} in JUnit 4:
     * </p>
     *
     * <pre>
     * &#64;Rule
     * public TestName testName = new TestName();
     * </pre>
     *
     * @param testClass
     *            test class that takes the screenshot
     * @param name
     *            screenshot identifier (e.g. test name)
     * @return file system path to the screenshot file
     */
    public static String takeScreenshot(Class<?> testClass, String name) {
        File resultsDir = getResultsDirectory();
        String filename = new File(resultsDir.getAbsolutePath(), testClass.getName() + "." + name + ".png").getAbsolutePath();
        AwtScreenshot.dumpAwtScreenshot(filename);
        return filename;
    }

    /**
     * @return unspecified
     * @noreference This method is not intended to be referenced by clients.
     */
    public static File getResultsDirectory() {
        File resultsDir = getJunitReportOutput(); // ends up in testresults/linux.gtk.x86_6.0/<class>.<test>.png

        if (resultsDir == null) {
            File eclipseDir = new File("").getAbsoluteFile();
            if (isRunByGerritHudsonJob()) {
                resultsDir = new File(eclipseDir, "/../").getAbsoluteFile(); // ends up in the workspace root
            } else {
                resultsDir = new File(System.getProperty("java.io.tmpdir"));
            }
        }

        resultsDir.mkdirs();
        return resultsDir;
    }

    private static File getJunitReportOutput() {
        String[] args = Platform.getCommandLineArgs();
        for (int i = 0; i < args.length - 1; i++) {
            if ("-junitReportOutput".equals(args[i])) { // see library.xml and org.eclipse.test.EclipseTestRunner
                return new File(args[i + 1]).getAbsoluteFile();
            }
        }
        return null;
    }

    /**
     * @return unspecified
     * @noreference This method is not intended to be referenced by clients.
     */
    public static boolean isRunByGerritHudsonJob() {
        return System.getProperty("user.dir").matches(".*/(?:eclipse|rt\\.equinox)\\.[^/]+-Gerrit/.*");
    }

}
