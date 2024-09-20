/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Test suite with user-specified test order (fails if not all test methods are listed) or bytecode declaration order.
 *
 * <p>
 * <b>Background:</b> {@link java.lang.Class#getDeclaredMethods()} does not specify the order of the methods. Up to Java SE 6, the
 * methods were usually sorted in declaration order, but in Java SE 7 and later, the order is random. This class guarantees reliable
 * test execution order even for questionable VM implementations.
 * </p>
 *
 * @since 3.8
 */
public class OrderedTestSuite extends TestSuite {

    /**
     * Creates a new ordered test suite that runs tests in the specified execution order.
     *
     * @param testClass
     *            the JUnit-3-style test class
     * @param testMethods
     *            the names of all test methods in the expected execution order
     */
    public OrderedTestSuite(final Class<? extends TestCase> testClass, String[] testMethods) {
        super(testClass.getName());

        Set<String> existingMethods = new HashSet<>();
        Method[] methods = testClass.getMethods(); // just public member methods
        for (Method method : methods) {
            existingMethods.add(method.getName());
        }

        for (final String testMethod : testMethods) {
            if (existingMethods.remove(testMethod)) {
                addTest(createTest(testClass, testMethod));
            } else {
                addTest(error(testClass, testMethod, new IllegalArgumentException("Class '" + testClass.getName() + " misses test method '" + testMethod + "'."))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            }
        }

        for (String existingMethod : existingMethods) {
            if (existingMethod.startsWith("test")) { //$NON-NLS-1$
                addTest(error(testClass, existingMethod, new IllegalArgumentException("Test method '" + existingMethod + "' not listed in OrderedTestSuite of class '" + testClass.getName() + "'."))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            }
        }

    }

    /**
     * Creates a new test suite that runs tests in bytecode declaration order.
     *
     * @param testClass
     *            the JUnit-3-style test class
     * @since 3.9
     */
    public OrderedTestSuite(Class<? extends TestCase> testClass) {
        this(testClass, testClass.getName());
    }

    /**
     * Creates a new test suite that runs tests in bytecode declaration order.
     *
     * @param testClass
     *            the JUnit-3-style test class
     * @param name
     *            the name of the suite
     * @since 3.9
     */
    public OrderedTestSuite(Class<? extends TestCase> testClass, String name) {
        super(name);

        TestSuite vmOrderSuite = new TestSuite(testClass);
        ArrayList<Test> tests = Collections.list(vmOrderSuite.tests());

        class SortingException extends RuntimeException {

            private static final long serialVersionUID = 1L;

            public SortingException(String message) {
                super(message);
            }
        }

        try {
            final List<String> orderedMethodNames = getBytecodeOrderedTestNames(testClass);
            tests.sort((o1, o2) -> {
                if (o1 instanceof TestCase && o2 instanceof TestCase) {
                    TestCase t1 = (TestCase) o1;
                    TestCase t2 = (TestCase) o2;
                    int i1 = orderedMethodNames.indexOf(t1.getName());
                    int i2 = orderedMethodNames.indexOf(t2.getName());
                    if (i1 != -1 && i2 != -1)
                        return i1 - i2;
                }
                throw new SortingException("suite failed to detect test order: " + o1 + ", " + o2); //$NON-NLS-1$ //$NON-NLS-2$
            });
        } catch (SortingException | IOException e) {
            addTest(error(testClass, "suite failed to detect test order", e)); //$NON-NLS-1$
        }

        for (Test test : tests) {
            addTest(test);
        }
    }

    /**
     * Returns the names of JUnit-3-style test cases declared in the given <code>testClass</code> and its superclasses, in bytecode
     * declaration order, listing test cases from a class before the non-overridden test cases from its superclass.
     *
     * @param testClass
     *            the JUnit-3-style test class
     * @return a modifiable <code>List&lt;String&gt;</code> of test names in bytecode declaration order
     * @throws IOException
     *             if an I/O error occurs.
     *
     * @since 3.10
     */
    public static List<String> getBytecodeOrderedTestNames(Class<? extends TestCase> testClass) throws IOException {
        ArrayList<String> orderedMethodNames = new ArrayList<>();
        Class<?> c = testClass;
        while (Test.class.isAssignableFrom(c)) {
            addDeclaredTestMethodNames(c, orderedMethodNames);
            c = c.getSuperclass();
        }
        return orderedMethodNames;
    }

    private static void addDeclaredTestMethodNames(Class<?> c, ArrayList<String> methodNames) throws IOException {
        /*
         * XXX: This method needs to be updated if new constant pool tags are specified. Current supported major class file version:
         * 52 (Java SE 8).
         *
         * See JVMS 8, 4.4 The Constant Pool.
         */
        String className = c.getName();
        int lastDot = className.lastIndexOf("."); //$NON-NLS-1$
        if (lastDot != -1)
            className = className.substring(lastDot + 1);
        DataInputStream is = new DataInputStream(new BufferedInputStream(c.getResourceAsStream(className + ".class"))); //$NON-NLS-1$
        int magic = is.readInt();
        if (magic != 0xcafebabe)
            throw new IOException("bad magic bytes: 0x" + Integer.toHexString(magic)); //$NON-NLS-1$
        int minor = is.readUnsignedShort();
        int major = is.readUnsignedShort();
        int cpCount = is.readUnsignedShort();
        String[] constantPoolStrings = new String[cpCount];
        for (int i = 1; i < cpCount; i++) {

            byte tag = is.readByte();
            switch (tag) {
                case 7: // CONSTANT_Class
                    skip(is, 2);
                    break;
                case 9: // CONSTANT_Fieldref
                case 10: // CONSTANT_Methodref
                case 11: // CONSTANT_InterfaceMethodref
                    skip(is, 4);
                    break;
                case 8: // CONSTANT_String
                    skip(is, 2);
                    break;
                case 3: // CONSTANT_Integer
                case 4: // CONSTANT_Float
                    skip(is, 4);
                    break;
                case 5: // CONSTANT_Long
                case 6: // CONSTANT_Double
                    skip(is, 8);
                    i++; // weird spec wants this
                    break;
                case 12: // CONSTANT_NameAndType
                    skip(is, 4);
                    break;
                case 1: // CONSTANT_Utf8
                    constantPoolStrings[i] = is.readUTF();
                    break;
                case 15: // CONSTANT_MethodHandle
                    skip(is, 3);
                    break;
                case 16: // CONSTANT_MethodType
                    skip(is, 2);
                    break;
                case 18: // CONSTANT_InvokeDynamic
                    skip(is, 4);
                    break;
                default:
                    throw new IOException("unknown constant pool tag " + tag + " at index " + i + ". Class file version: " + major + "." + minor); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
            }
        }
        skip(is, 2 * 3); // access_flags, this_class, super_class
        int interfacesCount = is.readUnsignedShort();
        skip(is, 2 * interfacesCount);
        int fieldsCount = is.readUnsignedShort();
        for (int i = 0; i < fieldsCount; i++) {
            skip(is, 2 * 3); // access_flags, name_index, descriptor_index
            int attributesCount = is.readUnsignedShort();
            for (int j = 0; j < attributesCount; j++) {
                skip(is, 2); // attribute_name_index
                long attInfoCount = readUnsignedInt(is);
                skip(is, attInfoCount);
            }
        }

        int methodsCount = is.readUnsignedShort();
        for (int i = 0; i < methodsCount; i++) {
            skip(is, 2); // access_flags
            int nameIndex = is.readUnsignedShort();
            int descIndex = is.readUnsignedShort();
            if ("()V".equals(constantPoolStrings[descIndex])) { //$NON-NLS-1$
                String name = constantPoolStrings[nameIndex];
                if (name.startsWith("test") && !methodNames.contains(name)) //$NON-NLS-1$
                    methodNames.add(name);
            }
            int attributesCount = is.readUnsignedShort();
            for (int j = 0; j < attributesCount; j++) {
                skip(is, 2); // attribute_name_index
                long attInfoCount = readUnsignedInt(is);
                skip(is, attInfoCount);
            }
        }
    }

    private static void skip(DataInputStream is, long bytes) throws IOException {
        while (bytes > 0)
            bytes -= is.skip(bytes);
        if (bytes != 0)
            throw new IOException("error in skipping bytes: " + bytes); //$NON-NLS-1$
    }

    private static long readUnsignedInt(DataInputStream is) throws IOException {
        return is.readInt() & 0xFFFFffffL;
    }

    private static Test error(Class<? extends TestCase> testClass, String testMethod, Exception exception) {
        final Throwable e2 = exception.fillInStackTrace();
        return new TestCase(testMethod + "(" + testClass.getName() + ")") { //$NON-NLS-1$ //$NON-NLS-2$

            @Override
            protected void runTest() throws Throwable {
                throw e2;
            }
        };
    }
}
