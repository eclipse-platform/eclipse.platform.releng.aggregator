package org.eclipse.test.internal.performance.db;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.test.internal.performance.InternalDimensions;
import org.eclipse.test.internal.performance.data.DataPoint;
import org.eclipse.test.internal.performance.data.Dim;
import org.eclipse.test.performance.Dimension;
import org.eclipse.test.performance.Performance;
import org.eclipse.test.performance.PerformanceMeter;


public class DBHelpers {
        
    private Connection fConnection;
    
    
    public static void main(String[] args) throws SQLException {
                
        //System.setProperty("eclipse.perf.dbloc", "net://localhost"); //$NON-NLS-1$ //$NON-NLS-2$
        //System.setProperty("eclipse.perf.dbloc", "/Volumes/Stuff/Eclipse/Dani2/cloudscape");
        //System.setProperty("eclipse.perf.config", "config=awtest;build=b0");

        //System.setProperty("perf_ctrl", "dbloc=net://relengbuildserv.ottawa.ibm.com;config=awtest;build=b0");
        
        DBHelpers db= new DBHelpers();
        
		String outFile= null;
		//outFile= "out.txt";	//$NON-NLS-1$
		PrintStream ps= null;
		if (outFile != null) {
		    try {
                ps= new PrintStream(new BufferedOutputStream(new FileOutputStream(outFile)));
            } catch (FileNotFoundException e) {
                System.err.println("can't create output file"); //$NON-NLS-1$
            }
		}
		if (ps == null)
		    ps= System.out;

		long start= System.currentTimeMillis();
		
		//db.dumpTable(ps, "SUMMARYENTRY", 1000);
		//db.dumpSummaries("relengbuildwin2", "N200410192000", "org.eclipse.jdt.text.%");
		
		//db.count(ps);
		//db.countAllDimensions(ps);
		//db.removeDimension(InternalDimensions.BYTES_WRITTEN);
		
		//db.countSamples(ps, new Variations("relengbuildwin2", "3.0.0_200410130800"));
		//db.countDatapoints(ps, new Variations("relengbuildwin2", "3.0.0_200410130800"));
		//db.countScalars(ps, new Variations("relengbuildwin2", "3.0.0_200410130800"));
		
		//db.removeSamples(ps, new Variations("awtest", "b0"));		
		//db.test();
		//db.view(ps, "localhost", "%", "%");	//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		//db.view(ps, "relengbuildwin2", "3%", "%OpenJava%2%");	//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		//db.dumpTable(ps, "VARIATION", 1000);
		//db.dumpScenarios(ps, "test_scenario%"); //$NON-NLS-1$
		//db.countSamplesWithNullVariations();
        //db.dumpSizes(ps);
		//db.dumpAll(ps, 100);
		
		
//		Variations v= new Variations("relengbuildwin2", "3.1M1_200409212000");
//		Set set= new HashSet();
//		set.add(Dimension.CPU_TIME);
//		DataPoint[] points= DB.queryDataPoints(v, "org.eclipse.jdt.text.tests.performance.RevertJavaEditorTest#testRevertJavaEditor()", null); //$NON-NLS-1$ //$NON-NLS-2$
//		//DataPoint[] points= DB.queryDataPoints("localhost", "R3.0", "%OpenJava%2%", new Dim[] {InternalDimensions.CPU_TIME}); //$NON-NLS-1$ //$NON-NLS-2$
//		//DataPoint[] points= DB.queryDataPoints("localhost", "R3.0", "org.eclipse.jdt.text.tests.performance.OpenJavaEditorTest#testOpenJavaEditor2()", dims); //$NON-NLS-1$ //$NON-NLS-2$
//		System.out.println("dps: " + points.length);
				
		
        System.out.println("time: " + ((System.currentTimeMillis()-start)/1000.0)); //$NON-NLS-1$
        
        if (ps != System.out)
            ps.close();
    }

    public DBHelpers() {
        fConnection= DB.getConnection();
    }
    
    void test() throws SQLException {
        Statement stmt= fConnection.createStatement();
        //stmt.execute("drop table CONFIG_ORG"); //$NON-NLS-1$
        stmt.close(); 
    }
    
    void dumpSummaries(String config, String build, String scenarioPattern) {
        Variations variations= new Variations();
        variations.put("config", config);
        variations.put("build", build);
        SummaryEntry[] summries= DB.querySummaries(variations, scenarioPattern);
        for (int i= 0; i < summries.length; i++) {
            SummaryEntry entry= summries[i];
            System.out.println(entry);
        }
    }
    
    void count(PrintStream ps) throws SQLException {
        PreparedStatement stmt= fConnection.prepareStatement("select count(*) from SCALAR where DATAPOINT_ID not in (select DATAPOINT.ID from DATAPOINT)"); //$NON-NLS-1$
        ResultSet set= stmt.executeQuery();
        if (set.next())
            ps.println("count: " + set.getInt(1));
        set.close();
        stmt.close();
    }

    void countDimension(PrintStream ps, Dim dim) throws SQLException {
        PreparedStatement stmt= fConnection.prepareStatement("select count(*) from SCALAR where DIM_ID = ?");
        stmt.setInt(1, dim.getId());
        ResultSet set= stmt.executeQuery();
        if (set.next())
            ps.println("dimension " + dim + ": " + set.getInt(1));
    }
    
    void countAllDimensions(PrintStream ps) throws SQLException {
        PreparedStatement stmt= fConnection.prepareStatement("select distinct DIM_ID from SCALAR");
        ResultSet set= stmt.executeQuery();
        while (set.next()) {
            Dim dimension= Dim.getDimension(set.getInt(1));
            if (dimension != null)
                countDimension(ps, dimension);
        }
    }

    void countSamples(PrintStream ps, Variations v) throws SQLException {
        PreparedStatement stmt= fConnection.prepareStatement("select count(*) from SAMPLE, VARIATION where VARIATION.KEYVALPAIRS = ? and SAMPLE.VARIATION_ID = VARIATION.ID");
        stmt.setString(1, v.toExactMatchString());
        ResultSet set= stmt.executeQuery();
        if (set.next())
            ps.println("samples with variation " + v + ": " + set.getInt(1));
    }
    
    void countDatapoints(PrintStream ps, Variations v) throws SQLException {
        PreparedStatement stmt= fConnection.prepareStatement("select count(*) from DATAPOINT, SAMPLE, VARIATION where VARIATION.KEYVALPAIRS = ? and SAMPLE.VARIATION_ID = VARIATION.ID and DATAPOINT.SAMPLE_ID= SAMPLE.ID");
        stmt.setString(1, v.toExactMatchString());
        ResultSet set= stmt.executeQuery();
        if (set.next())
            ps.println("datapoints with variation " + v + ": " + set.getInt(1));
    }
    
    void countScalars(PrintStream ps, Variations v) throws SQLException {
        PreparedStatement stmt= fConnection.prepareStatement("select count(*) from SCALAR, DATAPOINT, SAMPLE, VARIATION where VARIATION.KEYVALPAIRS = ? and SAMPLE.VARIATION_ID = VARIATION.ID and DATAPOINT.SAMPLE_ID= SAMPLE.ID and DATAPOINT.ID = SCALAR.DATAPOINT_ID");
        stmt.setString(1, v.toExactMatchString());
        ResultSet set= stmt.executeQuery();
        if (set.next())
            ps.println("scalars with variation " + v + ": " + set.getInt(1));
    }
    
    private void removeSamples(PrintStream ps, Variations v) throws SQLException {
        
        int sample_cnt= 0, dp_cnt= 0, scenario_cnt= 0;
        boolean delete= true;
        
        ps.println("removing:");
        
        int variation_id= 0;
        PreparedStatement stmt= fConnection.prepareStatement("select ID from VARIATION where KEYVALPAIRS = ?");
        stmt.setString(1, v.toExactMatchString());
        ResultSet set= stmt.executeQuery();
        if (set.next()) {
            variation_id= set.getInt(1);
            System.err.println("variation_id: " + variation_id);
        }
        
        if (variation_id <= 0) {
            System.err.println("nothing found for variation " + v);
        	return;
    	}

        PreparedStatement iterSamples= fConnection.prepareStatement("select SAMPLE.ID, SAMPLE.SCENARIO_ID from SAMPLE where SAMPLE.VARIATION_ID = ?"); //$NON-NLS-1$
        PreparedStatement iterDatapoints= fConnection.prepareStatement("select DATAPOINT.ID from DATAPOINT where DATAPOINT.SAMPLE_ID = ?");
        
        PreparedStatement deleteScalars= fConnection.prepareStatement("delete from SCALAR where DATAPOINT_ID = ?");
        PreparedStatement deleteDatapoints= fConnection.prepareStatement("delete from DATAPOINT where SAMPLE_ID = ?");
        PreparedStatement deleteSamples= fConnection.prepareStatement("delete from SAMPLE where SAMPLE.ID = ?");
        PreparedStatement deleteScenario= fConnection.prepareStatement("delete from SCENARIO where SCENARIO.ID = ?");
        
        ResultSet samples= null, datapoints= null, configs= null;
        iterSamples.setInt(1, variation_id);
        samples= iterSamples.executeQuery();
        while (samples.next()) {
            int sample_id= samples.getInt(1);
            int scenario_id= samples.getInt(2);
            ps.println(" sample: " + sample_id);
            iterDatapoints.setInt(1, sample_id);
	        datapoints= iterDatapoints.executeQuery();
	        int dps= 0;
	        while (datapoints.next()) {
	            int dp_id= datapoints.getInt(1);
	            ps.println("  dp: " + dp_id);
	            if (delete) {
	                deleteScalars.setInt(1, dp_id);
	                try {
                        deleteScalars.executeUpdate();
    		            fConnection.commit();
    		            dp_cnt++;
    		            dps++;
                    } catch (SQLException e) {
                        System.err.println("removing scalars: " + e);
                    }
	            }
	        }
	        if (delete) {
	            deleteDatapoints.setInt(1, sample_id);
	            try {
                    deleteDatapoints.executeUpdate();
                    fConnection.commit();
                } catch (SQLException e1) {
                    System.err.println("removing datapoints: " + e1);
                }
	            
	            deleteSamples.setInt(1, sample_id);
	            try {
                    deleteSamples.executeUpdate();
                    fConnection.commit();
                    sample_cnt++;
                } catch (SQLException e) {
                    System.err.println("removing sample: " + e);
                }
                
                deleteScenario.setInt(1, scenario_id);
	            try {
	                deleteScenario.executeUpdate();
                    fConnection.commit();
                    scenario_cnt++;
                } catch (SQLException e) {
                    System.err.println("removing scenario: " + e);
                }
	        }
        }
        if (delete) {
            PreparedStatement deleteSummaries= fConnection.prepareStatement("delete from SUMMARYENTRY where VARIATION_ID = ?"); //$NON-NLS-1$
            deleteSummaries.setInt(1, variation_id);
            deleteSummaries.executeUpdate();
            deleteSummaries.close();
            
            PreparedStatement deleteVariation= fConnection.prepareStatement("delete from VARIATION where ID = ?"); //$NON-NLS-1$
            deleteVariation.setInt(1, variation_id);
            deleteVariation.executeUpdate();
            deleteVariation.close();
            
        }
        ps.println("  samples: " + sample_cnt);
        ps.println("  scenarios: " + scenario_cnt);
        ps.println("  datapoints: " + dp_cnt);
        
        if (configs != null) configs.close();
        if (samples != null) samples.close();
        if (datapoints != null) datapoints.close();
        
        if (iterSamples != null) iterSamples.close();
        if (iterDatapoints != null) iterDatapoints.close();
        
        if (deleteSamples != null) deleteSamples.close();
        if (deleteScenario != null) deleteScenario.close();
        if (deleteScalars != null) deleteScalars.close();
        if (deleteDatapoints != null) deleteDatapoints.close();
    }

    private void countSamplesWithNullVariations() throws SQLException {
        Statement stmt= fConnection.createStatement();
        ResultSet rs= stmt.executeQuery("select count(*) from SAMPLE where SAMPLE.VARIATION_ID is null"); //$NON-NLS-1$
        while (rs.next()) {
            int config_id= rs.getInt(1);
            System.out.println("samples with NULL variation: " + config_id);
            //System.out.println(" " + rs.getString(2));
            //removeSamples(config_id);
        }
        rs.close();
        stmt.close();
    }
   
    private void removeDimension(Dim dim) throws SQLException {
        PreparedStatement q= fConnection.prepareStatement("delete from SCALAR where SCALAR.DIM_ID = ?"); //$NON-NLS-1$
        q.setInt(1, dim.getId());
        q.executeUpdate();
        q.close();
    }
    
    private void dumpScenarios(PrintStream ps, String pattern) throws SQLException {
        PreparedStatement stmt= fConnection.prepareStatement("select NAME from SCENARIO where NAME like ? order by NAME"); //$NON-NLS-1$
        stmt.setString(1, pattern);
        ResultSet rs= stmt.executeQuery();
        while (rs.next())
            ps.println(rs.getString(1));
        rs.close();
        stmt.close();
    }
    
    private void dumpSizes(PrintStream ps) throws SQLException {
        if (fConnection == null)
            return;    
        Statement stmt= fConnection.createStatement();
        try {
	        ResultSet rs= stmt.executeQuery("SELECT sys.systables.tablename FROM sys.systables WHERE sys.systables.tablename NOT LIKE 'SYS%' "); //$NON-NLS-1$
	        while (rs.next())
	            dumpSize(ps, rs.getString(1));
	        rs.close();   
        } finally {
            stmt.close();
        }
    }

    private void dumpSize(PrintStream ps, String table) throws SQLException {
        Statement stmt= fConnection.createStatement();
        ResultSet rs= stmt.executeQuery("select Count(*) from " + table); //$NON-NLS-1$
        if (rs.next())
            ps.println(table + ": " + rs.getInt(1)); //$NON-NLS-1$
        rs.close();   
        stmt.close();
    }
    
    public void dumpAll(PrintStream ps, int maxRow) throws SQLException {
        if (fConnection == null)
            return;
        if (maxRow < 0)
            maxRow= 1000000;
		Statement stmt= fConnection.createStatement();
        try {
	        ResultSet rs= stmt.executeQuery("select SYS.SYSTABLES.TABLENAME from SYS.SYSTABLES where SYS.SYSTABLES.TABLENAME not like 'SYS%' "); //$NON-NLS-1$
	        while (rs.next()) {
	            dumpTable(ps, rs.getString(1), maxRow);
	            ps.println();
	        }
	        rs.close();
        } finally {
            stmt.close();
        }
    }
    
    private void dumpTable(PrintStream ps, String tableName, int maxRow) throws SQLException {
        ps.print(tableName + '(');
		Statement select= fConnection.createStatement();
        ResultSet result= select.executeQuery("select * from " + tableName); //$NON-NLS-1$
        ResultSetMetaData metaData= result.getMetaData();
        int n= metaData.getColumnCount();
        for (int i= 0; i < n; i++) {
            ps.print(metaData.getColumnLabel(i+1));
            if (i < n-1)
                ps.print(", "); //$NON-NLS-1$
        }
        ps.println("):"); //$NON-NLS-1$
        for (int r= 0; result.next() && r < maxRow; r++) {
            for (int i= 0; i < n; i++)
                ps.print(' ' + result.getString(i+1));
            ps.println();
        }
        select.close();
    }

    private void view(PrintStream ps, String config, String buildPattern, String scenarioPattern) throws SQLException {
        Scenario[] scenarios= DB.queryScenarios(config, buildPattern, scenarioPattern); 
        ps.println(scenarios.length + " Scenarios"); //$NON-NLS-1$
        ps.println();
        for (int s= 0; s < scenarios.length; s++)
            scenarios[s].dump(ps);
    }
}
