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
    }
    
    public void testParseVariations() {
        Variations v1= new Variations("foo", null); //$NON-NLS-1$
        assertEquals(v1, new Variations(v1.toExactMatchString()));
        
        Variations v2= new Variations("foo", "bar");         //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals(v2, new Variations(v2.toExactMatchString()));
    }
}
