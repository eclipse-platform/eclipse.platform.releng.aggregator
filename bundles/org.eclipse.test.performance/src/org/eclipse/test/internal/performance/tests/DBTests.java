package org.eclipse.test.internal.performance.tests;

import java.sql.SQLException;
import java.util.Date;

import org.eclipse.test.internal.performance.PerformanceTestPlugin;
import org.eclipse.test.internal.performance.data.DataPoint;
import org.eclipse.test.internal.performance.data.Dim;
import org.eclipse.test.internal.performance.data.Scalar;
import org.eclipse.test.internal.performance.db.DB;
import org.eclipse.test.internal.performance.db.Variations;
import org.eclipse.test.performance.PerformanceMeter;

import junit.framework.TestCase;

public class DBTests extends TestCase {
    
    private static final String SCENARIO_NAME_1= "testScenario1"; //$NON-NLS-1$
    private static final String DBLOC= "testDBs"; //$NON-NLS-1$
    private static String DBNAME;
    private static final String DBUSER= "testUser"; //$NON-NLS-1$
    private static final String DBPASSWD= "testPassword"; //$NON-NLS-1$
    
    
    protected void setUp() throws Exception {
        super.setUp();
        
        DBNAME= "testDB_" + (new Date().getTime()/1000); //$NON-NLS-1$
        
        System.setProperty("eclipse.perf.dbloc", DBLOC + ";dbname=" + DBNAME + ";dbuser=" + DBUSER + ";dbpasswd=" + DBPASSWD); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        System.setProperty("eclipse.perf.config", "config=test;build=b0001;jvm=sun142"); //$NON-NLS-1$ //$NON-NLS-2$
        System.setProperty("eclipse.perf.assertAgainst", "build=base"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }
    
    public void testSimple() throws SQLException {
        
        assertEquals(DBLOC, PerformanceTestPlugin.getDBLocation());
        assertEquals(DBNAME, PerformanceTestPlugin.getDBName());
        assertEquals(DBUSER, PerformanceTestPlugin.getDBUser());
        assertEquals(DBPASSWD, PerformanceTestPlugin.getDBPassword());
        
        assertEquals("|build=b0001||config=test||jvm=sun142|", PerformanceTestPlugin.getVariations().toExactMatchString()); //$NON-NLS-1$
        assertEquals("|build=base||config=test||jvm=sun142|", PerformanceTestPlugin.getAssertAgainst().toExactMatchString()); //$NON-NLS-1$
        
		PerformanceMeter pm= new TestPerformanceMeter(SCENARIO_NAME_1);
		
		pm.start();
		pm.stop();
		pm.commit();
		
		pm.dispose();
		
		Variations v= new Variations("test", "b0001"); //$NON-NLS-1$ //$NON-NLS-2$
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
    }
}
