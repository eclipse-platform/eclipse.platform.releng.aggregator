
package org.eclipse.releng.tests;

import java.util.Properties;

import org.junit.Before;
import org.junit.Test;

public class CheckMacSignatures {

    static boolean runningOnMac;
    static String  eclipseInstall;

    public CheckMacSignatures() {

    }

    @Before
    public void checkIfOnMac() {
        String os = System.getProperty("osgi.os");
        if ("macosx".equals(os)) {
            runningOnMac = true;
            eclipseInstall = System.getProperty("eclipse.install.location");
        }
        // temp
        System.out.println("eclipse.home: " + System.getProperty("eclipse.home"));
        System.out.println("eclipse.home.location: " + System.getProperty("eclipse.home.location"));
        System.out.println("All properties");
        Properties allProperties = System.getProperties();
        allProperties.list(System.out);
    }

    @Test
    public void checkSignature() {
        if (!runningOnMac) {
            System.out.println("Not running on Mac. No need to check Mac signature");
        } else {
            System.out.println("Eclipse Install location: " + eclipseInstall);
        }
    }
}
