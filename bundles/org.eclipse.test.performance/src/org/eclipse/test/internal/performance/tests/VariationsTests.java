/*******************************************************************************
 * Copyright (c) 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.test.internal.performance.tests;

import org.eclipse.test.internal.performance.db.Variations;

import junit.framework.TestCase;

public class VariationsTests extends TestCase {

    public void testVariations() {
        Variations v1= new Variations("foo", null);         //$NON-NLS-1$
        assertEquals("|config=foo|", v1.toExactMatchString()); //$NON-NLS-1$
        assertEquals("%|config=foo|%", v1.toQueryPattern()); //$NON-NLS-1$
        
        Variations v2= new Variations("foo", "bar");         //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("|build=bar||config=foo|", v2.toExactMatchString()); //$NON-NLS-1$
        assertEquals("%|build=bar|%|config=foo|%", v2.toQueryPattern()); //$NON-NLS-1$

        Variations v3= new Variations("foo", "bar");         //$NON-NLS-1$ //$NON-NLS-2$
        v3.put("abc", "xyz"); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("|abc=xyz||build=bar||config=foo|", v3.toExactMatchString()); //$NON-NLS-1$
        assertEquals("%|abc=xyz|%|build=bar|%|config=foo|%", v3.toQueryPattern()); //$NON-NLS-1$
}
    
    public void testParseVariations() {
        Variations v1= new Variations("foo", null); //$NON-NLS-1$
        assertEquals(v1, new Variations(v1.toExactMatchString()));
        
        Variations v2= new Variations("foo", "bar");         //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals(v2, new Variations(v2.toExactMatchString()));

        Variations v3= new Variations("foo", "bar");         //$NON-NLS-1$ //$NON-NLS-2$
        v3.put("abc", "xyz"); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals(v3, new Variations(v3.toExactMatchString()));
    }
}
