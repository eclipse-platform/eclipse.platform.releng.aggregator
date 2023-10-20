/*******************************************************************************
 * Copyright (c) 2004, 2005 IBM Corporation and others.
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

package org.eclipse.test.internal.performance.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.eclipse.test.internal.performance.db.Variations;
import org.junit.jupiter.api.Test;

public class VariationsTests {

    @Test
    public void testVariations() {
        Variations v1 = new Variations();
        v1.put("k1", "foo"); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("|k1=foo|", v1.toExactMatchString()); //$NON-NLS-1$
        assertEquals("%|k1=foo|%", v1.toQueryPattern()); //$NON-NLS-1$

        Variations v2 = new Variations();
        v2.put("k1", "foo"); //$NON-NLS-1$ //$NON-NLS-2$
        v2.put("k2", "bar"); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("|k1=foo||k2=bar|", v2.toExactMatchString()); //$NON-NLS-1$
        assertEquals("%|k1=foo|%|k2=bar|%", v2.toQueryPattern()); //$NON-NLS-1$

        Variations v3 = new Variations();
        v3.put("k1", "foo"); //$NON-NLS-1$ //$NON-NLS-2$
        v3.put("k2", "bar"); //$NON-NLS-1$ //$NON-NLS-2$
        v3.put("k3", "xyz"); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("|k1=foo||k2=bar||k3=xyz|", v3.toExactMatchString()); //$NON-NLS-1$
        assertEquals("%|k1=foo|%|k2=bar|%|k3=xyz|%", v3.toQueryPattern()); //$NON-NLS-1$
    }

    @Test
    public void testParseVariations() {
        Variations v1 = new Variations();
        v1.put("k1", "foo"); //$NON-NLS-1$ //$NON-NLS-2$

        Variations v = new Variations();
        v.parseDB(v1.toExactMatchString());
        assertEquals(v1, v);

        Variations v2 = new Variations();
        v2.put("k1", "foo"); //$NON-NLS-1$ //$NON-NLS-2$
        v2.put("k2", "bar"); //$NON-NLS-1$ //$NON-NLS-2$
        v = new Variations();
        v.parseDB(v2.toExactMatchString());
        assertEquals(v2, v);

        Variations v3 = new Variations();
        v3.put("k1", "foo"); //$NON-NLS-1$ //$NON-NLS-2$
        v3.put("k2", "bar"); //$NON-NLS-1$ //$NON-NLS-2$
        v3.put("k3", "xyz"); //$NON-NLS-1$ //$NON-NLS-2$
        v = new Variations();
        v.parseDB(v3.toExactMatchString());
        assertEquals(v3, v);
    }

    // public void testMaxLength() {
    // StringBuffer sb= new StringBuffer();
    // for (int i= 0; i < 1000; i++)
    //            sb.append("0123456789"); //$NON-NLS-1$
    // Variations v= new Variations(sb.toString(), null);
    //
    // String s1= v.toExactMatchString();
    // assertTrue(s1.length() == 10000);
    //
    // String s2= v.toQueryPattern();
    // assertTrue(s2.length() == 10000);
    // }
}
