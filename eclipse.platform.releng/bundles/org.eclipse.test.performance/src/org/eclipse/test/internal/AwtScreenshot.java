/*******************************************************************************
 * Copyright (c) 2015, 2016 IBM Corporation and others.
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

package org.eclipse.test.internal;

import java.awt.AWTException;
import java.awt.HeadlessException;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.net.URL;

import javax.imageio.ImageIO;

public class AwtScreenshot {

    private static final int TIMEOUT_SECONDS = 15;

    public static void main(String[] args) {
        try {
            System.setProperty("java.awt.headless", "false");
            Robot robot = new Robot();
            Rectangle rect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
            BufferedImage image = robot.createScreenCapture(rect);
            File file = new File(args[0]);
            ImageIO.write(image, "png", file);

            System.out.println("AWT screenshot saved to: " + file.getAbsolutePath());
        } catch (HeadlessException | AWTException | IOException e) {
            e.printStackTrace();
        }
    }

    static class StreamForwarder extends Thread {

        private InputStream fProcessOutput;

        private PrintStream fStream;

        public StreamForwarder(InputStream processOutput, PrintStream stream) {
            fProcessOutput = processOutput;
            fStream = stream;
        }

        @Override
        public void run() {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(fProcessOutput))) {
                String line = null;
                while ((line = reader.readLine()) != null) {
                    fStream.println(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void dumpAwtScreenshot(String screenshotFile) {
        try {
            URL location = AwtScreenshot.class.getProtectionDomain().getCodeSource().getLocation();
            String cp = location.toURI().getPath();
            if (new File(cp).isDirectory() && !cp.endsWith(File.separatorChar + "bin" + File.separatorChar)) {
                cp += "bin" + File.separatorChar;
            }
            String javaHome = System.getProperty("java.home");
            String javaExe = javaHome + File.separatorChar + "bin" + File.separatorChar + "java";
            if (File.separatorChar == '\\') {
                javaExe += ".exe"; // assume it's Windows
            }
            String[] args = new String[] { javaExe, "-cp", cp, AwtScreenshot.class.getName(), screenshotFile };
            // System.out.println("Start process: " + Arrays.asList(args));
            ProcessBuilder processBuilder = new ProcessBuilder(args);
            if ("Mac OS X".equals(System.getProperty("os.name"))) {
                processBuilder.environment().put("AWT_TOOLKIT", "CToolkit");
            }
            Process process = processBuilder.start();
            new StreamForwarder(process.getErrorStream(), System.out).start();
            new StreamForwarder(process.getInputStream(), System.out).start();
            long end = System.currentTimeMillis() + TIMEOUT_SECONDS * 1000;
            boolean done = false;
            do {
                try {
                    process.exitValue();
                    done = true;
                } catch (IllegalThreadStateException e) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e1) {
                        // continue
                    }
                }
            } while (!done && System.currentTimeMillis() < end);

            if (done) {
                int exitCode = process.exitValue();
                if (exitCode != 0) {
                    System.out.println("AwtScreenshot VM finished with exit code " + exitCode + ".");
                }
            } else {
                process.destroy();
                System.out.println("Killed AwtScreenshot VM after " + TIMEOUT_SECONDS + " seconds.");
            }
        } catch (URISyntaxException | IOException e) {
            e.printStackTrace();
        }
    }
}
