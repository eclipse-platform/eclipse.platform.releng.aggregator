package org.eclipse.test.internal.performance.db;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.eclipse.test.internal.performance.PerformanceTestPlugin;
import org.eclipse.test.internal.performance.data.Dim;


public class DBHelpers {
    
    Connection fConnection;
    SQL fSQL;
    PrintStream fPS;

    public static void main(String[] args) throws SQLException {
        DBHelpers db= new DBHelpers();
        
		String outFile= null;
		//outFile= "/tmp/dbdump";	//$NON-NLS-1$
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
		
		/*
		Properties p= new Properties();
		PerformanceTestPlugin.parseVariations(p, "|abc=def||xyz=123|");
		*/
		
		
		/*
		Properties p= new Properties();
		PerformanceTestPlugin.parsePairs(p, ";abc=123;");
		System.out.println(p);
		*/
		
		/*
		Scenario[] sc= DB.queryScenarios("relengbuildwin2", new String[] { "3%" }, "%", null);
		System.out.println("Scenarios: " + sc.length);
        */
		
		//db.convertVariations();
		//db.listBuildnames();
		db.countVariations();
		db.addVariations2();
		db.countVariations();
		//db.selectVariations(ps);
        //db.dumpSizes();
		//db.dumpAll(ps);
        //db.selectDistinctBuildNames(ps);
        //db.countScalars("N200409300010");
        //db.removeScenarios("%C:\\buildtest%");
        //db.selectScenarios(ps, "%C:\\buildtest%");
        //db.removeBuild("N200409162014");
		//db.removeDimension(InternalDimensions.BYTES_READ);
		//db.countDimension(InternalDimensions.USER_TIME);
		//db.getRefData("3.1M1_200409212000", "org.eclipse.jdt.text.tests.performance.RevertJavaEditorTest#testRevertJavaEditor()");
//		System.out.println("time: " + ((System.currentTimeMillis()-start)/1000.0));
//		
//		start= System.currentTimeMillis();
		//DataPoint[] points= DB.queryDataPoints("relengbuildwin2", "3.1M1_200409212000", "org.eclipse.jdt.text.tests.performance.RevertJavaEditorTest#testRevertJavaEditor()", new Dim[] {InternalDimensions.CPU_TIME}); //$NON-NLS-1$ //$NON-NLS-2$
		//System.out.println("dps: " + points.length);
		
        System.out.println("time: " + ((System.currentTimeMillis()-start)/1000.0));
        
        if (ps != System.out)
            ps.close();
    }
    
    private void listBuildnames() {
		List l= new ArrayList();
		DB.queryBuildNames(l, "relengbuildwin2", "3%", "%");
		Iterator iter= l.iterator();
		while (iter.hasNext()) {
		    System.out.println(iter.next());
		}
    }

    private void removeDimension(Dim dim) throws SQLException {
        PreparedStatement q= fConnection.prepareStatement("delete from SCALAR where SCALAR.DIM_ID = ?"); //$NON-NLS-1$
        q.setInt(1, dim.getId());
        q.executeUpdate();
        q.close();
    }
    
    void countDimension(Dim dim) throws SQLException {
        PreparedStatement q= fConnection.prepareStatement("select count(*) from SCALAR where SCALAR.DIM_ID = ?"); //$NON-NLS-1$
        q.setInt(1, dim.getId());

        ResultSet set= q.executeQuery();
        if (set.next())
            System.out.println(dim.getName() + ": " + set.getInt(1));      //$NON-NLS-1$
    }
    
    private void getRefData(String build, String scenario) throws SQLException {       
        /*
        PreparedStatement q= fConnection.prepareStatement("select DATAPOINT.ID from DATAPOINT, CONFIG, SAMPLE, SCENARIO where DATAPOINT.SAMPLE_ID = SAMPLE.ID and CONFIG.ID = SAMPLE.CONFIG_ID and SAMPLE.SCENARIO_ID = SCENARIO.ID and CONFIG.BUILD = ? and SCENARIO.NAME = ?");
        q.setString(1, build);
        q.setString(2, scenario);
        */
        
        Properties p= new Properties();
        p.put(PerformanceTestPlugin.CONFIG, "relengbuildwin2");
        p.put(PerformanceTestPlugin.BUILD, build);
        String key= PerformanceTestPlugin.toVariations(p);
 
        PreparedStatement q= fConnection.prepareStatement("select DATAPOINT.ID from DATAPOINT, VARIATION, SAMPLE, SCENARIO where DATAPOINT.SAMPLE_ID = SAMPLE.ID and VARIATION.ID = SAMPLE.VARIATION_ID and SAMPLE.SCENARIO_ID = SCENARIO.ID and VARIATION.KEYVALPAIRS = ? and SCENARIO.NAME = ?");
        q.setString(1, key);
        q.setString(2, scenario);
 
        PreparedStatement q2= fConnection.prepareStatement("select SCALAR.DIM_ID, SCALAR.VALUE from SCALAR where SCALAR.DATAPOINT_ID = ?");

        ResultSet set= q.executeQuery();
        while (set.next()) {
            int datapoint_id= set.getInt(1);
            q2.setInt(1, datapoint_id);
            ResultSet set2= q2.executeQuery();
            while (set2.next()) {
                //System.out.print(set2.getInt(1));
                //System.out.println(" " + set2.getBigDecimal(2));
            }
            set2.close();
        }
        set.close();
        q.close();
        q2.close();
    }

    private void countScalars(String build) throws SQLException {
        //PreparedStatement q= fConnection.prepareStatement("select count(*) from SCALAR, DATAPOINT, CONFIG, SAMPLE where SCALAR.DATAPOINT_ID = DATAPOINT.ID and DATAPOINT.SAMPLE_ID = SAMPLE.ID and CONFIG.ID = SAMPLE.CONFIG_ID and CONFIG.BUILD = ? ");
        PreparedStatement q= fConnection.prepareStatement("select count(*) from DATAPOINT, CONFIG, SAMPLE where DATAPOINT.SAMPLE_ID = SAMPLE.ID and CONFIG.ID = SAMPLE.CONFIG_ID and CONFIG.BUILD = ? ");
        q.setString(1, build);

        ResultSet set= q.executeQuery();
        while (set.next())
            System.out.println(" " + set.getInt(1));
    }

    DBHelpers() {
        DB db= DB.getDefault();
        fConnection= DB.getConnection();
        fSQL= db.getSQL();
        fPS= System.out;
    }
    
    private void removeBuild(String name) throws SQLException {
        
        int config_cnt= 0, sample_cnt= 0, dp_cnt= 0;
        int max_dps= 0;
        boolean delete= true;
        
        fPS.println("removing:");

        PreparedStatement iterConfigs= fConnection.prepareStatement(
         		"select CONFIG.ID from CONFIG where CONFIG.BUILD = ?"
        );
        PreparedStatement iterSamples= fConnection.prepareStatement(
         		"select SAMPLE.ID from SAMPLE where SAMPLE.CONFIG_ID = ?"
        );
        PreparedStatement iterDatapoints= fConnection.prepareStatement(
         		"select DATAPOINT.ID from DATAPOINT where DATAPOINT.SAMPLE_ID = ?"
        );
        
        PreparedStatement deleteScalars= fConnection.prepareStatement("delete from SCALAR where DATAPOINT_ID = ?");
        PreparedStatement deleteDatapoints= fConnection.prepareStatement("delete from DATAPOINT where SAMPLE_ID = ?");
        PreparedStatement deleteSamples= fConnection.prepareStatement("delete from SAMPLE where CONFIG_ID = ?");
        PreparedStatement deleteConfigProperties= fConnection.prepareStatement("delete from CONFIG_PROPERTIES where CONFIG_ID = ?");
        PreparedStatement deleteConfigs= fConnection.prepareStatement("delete from CONFIG where CONFIG.BUILD = ?");
        
        ResultSet samples= null, datapoints= null, configs= null;
        iterConfigs.setString(1, name);
        configs= iterConfigs.executeQuery();
        while (configs.next()) {
            int config_id= configs.getInt(1);
            fPS.println("config: " + config_id);
	        iterSamples.setInt(1, config_id);
	        samples= iterSamples.executeQuery();
	        while (samples.next()) {
	            int sample_id= samples.getInt(1);
	            fPS.println(" sample: " + sample_id);
	            iterDatapoints.setInt(1, sample_id);
		        datapoints= iterDatapoints.executeQuery();
		        int dps= 0;
		        while (datapoints.next()) {
		            int dp_id= datapoints.getInt(1);
		            //fPS.println("  dp: " + dp_id);
		            if (delete) {
		                deleteScalars.setInt(1, dp_id);
		                deleteScalars.executeUpdate();
			            dp_cnt++;
			            dps++;
		            }
		        }
		        max_dps= Math.max(max_dps, dps);
		        if (delete) {
		            deleteDatapoints.setInt(1, sample_id);
		            deleteDatapoints.executeUpdate();
		            sample_cnt++;
		        }
	        }
	        if (delete) {
	            deleteSamples.setInt(1, config_id);
	            deleteSamples.executeUpdate();
	            deleteConfigProperties.setInt(1, config_id);
	            deleteConfigProperties.executeUpdate();
	            config_cnt++;
	        }
        }
        if (delete) {
            deleteConfigs.setString(1, name);
            deleteConfigs.executeUpdate();
        }
        fPS.println("  configs: " + config_cnt);
        fPS.println("  samples: " + sample_cnt);
        fPS.println("  datapoints: " + dp_cnt);
        fPS.println("  max datapoints: " + max_dps);
        
        if (configs != null) configs.close();
        if (samples != null) samples.close();
        if (datapoints != null) datapoints.close();
        
        if (iterConfigs != null) iterConfigs.close();
        if (iterSamples != null) iterSamples.close();
        if (iterDatapoints != null) iterDatapoints.close();
        
        if (deleteSamples != null) deleteSamples.close();
        if (deleteScalars != null) deleteScalars.close();
        if (deleteDatapoints != null) deleteDatapoints.close();
        if (deleteConfigProperties != null) deleteConfigProperties.close();
    }

    private void removeScenarios(String pattern) throws SQLException {
        
        int config_cnt= 0, sample_cnt= 0, dp_cnt= 0;
        int max_dps= 0;
        boolean delete= true;
        
        fPS.println("removing:");

        PreparedStatement iterScenarios= fConnection.prepareStatement("select ID from SCENARIO where NAME like ?");
        PreparedStatement iterSamples= fConnection.prepareStatement("select SAMPLE.ID from SAMPLE where SAMPLE.SCENARIO_ID = ?");
        PreparedStatement iterDatapoints= fConnection.prepareStatement("select DATAPOINT.ID from DATAPOINT where DATAPOINT.SAMPLE_ID = ?");
        
        PreparedStatement deleteScalars= fConnection.prepareStatement("delete from SCALAR where DATAPOINT_ID = ?");
        PreparedStatement deleteDatapoints= fConnection.prepareStatement("delete from DATAPOINT where SAMPLE_ID = ?");
        PreparedStatement deleteSamples= fConnection.prepareStatement("delete from SAMPLE where SCENARIO_ID = ?");
        PreparedStatement deleteScenarios= fConnection.prepareStatement("delete from SCENARIO where NAME like ?");
        
        ResultSet samples= null, datapoints= null, configs= null;
        iterScenarios.setString(1, pattern);
        configs= iterScenarios.executeQuery();
        for (int n= 0; configs.next(); n++) {
            int scenario_id= configs.getInt(1);
            fPS.println(n + " scenario: " + scenario_id);
	        iterSamples.setInt(1, scenario_id);
	        samples= iterSamples.executeQuery();
	        while (samples.next()) {
	            int sample_id= samples.getInt(1);
	            fPS.println(" sample: " + sample_id);
	            iterDatapoints.setInt(1, sample_id);
		        datapoints= iterDatapoints.executeQuery();
		        int dps= 0;
		        while (datapoints.next()) {
		            int dp_id= datapoints.getInt(1);
		            //fPS.println("  dp: " + dp_id);
		            if (delete) {
		                deleteScalars.setInt(1, dp_id);
		                deleteScalars.executeUpdate();
			            dp_cnt++;
			            dps++;
		            }
		        }
		        max_dps= Math.max(max_dps, dps);
		        if (delete) {
		            deleteDatapoints.setInt(1, sample_id);
		            deleteDatapoints.executeUpdate();
		            sample_cnt++;
		        }
	        }
	        if (delete) {
	            deleteSamples.setInt(1, scenario_id);
	            deleteSamples.executeUpdate();
	            fConnection.commit();
	            config_cnt++;
	        }
        }
        if (delete) {
            deleteScenarios.setString(1, pattern);
            deleteScenarios.executeUpdate();
        }
        fPS.println("  configs: " + config_cnt);
        fPS.println("  samples: " + sample_cnt);
        fPS.println("  datapoints: " + dp_cnt);
        fPS.println("  max datapoints: " + max_dps);
        
        if (configs != null) configs.close();
        if (samples != null) samples.close();
        if (datapoints != null) datapoints.close();
        
        if (iterScenarios != null) iterScenarios.close();
        if (iterSamples != null) iterSamples.close();
        if (iterDatapoints != null) iterDatapoints.close();
        
        if (deleteSamples != null) deleteSamples.close();
        if (deleteScalars != null) deleteScalars.close();
        if (deleteDatapoints != null) deleteDatapoints.close();
        if (deleteScenarios != null) deleteScenarios.close();
    }

    private void removeSamples(int config_id) throws SQLException {
        
        int sample_cnt= 0, dp_cnt= 0;
        boolean delete= true;
        
        fPS.println("removing:");

        PreparedStatement iterSamples= fConnection.prepareStatement("select SAMPLE.ID from SAMPLE where SAMPLE.CONFIG_ID = ?");
        PreparedStatement iterDatapoints= fConnection.prepareStatement("select DATAPOINT.ID from DATAPOINT where DATAPOINT.SAMPLE_ID = ?");
        
        PreparedStatement deleteScalars= fConnection.prepareStatement("delete from SCALAR where DATAPOINT_ID = ?");
        PreparedStatement deleteDatapoints= fConnection.prepareStatement("delete from DATAPOINT where SAMPLE_ID = ?");
        PreparedStatement deleteSamples= fConnection.prepareStatement("delete from SAMPLE where SAMPLE.ID = ?");
        
        ResultSet samples= null, datapoints= null, configs= null;
        iterSamples.setInt(1, config_id);
        samples= iterSamples.executeQuery();
        while (samples.next()) {
            int sample_id= samples.getInt(1);
            fPS.println(" sample: " + sample_id);
            iterDatapoints.setInt(1, sample_id);
	        datapoints= iterDatapoints.executeQuery();
	        int dps= 0;
	        while (datapoints.next()) {
	            int dp_id= datapoints.getInt(1);
	            //fPS.println("  dp: " + dp_id);
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
	        }
        }
//        if (delete) {
//            deleteSamples.setInt(1, config_id);
//            
//            try {
//                deleteSamples.executeUpdate();
//                fConnection.commit();
//            } catch (SQLException e) {
//                System.err.println(e);
//            }
//        }
        fPS.println("  samples: " + sample_cnt);
        fPS.println("  datapoints: " + dp_cnt);
        
        if (configs != null) configs.close();
        if (samples != null) samples.close();
        if (datapoints != null) datapoints.close();
        
        if (iterSamples != null) iterSamples.close();
        if (iterDatapoints != null) iterDatapoints.close();
        
        if (deleteSamples != null) deleteSamples.close();
        if (deleteScalars != null) deleteScalars.close();
        if (deleteDatapoints != null) deleteDatapoints.close();
    }

    private void dumpSizes() throws SQLException {
        if (fConnection == null)
            return;    
        dumpSize("CONFIG"); //$NON-NLS-1$
        dumpSize("CONFIG_PROPERTIES"); //$NON-NLS-1$
        dumpSize("SCENARIO"); //$NON-NLS-1$
        dumpSize("VARIATION"); //$NON-NLS-1$
        dumpSize("SAMPLE"); //$NON-NLS-1$
        dumpSize("DATAPOINT"); //$NON-NLS-1$
        dumpSize("SCALAR"); //$NON-NLS-1$
    }
    
    private void dumpAll(PrintStream ps) throws SQLException {
        if (fConnection == null)
            return;

		Statement stmt= fConnection.createStatement();
        try {
            
            // do we have VARIATIONS
	        ResultSet rs= stmt.executeQuery("SELECT count(*) FROM sys.systables WHERE sys.systables.tablename = 'VARIATION' "); //$NON-NLS-1$
	        boolean variations= rs.next() && rs.getInt(1) > 0;
           
            ps.println("CONFIG(ID, NAME, BUILD):"); //$NON-NLS-1$
	        rs= stmt.executeQuery("SELECT ID, NAME, BUILD FROM CONFIG"); //$NON-NLS-1$
 	        while (rs.next()) {
	            ps.print(" " + rs.getInt(1)); //$NON-NLS-1$
	            ps.print(" " + rs.getString(2)); //$NON-NLS-1$
	            ps.print(" " + rs.getString(3)); //$NON-NLS-1$
	            ps.println();
	        }
            ps.println();
            
            ps.println("CONFIG_PROPERTIES(CONFIG_ID, NAME, VALUE):"); //$NON-NLS-1$
	        rs= stmt.executeQuery("SELECT CONFIG_ID, NAME, VALUE FROM CONFIG_PROPERTIES"); //$NON-NLS-1$
 	        while (rs.next()) {
	            ps.print(" " + rs.getInt(1)); //$NON-NLS-1$
	            ps.print(" " + rs.getString(2)); //$NON-NLS-1$
	            ps.print(" " + rs.getString(3)); //$NON-NLS-1$
	            ps.println();
	        }
            ps.println();
            
            ps.println("VARIATION(ID, KEYVALPAIRS):"); //$NON-NLS-1$
	        rs= stmt.executeQuery("SELECT ID, KEYVALPAIRS FROM VARIATION"); //$NON-NLS-1$
 	        while (rs.next()) {
	            ps.print(" " + rs.getInt(1)); //$NON-NLS-1$
	            ps.print(" " + rs.getString(2)); //$NON-NLS-1$
	            ps.println();
	        }
            ps.println();
            
            ps.println("SCENARIO(ID, NAME):"); //$NON-NLS-1$
	        rs= stmt.executeQuery("SELECT ID, NAME FROM SCENARIO"); //$NON-NLS-1$
 	        while (rs.next()) {
	            ps.print(" " + rs.getInt(1)); //$NON-NLS-1$
	            ps.print(" " + rs.getString(2)); //$NON-NLS-1$
	            ps.println();
	        }
            ps.println();
            
            if (variations) {
                ps.println("SAMPLE(ID, CONFIG_ID, VARIATION_ID, SCENARIO_ID, STARTTIME):"); //$NON-NLS-1$
    	        rs= stmt.executeQuery("SELECT ID, CONFIG_ID, VARIATION_ID, SCENARIO_ID, STARTTIME FROM SAMPLE"); //$NON-NLS-1$
     	        while (rs.next()) {
    	            ps.print(" " + rs.getInt(1)); //$NON-NLS-1$
    	            ps.print(" " + rs.getInt(2)); //$NON-NLS-1$
    	            ps.print(" " + rs.getInt(3)); //$NON-NLS-1$
    	            ps.print(" " + rs.getInt(4)); //$NON-NLS-1$
    	            ps.print(" " + rs.getTimestamp(5).toString()); //$NON-NLS-1$
    	            ps.println();
    	        }
            } else {
                ps.println("SAMPLE(ID, CONFIG_ID, SCENARIO_ID, STARTTIME):"); //$NON-NLS-1$
    	        rs= stmt.executeQuery("SELECT ID, CONFIG_ID, SCENARIO_ID, STARTTIME FROM SAMPLE"); //$NON-NLS-1$              
     	        while (rs.next()) {
    	            ps.print(" " + rs.getInt(1)); //$NON-NLS-1$
    	            ps.print(" " + rs.getInt(2)); //$NON-NLS-1$
    	            ps.print(" " + rs.getInt(3)); //$NON-NLS-1$
    	            ps.print(" " + rs.getTimestamp(4).toString()); //$NON-NLS-1$
    	            ps.println();
    	        }
            }
            ps.println();
            
            ps.println("SAMPLE_PROPERTIES(CONFIG_ID, KEY_ID, VALUE):"); //$NON-NLS-1$
	        rs= stmt.executeQuery("SELECT SAMPLE_ID, NAME, VALUE FROM SAMPLE_PROPERTIES"); //$NON-NLS-1$
 	        while (rs.next()) {
	            ps.print(" " + rs.getInt(1)); //$NON-NLS-1$
	            ps.print(" " + rs.getString(2)); //$NON-NLS-1$
	            ps.print(" " + rs.getString(3)); //$NON-NLS-1$
	            ps.println();
	        }
            ps.println();
            
            ps.println("DATAPOINT(ID, SAMPLE_ID, SEQ, STEP):"); //$NON-NLS-1$
	        rs= stmt.executeQuery("SELECT ID, SAMPLE_ID, SEQ, STEP FROM DATAPOINT"); //$NON-NLS-1$
 	        while (rs.next()) {
	            ps.print(" " + rs.getInt(1)); //$NON-NLS-1$
	            ps.print(" " + rs.getInt(2)); //$NON-NLS-1$
	            ps.print(" " + rs.getInt(3)); //$NON-NLS-1$
	            ps.print(" " + rs.getInt(4)); //$NON-NLS-1$
	            ps.println();
	        }
            ps.println();
            
            ps.println("SCALAR(DATAPOINT_ID, DIM_ID, VALUE):"); //$NON-NLS-1$
	        rs= stmt.executeQuery("SELECT DATAPOINT_ID, DIM_ID, VALUE FROM SCALAR"); //$NON-NLS-1$
 	        while (rs.next()) {
	            ps.print(" " + rs.getInt(1)); //$NON-NLS-1$
	            ps.print(" " + rs.getInt(2)); //$NON-NLS-1$
	            ps.print(" " + rs.getBigDecimal(3).toString()); //$NON-NLS-1$
	            ps.println();
	        }
            ps.println();
        } finally {
            stmt.close();
        }
    }

    private void selectVariations(PrintStream ps) throws SQLException {
        Statement stmt= fConnection.createStatement();
        ResultSet rs= stmt.executeQuery("select KEYVALPAIRS from VARIATION order by KEYVALPAIRS"); //$NON-NLS-1$
        while (rs.next())
            ps.println(rs.getString(1));
        rs.close();
        stmt.close();
    }
    
    private void selectDistinctBuildNames(PrintStream ps) throws SQLException {
        Statement stmt= fConnection.createStatement();
        ResultSet rs= stmt.executeQuery("select distinct BUILD from CONFIG order by BUILD"); //$NON-NLS-1$
        while (rs.next())
            ps.println(rs.getString(1));
        rs.close();
        stmt.close();
    }
    
    private void selectScenarios(PrintStream ps, String pattern) throws SQLException {
        PreparedStatement stmt= fConnection.prepareStatement("select NAME from SCENARIO where NAME like ? order by NAME"); //$NON-NLS-1$
        stmt.setString(1, pattern);
        ResultSet rs= stmt.executeQuery();
        while (rs.next())
            ps.println(rs.getString(1));
        rs.close();
        stmt.close();
    }
    
    private void dumpSize(String table) throws SQLException {
        Statement stmt= fConnection.createStatement();
        ResultSet rs= stmt.executeQuery("select Count(*) from " + table); //$NON-NLS-1$
        if (rs.next())
            fPS.println(table + ": " + rs.getInt(1)); //$NON-NLS-1$
        rs.close();   
        stmt.close();
    }

    private void countVariations() throws SQLException {
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
   
    private void addVariations() throws SQLException { 
        //fSQL.initializeVariations();
        
        // convert variations from config
        Map map= new HashMap();
        
        PreparedStatement update= fConnection.prepareStatement("update SAMPLE set VARIATION_ID = ? where SAMPLE.VARIATION_ID is null and SAMPLE.CONFIG_ID = ? ");

        Statement stmt= fConnection.createStatement();
        ResultSet rs= stmt.executeQuery("select distinct CONFIG.NAME, CONFIG.BUILD, CONFIG.ID from CONFIG, SAMPLE where CONFIG.ID = SAMPLE.CONFIG_ID and SAMPLE.VARIATION_ID is null "); //$NON-NLS-1$
        while (rs.next()) {
            String config= rs.getString(1);
            String build= rs.getString(2);
            int config_id= rs.getInt(3);
            System.out.println("config_id: " + config_id);
            String lk= config + "/" + build;
            Integer i= (Integer) map.get(lk);
            if (i == null) {
                Properties properties= new Properties();
                properties.put(PerformanceTestPlugin.CONFIG, config); //$NON-NLS-1$
                properties.put(PerformanceTestPlugin.BUILD, build); //$NON-NLS-1$
                String variation= PerformanceTestPlugin.toVariations(properties);
                int id= fSQL.createVariations(variation);
                i= new Integer(id);
                map.put(lk, i);
                System.out.println("   " + i + ": " + variation);
            }
            update.setInt(1, i.intValue());
            update.setInt(2, config_id);
            try {
                update.executeUpdate();
            } catch (SQLException e) {
                System.err.println(e);
            }
        }
        rs.close();
        update.close();
        stmt.close();
    }
    
    private void addVariations2() throws SQLException { 
        //fSQL.initializeVariations();
        
        // convert variations from config
        Map map= new HashMap();
        
        PreparedStatement update= fConnection.prepareStatement("update SAMPLE set VARIATION_ID = ? where SAMPLE.ID = ? ");

        Statement stmt= fConnection.createStatement();
        ResultSet rs= stmt.executeQuery("select CONFIG.NAME, CONFIG.BUILD, SAMPLE.ID from CONFIG, SAMPLE where CONFIG.ID = SAMPLE.CONFIG_ID and SAMPLE.VARIATION_ID is null "); //$NON-NLS-1$
        while (rs.next()) {
            String config= rs.getString(1);
            String build= rs.getString(2);
            int sample_id= rs.getInt(3);
            System.out.println("sample_id: " + sample_id);
            String lk= config + "/" + build;
            Integer i= (Integer) map.get(lk);
            if (i == null) {
                Properties properties= new Properties();
                properties.put(PerformanceTestPlugin.CONFIG, config); //$NON-NLS-1$
                properties.put(PerformanceTestPlugin.BUILD, build); //$NON-NLS-1$
                String variation= PerformanceTestPlugin.toVariations(properties);
                int id= fSQL.createVariations(variation);
                i= new Integer(id);
                map.put(lk, i);
                System.out.println("   " + i + ": " + variation);
            }
            update.setInt(1, i.intValue());
            update.setInt(2, sample_id);
            try {
                update.executeUpdate();
            } catch (SQLException e) {
                System.err.println(e);
            }
        }
        rs.close();
        update.close();
        stmt.close();
    }

    private void convertVariations() throws SQLException {
        PreparedStatement update= fConnection.prepareStatement("update VARIATION set KEYVALPAIRS = ? where ID = ? ");
        
        Statement stmt= fConnection.createStatement();
        ResultSet rs= stmt.executeQuery("select ID, KEYVALPAIRS from VARIATION for update of KEYVALPAIRS"); //$NON-NLS-1$
        while (rs.next()) {
            int id= rs.getInt(1);
            String variation= rs.getString(2);
            Properties p= new Properties();
            try {
                PerformanceTestPlugin.parseVariations(p, variation);
                String s= PerformanceTestPlugin.toVariations(p);
                System.out.println(s);
                
                update.setString(1, s);
                update.setInt(2, id);
                update.executeUpdate();
            } catch (IllegalArgumentException e) {
                System.out.println(e);
            }
        }
        rs.close();
        stmt.close();   
    }
}
