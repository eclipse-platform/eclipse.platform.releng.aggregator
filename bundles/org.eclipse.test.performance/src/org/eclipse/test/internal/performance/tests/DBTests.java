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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.test.internal.performance.PerformanceTestPlugin;
import org.eclipse.test.internal.performance.data.DataPoint;
import org.eclipse.test.internal.performance.data.Dim;
import org.eclipse.test.internal.performance.data.Scalar;
import org.eclipse.test.internal.performance.db.DB;
import org.eclipse.test.internal.performance.db.SummaryEntry;
import org.eclipse.test.internal.performance.db.Variations;
import org.eclipse.test.performance.Dimension;
import org.eclipse.test.performance.Performance;
import org.eclipse.test.performance.PerformanceMeter;

import junit.framework.TestCase;

public class DBTests extends TestCase {
    
    private static final String CONFIG= "c"; //$NON-NLS-1$
    private static final String BUILD= "b"; //$NON-NLS-1$
    
    private static final String SCENARIO_NAME_1= "bar.testScenario1"; //$NON-NLS-1$
    private static final String SCENARIO_NAME_2= "bar.testScenario2"; //$NON-NLS-1$
    private static final String SCENARIO_NAME_3= "foo.testScenario3"; //$NON-NLS-1$
    private static final String SCENARIO_NAME_4= "foo.testScenario4"; //$NON-NLS-1$
    private static final String SHORT_NAME_2= "ShortName2"; //$NON-NLS-1$
    private static final String SHORT_NAME_3= "ShortName3"; //$NON-NLS-1$
    private static final String SHORT_NAME_4= "ShortName4"; //$NON-NLS-1$

    private static final String DBLOC= "testDBs"; //$NON-NLS-1$
    private static String DBNAME;
    private static final String DBUSER= "testUser"; //$NON-NLS-1$
    private static final String DBPASSWD= "testPassword"; //$NON-NLS-1$
    
    
    protected void setUp() throws Exception {
        super.setUp();
        
        // generate a unique DB name
        DBNAME= "testDB_" + (new Date().getTime()/1000); //$NON-NLS-1$
        
        System.setProperty("eclipse.perf.dbloc", DBLOC + ";dbname=" + DBNAME + ";dbuser=" + DBUSER + ";dbpasswd=" + DBPASSWD); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        System.setProperty("eclipse.perf.config", CONFIG+"=test;"+BUILD+"=b0001;jvm=sun142"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        System.setProperty("eclipse.perf.assertAgainst", BUILD+"=base"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }
    
    public void testSimple() throws SQLException {
        
        assertEquals(DBLOC, PerformanceTestPlugin.getDBLocation());
        assertEquals(DBNAME, PerformanceTestPlugin.getDBName());
        assertEquals(DBUSER, PerformanceTestPlugin.getDBUser());
        assertEquals(DBPASSWD, PerformanceTestPlugin.getDBPassword());
        
        assertEquals("|"+BUILD+"=b0001||"+CONFIG+"=test||jvm=sun142|", PerformanceTestPlugin.getVariations().toExactMatchString()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        assertEquals("|"+BUILD+"=base||"+CONFIG+"=test||jvm=sun142|", PerformanceTestPlugin.getAssertAgainst().toExactMatchString()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        
        Performance perf= Performance.getDefault();
        
		PerformanceMeter pm1= new TestPerformanceMeter(SCENARIO_NAME_1);
		pm1.start();
		pm1.stop();
		pm1.commit();
		pm1.dispose();
		
		PerformanceMeter pm2= new TestPerformanceMeter(SCENARIO_NAME_2);
		perf.tagAsGlobalSummary(pm2, SHORT_NAME_2, new Dimension[] { Dimension.CPU_TIME, Dimension.USED_JAVA_HEAP } );
		pm2.start();
		pm2.stop();
		pm2.commit();
		pm2.dispose();

		PerformanceMeter pm3= new TestPerformanceMeter(SCENARIO_NAME_3);
		perf.tagAsGlobalSummary(pm3, SHORT_NAME_3, Dimension.CPU_TIME);
		pm3.start();
		pm3.stop();
		pm3.commit();
		pm3.dispose();

		PerformanceMeter pm4= new TestPerformanceMeter(SCENARIO_NAME_4);
		perf.tagAsSummary(pm4, SHORT_NAME_4, Dimension.USED_JAVA_HEAP);
		pm4.start();
		pm4.stop();
		pm4.commit();
		pm4.dispose();

		//
		
		Variations v= new Variations();
		v.put(CONFIG, "test"); //$NON-NLS-1$
		v.put(BUILD, "b0001"); //$NON-NLS-1$
		v.put("jvm", "sun142"); //$NON-NLS-1$ //$NON-NLS-2$
		DataPoint[] points= DB.queryDataPoints(v, SCENARIO_NAME_1, null);
		assertEquals(1, points.length);
		
		DataPoint dp= points[0];
		Dim[] dimensions= dp.getDimensions();
		assertEquals(2, dimensions.length);
		
		Scalar s1= dp.getScalar(TestPerformanceMeter.TESTDIM1);
		assertNotNull(s1);
		assertEquals(900, s1.getMagnitude());

		Scalar s2= dp.getScalar(TestPerformanceMeter.TESTDIM2);
		assertNotNull(s2);
		assertEquals(1000, s2.getMagnitude());

		//
		Set dims= new HashSet();
		dims.add(TestPerformanceMeter.TESTDIM2);
		points= DB.queryDataPoints(v, SCENARIO_NAME_1, dims);
		assertEquals(1, points.length);
		dimensions= points[0].getDimensions();
		assertEquals(1, dimensions.length);
		Scalar s= points[0].getScalar(TestPerformanceMeter.TESTDIM2);
		assertNotNull(s);	
		assertEquals(1000, s.getMagnitude());
		
		//
		List buildNames= new ArrayList();
		Variations v2= new Variations();
		v2.put(CONFIG, "%"); //$NON-NLS-1$
		v2.put(BUILD, "%"); //$NON-NLS-1$
		DB.queryDistinctValues(buildNames, BUILD, v2, "%"); //$NON-NLS-1$
		assertEquals(1, buildNames.size());
		assertEquals("b0001", buildNames.get(0)); //$NON-NLS-1$
		
	    SummaryEntry[] fps= DB.queryGlobalSummaries(PerformanceTestPlugin.getVariations());
	    assertEquals(3, fps.length);
	    
	    assertEquals(SCENARIO_NAME_2, fps[0].scenarioName);
	    assertEquals(SHORT_NAME_2, fps[0].shortName);
	    assertEquals(Dimension.USED_JAVA_HEAP, fps[0].dimension);

	    assertEquals(SCENARIO_NAME_2, fps[1].scenarioName);
	    assertEquals(SHORT_NAME_2, fps[1].shortName);
	    assertEquals(Dimension.CPU_TIME, fps[1].dimension);

	    assertEquals(SCENARIO_NAME_3, fps[2].scenarioName);
	    assertEquals(SHORT_NAME_3, fps[2].shortName);
	    assertEquals(Dimension.CPU_TIME, fps[2].dimension);
	    
	    
	    SummaryEntry[] fps2= DB.querySummaries(PerformanceTestPlugin.getVariations(), "foo.%");
	    assertEquals(2, fps2.length);
	    
	    assertEquals(SCENARIO_NAME_3, fps2[0].scenarioName);
	    assertEquals(SHORT_NAME_3, fps2[0].shortName);
	    assertEquals(Dimension.CPU_TIME, fps2[0].dimension);

	    assertEquals(SCENARIO_NAME_4, fps2[1].scenarioName);
	    assertEquals(SHORT_NAME_4, fps2[1].shortName);
	    assertEquals(Dimension.USED_JAVA_HEAP, fps2[1].dimension);
    }
}
