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
        
        //db.dumpSizes();
        db.selectDistinctBuildNames(ps);
        //db.removeScenarios("%C:\\buildtest%");
        //db.selectScenarios(ps, "%C:\\buildtest%");
        //db.removeBuild("N200409162014");
        
        System.out.println("time: " + ((System.currentTimeMillis()-start)/1000.0));
        
        if (ps != System.out)
            ps.close();
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

    private void dumpSizes() throws SQLException {
        if (fConnection == null)
            return;    
        dumpSize("CONFIG"); //$NON-NLS-1$
        dumpSize("CONFIG_PROPERTIES"); //$NON-NLS-1$
        dumpSize("SCENARIO"); //$NON-NLS-1$
        dumpSize("SAMPLE"); //$NON-NLS-1$
        dumpSize("DATAPOINT"); //$NON-NLS-1$
        dumpSize("SCALAR"); //$NON-NLS-1$
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
        ResultSet rs= stmt.executeQuery("SELECT Count(*) FROM " + table); //$NON-NLS-1$
        if (rs.next())
            fPS.println(table + ": " + rs.getInt(1)); //$NON-NLS-1$
        rs.close();   
        stmt.close();
    }
}
