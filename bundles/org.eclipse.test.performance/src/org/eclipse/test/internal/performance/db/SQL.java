/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.test.internal.performance.db;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import org.eclipse.test.internal.performance.PerformanceTestPlugin;

public class SQL {
    
    private Connection fConnection;
    private PreparedStatement fInsertConfig, fInsertVariation, fInsertScenario, fInsertSample, fInsertDataPoint, fInsertScalar;
    private PreparedStatement fQueryDatapoints2, fQueryScalars, fQueryVariation, fQueryScenario, fQueryAllScenarios, fQueryVariations;
    //private PreparedStatement fQueryDatapoints;
    //private PreparedStatement[] fQueries= new PreparedStatement[20];
    

    SQL(Connection con) {
        fConnection= con;
    }
    
    public void dispose() throws SQLException {
    	// TODO: close all prepared statements
//    	if (fQueries != null) {
//    		for (int i= 0; i < fQueries.length; i++) {
//    			if (fQueries[i] != null)
//    				fQueries[i].close();
//    		}
//    		fQueries= null;
//    	}
    }
    
//    PreparedStatement getQueryStatement(int n) throws SQLException {
//    	if (n < 0 || n > 20)
//    		return null;
//    	if (fQueries[n] == null) {
//    		StringBuffer sb= new StringBuffer(
//    				"select SAMPLE.ID, DATAPOINT.ID, DATAPOINT.STEP, SCALAR.DIM_ID, SCALAR.VALUE from SCALAR, DATAPOINT, SAMPLE, SCENARIO, CONFIG " + //$NON-NLS-1$
//					"where " + //$NON-NLS-1$
//					"SAMPLE.CONFIG_ID = CONFIG.ID and CONFIG.NAME = ? and CONFIG.BUILD LIKE ? and " + //$NON-NLS-1$
//					"SAMPLE.SCENARIO_ID = SCENARIO.ID and SCENARIO.NAME = ? and " + //$NON-NLS-1$
//					"SCALAR.DATAPOINT_ID = DATAPOINT.ID and " + //$NON-NLS-1$
//					"DATAPOINT.SAMPLE_ID = SAMPLE.ID " //$NON-NLS-1$
//			);
//    		
//    		if (n > 0) {
//				sb.append("and (SCALAR.DIM_ID = ?"); //$NON-NLS-1$
//    			for (int i= 1; i < n; i++)
//    				sb.append(" or SCALAR.DIM_ID = ?"); //$NON-NLS-1$
//				sb.append(") "); //$NON-NLS-1$
//    		}
//    		
//			sb.append(
//					"order by SAMPLE.STARTTIME, DATAPOINT.ID, DATAPOINT.STEP" //$NON-NLS-1$
//    		);    		
//    		
//			fQueries[n]= fConnection.prepareStatement(sb.toString());
//    	}
//    	return fQueries[n];
//    }
    
    void createPreparedStatements() throws SQLException {
        fInsertConfig= fConnection.prepareStatement(
                "insert into CONFIG (NAME, BUILD) values (?, ?)", Statement.RETURN_GENERATED_KEYS); //$NON-NLS-1$
        fInsertVariation= fConnection.prepareStatement(
                "insert into VARIATION (KEYVALPAIRS) values (?)", Statement.RETURN_GENERATED_KEYS); //$NON-NLS-1$
        fInsertScenario= fConnection.prepareStatement(
                "insert into SCENARIO (NAME) values (?)", Statement.RETURN_GENERATED_KEYS); //$NON-NLS-1$
        fInsertSample= fConnection.prepareStatement(
                "insert into SAMPLE (CONFIG_ID, VARIATION_ID, SCENARIO_ID, STARTTIME) values (?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS); //$NON-NLS-1$
        fInsertDataPoint= fConnection.prepareStatement(
                "insert into DATAPOINT (SAMPLE_ID, SEQ, STEP) values (?, ?, ?)", Statement.RETURN_GENERATED_KEYS); //$NON-NLS-1$
        fInsertScalar= fConnection.prepareStatement(
                "insert into SCALAR values (?, ?, ?)"); //$NON-NLS-1$

        fQueryScenario= fConnection.prepareStatement(
                "select ID from SCENARIO where NAME = ?"); //$NON-NLS-1$
        fQueryVariation= fConnection.prepareStatement(
        		"select ID from VARIATION where KEYVALPAIRS = ?"); //$NON-NLS-1$
        fQueryAllScenarios= fConnection.prepareStatement(
        		"select distinct SCENARIO.NAME from SCENARIO, SAMPLE, VARIATION where " +	//$NON-NLS-1$
        		"SAMPLE.VARIATION_ID = VARIATION.ID and VARIATION.KEYVALPAIRS LIKE ? and " +	//$NON-NLS-1$
        		"SAMPLE.SCENARIO_ID = SCENARIO.ID and SCENARIO.NAME LIKE ?"	//$NON-NLS-1$
        ); 
        fQueryVariations= fConnection.prepareStatement(
        		"select distinct VARIATION.KEYVALPAIRS from VARIATION, SAMPLE, SCENARIO where " +	//$NON-NLS-1$
        		"SAMPLE.VARIATION_ID = VARIATION.ID and VARIATION.KEYVALPAIRS LIKE ? and " +	//$NON-NLS-1$
        		"SAMPLE.SCENARIO_ID = SCENARIO.ID and SCENARIO.NAME LIKE ?"	//$NON-NLS-1$
        ); 
        fQueryDatapoints2= fConnection.prepareStatement(
        		"select DATAPOINT.ID, DATAPOINT.STEP from VARIATION, SCENARIO, SAMPLE, DATAPOINT " + //$NON-NLS-1$
        		"where " + //$NON-NLS-1$
        		"SAMPLE.VARIATION_ID = VARIATION.ID and VARIATION.KEYVALPAIRS = ? and " + //$NON-NLS-1$
        		"SAMPLE.SCENARIO_ID = SCENARIO.ID and SCENARIO.NAME = ? and " + //$NON-NLS-1$
         		"DATAPOINT.SAMPLE_ID = SAMPLE.ID " //$NON-NLS-1$
        );
        
        fQueryScalars= fConnection.prepareStatement(
        		"select SCALAR.DIM_ID, SCALAR.VALUE from SCALAR " + //$NON-NLS-1$
        		"where " + //$NON-NLS-1$
				"SCALAR.DATAPOINT_ID = ?" //$NON-NLS-1$
        );
    }
    
    void initializeVariations() {
        try {
            Statement stmt= fConnection.createStatement();
	        stmt.executeUpdate(
	                "create table VARIATION (" + //$NON-NLS-1$
	                    "ID int not null GENERATED ALWAYS AS IDENTITY," + //$NON-NLS-1$
	                    "KEYVALPAIRS varchar(10000) not null " + //$NON-NLS-1$
	                ")" //$NON-NLS-1$
	            );
	        stmt.executeUpdate("alter table VARIATION add constraint VA_KVP primary key (KEYVALPAIRS)"); //$NON-NLS-1$
	        stmt.executeUpdate("alter table SAMPLE add column VARIATION_ID int"); //$NON-NLS-1$

//	        stmt.executeUpdate("alter table SAMPLE add constraint SAMPLE_CONSTRAINT " + //$NON-NLS-1$
//    							"foreign key (VARIATION_ID) references VARIATION (ID)"); //$NON-NLS-1$

	        stmt.close();
	        fConnection.commit();
	        
        } catch (SQLException e) {
            try {
                fConnection.rollback();
            } catch (SQLException e1) {
                PerformanceTestPlugin.log(e1);
            }
        }
    }
    
    void initialize() {
        try {
            Statement stmt= fConnection.createStatement();
	        stmt.executeUpdate(
	                "create table CONFIG (" + //$NON-NLS-1$
	                    "ID int not null GENERATED ALWAYS AS IDENTITY," + //$NON-NLS-1$
	                    "NAME varchar(32)," + //$NON-NLS-1$
	                    "BUILD varchar(64)" + //$NON-NLS-1$
	                ")" //$NON-NLS-1$
	            );
	        stmt.executeUpdate(
	        		"create table SAMPLE (" + //$NON-NLS-1$
	        			"ID int not null GENERATED ALWAYS AS IDENTITY," + //$NON-NLS-1$
						"CONFIG_ID int not null," + //$NON-NLS-1$
						"SCENARIO_ID int not null," + //$NON-NLS-1$
						"STARTTIME timestamp" + //$NON-NLS-1$
					")" //$NON-NLS-1$
	        );           
	        stmt.executeUpdate(
	        		"create table SCENARIO (" + //$NON-NLS-1$
	                	"ID int not null GENERATED ALWAYS AS IDENTITY," + //$NON-NLS-1$
						"NAME varchar(256) not null" + //$NON-NLS-1$
					")" //$NON-NLS-1$
	        );
	        stmt.executeUpdate(
	        		"create table DATAPOINT (" + //$NON-NLS-1$
	                	"ID int not null GENERATED ALWAYS AS IDENTITY," + //$NON-NLS-1$
						"SAMPLE_ID int not null," + //$NON-NLS-1$
						"SEQ int," + //$NON-NLS-1$
						"STEP int" + //$NON-NLS-1$
					")" //$NON-NLS-1$
	        );
	        stmt.executeUpdate(
	        		"create table SCALAR (" + //$NON-NLS-1$
	                	"DATAPOINT_ID int not null," + //$NON-NLS-1$
						"DIM_ID int not null," + //$NON-NLS-1$
						"VALUE bigint" + //$NON-NLS-1$
					")" //$NON-NLS-1$
	        );
	        
	        // Primary/unique
	        stmt.executeUpdate("alter table DATAPOINT add constraint DP_ID primary key (ID)"); //$NON-NLS-1$
	        stmt.executeUpdate("alter table SAMPLE add constraint SA_ID primary key (ID)"); //$NON-NLS-1$
	        stmt.executeUpdate("alter table SCENARIO add constraint SC_NAME primary key (NAME)"); //$NON-NLS-1$
	        
	        // Foreign
	        stmt.executeUpdate("alter table DATAPOINT add constraint DP_CONSTRAINT " + //$NON-NLS-1$
	        		"foreign key (SAMPLE_ID) references SAMPLE (ID)"); //$NON-NLS-1$
	        stmt.executeUpdate("alter table SCALAR add constraint SCALAR_CONSTRAINT " + //$NON-NLS-1$
	        		"foreign key (DATAPOINT_ID) references DATAPOINT (ID)"); //$NON-NLS-1$

	        stmt.close();
	        fConnection.commit();
	        
        } catch (SQLException e) {
            try {
                fConnection.rollback();
            } catch (SQLException e1) {
                PerformanceTestPlugin.log(e1);
            }
        }
    }

    static int create(PreparedStatement stmt) throws SQLException {
        stmt.executeUpdate();
        ResultSet rs= stmt.getGeneratedKeys();
        if (rs != null) {
	        try {
		        if (rs.next()) {
			        BigDecimal idColVar= rs.getBigDecimal(1);
			        return idColVar.intValue();
		        }
	        } finally {
	            rs.close();
	        }
        }
        return 0;
	}

    int getScenario(String scenarioPattern) throws SQLException {
        fQueryScenario.setString(1, scenarioPattern);
        ResultSet result= fQueryScenario.executeQuery();
        while (result.next())
            return result.getInt(1);

        fInsertScenario.setString(1, scenarioPattern);
        return create(fInsertScenario);
    }
    
    int createConfig(String configName, String buildName) throws SQLException {
        fInsertConfig.setString(1, configName);
        fInsertConfig.setString(2, buildName);
        return create(fInsertConfig);
    }
    
    int createSample(int config_id, int variation_id, int scenario_id, Timestamp starttime) throws SQLException {
        fInsertSample.setInt(1, config_id);
        fInsertSample.setInt(2, variation_id);
        fInsertSample.setInt(3, scenario_id);
        fInsertSample.setTimestamp(4, starttime);
        return create(fInsertSample);
    }
        
    int createDataPoint(int sample_id, int seq, int step) throws SQLException {
        fInsertDataPoint.setInt(1, sample_id);
        fInsertDataPoint.setInt(2, seq);
        fInsertDataPoint.setInt(3, step);
        return create(fInsertDataPoint);
    }
    
    void insertScalar(int datapoint_id, int dim_id, long value) throws SQLException {
		fInsertScalar.setInt(1, datapoint_id);
		fInsertScalar.setInt(2, dim_id);
		fInsertScalar.setLong(3, value);
		fInsertScalar.executeUpdate();
    }
    
//    ResultSet queryDataPoints2(String configName, String buildPattern, String scenario, int[] dim_ids) throws SQLException {
//    	PreparedStatement queryStatement= getQueryStatement(dim_ids.length);
//    	queryStatement.setString(1, configName);
//    	queryStatement.setString(2, buildPattern);
//    	queryStatement.setString(3, scenario);
//    	int j= 4;
//    	for (int i= 0; i < dim_ids.length; i++)
//    		queryStatement.setInt(j++, dim_ids[i]);
//        return queryStatement.executeQuery();
//    }
    
    ResultSet queryDataPoints(Variations variations, String scenarioName) throws SQLException {
     	fQueryDatapoints2.setString(1, variations.toExactMatchString());
    	fQueryDatapoints2.setString(2, scenarioName);
    	return fQueryDatapoints2.executeQuery();
    }

    ResultSet queryDataPoints(int datapointId) throws SQLException {
        fQueryScalars.setInt(1, datapointId);
    	return fQueryScalars.executeQuery();
    }

    /*
     * Returns SCENARIO.NAME
     */
    ResultSet queryScenarios(Variations variations, String scenarioPattern) throws SQLException {
        fQueryAllScenarios.setString(1, variations.toQueryPattern());
        fQueryAllScenarios.setString(2, scenarioPattern);
        return fQueryAllScenarios.executeQuery();
    }
    
    /*
     * Returns VARIATION.KEYVALPAIRS
     */
    ResultSet queryVariations(String keyPattern, String scenarioPattern) throws SQLException {
        fQueryVariations.setString(1, keyPattern);
        fQueryVariations.setString(2, scenarioPattern);
        return fQueryVariations.executeQuery();
    }

    public int createVariations(Variations variations) throws SQLException {
        String exactMatchString= variations.toExactMatchString();
        fQueryVariation.setString(1, exactMatchString);
        ResultSet result= fQueryVariation.executeQuery();
        while (result.next())
            return result.getInt(1);

        fInsertVariation.setString(1, exactMatchString);
        return create(fInsertVariation);
    }
}
