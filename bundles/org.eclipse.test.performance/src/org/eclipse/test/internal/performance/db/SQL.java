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
import java.sql.Types;

public class SQL {
    
    private Connection fConnection;
    private PreparedStatement fInsertConfig, fInsertScenario, fInsertSample, fInsertDataPoint, fInsertScalar;
    private PreparedStatement fInsertConfigProperty, fInsertSampleProperty;
    private PreparedStatement fQueryScenario, fQueryAllScenarios, fQueryBuilds;
    private PreparedStatement[] fQueries= new PreparedStatement[20];
    

    SQL(Connection con) {
        fConnection= con;
    }
    
    public void dispose() throws SQLException {
    	// TODO: close all prepared statements
    	if (fQueries != null) {
    		for (int i= 0; i < fQueries.length; i++) {
    			if (fQueries[i] != null)
    				fQueries[i].close();
    		}
    		fQueries= null;
    	}
    }
    
    PreparedStatement getQueryStatement(int n) throws SQLException {
    	if (n < 0 || n > 20)
    		return null;
    	if (fQueries[n] == null) {
    		StringBuffer sb= new StringBuffer(
    				"select SAMPLE.ID, DATAPOINT.ID, DATAPOINT.STEP, SCALAR.DIM_ID, SCALAR.VALUE from SCALAR, DATAPOINT, SAMPLE, SCENARIO, CONFIG " + //$NON-NLS-1$
					"where " + //$NON-NLS-1$
					"SAMPLE.CONFIG_ID = CONFIG.ID and CONFIG.NAME LIKE ? and CONFIG.BUILD LIKE ? and " + //$NON-NLS-1$
					"SAMPLE.SCENARIO_ID = SCENARIO.ID and SCENARIO.NAME = ? and " + //$NON-NLS-1$
					"SCALAR.DATAPOINT_ID = DATAPOINT.ID and " + //$NON-NLS-1$
					"DATAPOINT.SAMPLE_ID = SAMPLE.ID " //$NON-NLS-1$
			);
    		
    		if (n > 0) {
				sb.append("and (SCALAR.DIM_ID = ?"); //$NON-NLS-1$
    			for (int i= 1; i < n; i++)
    				sb.append(" or SCALAR.DIM_ID = ?"); //$NON-NLS-1$
				sb.append(") "); //$NON-NLS-1$
    		}
    		
			sb.append(
					"order by SAMPLE.STARTTIME, DATAPOINT.ID, DATAPOINT.STEP" //$NON-NLS-1$
    		);    		
    		
			fQueries[n]= fConnection.prepareStatement(sb.toString());
    	}
    	return fQueries[n];
    }
    
    void createPreparedStatements() throws SQLException {
        fInsertConfig= fConnection.prepareStatement(
                "insert into CONFIG (NAME, BUILD) values (?, ?)", Statement.RETURN_GENERATED_KEYS); //$NON-NLS-1$
        fInsertConfigProperty= fConnection.prepareStatement(
                "insert into CONFIG_PROPERTIES values (?, ?, ?)"); //$NON-NLS-1$
        fInsertScenario= fConnection.prepareStatement(
                "insert into SCENARIO (NAME) values (?)", Statement.RETURN_GENERATED_KEYS); //$NON-NLS-1$
        fInsertSample= fConnection.prepareStatement(
                "insert into SAMPLE (CONFIG_ID, SCENARIO_ID, VARIATION, STARTTIME) values (?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS); //$NON-NLS-1$
        fInsertDataPoint= fConnection.prepareStatement(
                "insert into DATAPOINT (SAMPLE_ID, SEQ, STEP) values (?, ?, ?)", Statement.RETURN_GENERATED_KEYS); //$NON-NLS-1$
        fInsertScalar= fConnection.prepareStatement(
                "insert into SCALAR values (?, ?, ?)"); //$NON-NLS-1$
        fInsertSampleProperty= fConnection.prepareStatement(
        		"insert into SAMPLE_PROPERTIES values (?, ?, ?)"); //$NON-NLS-1$

        fQueryScenario= fConnection.prepareStatement(
                "select ID from SCENARIO where NAME = ?"); //$NON-NLS-1$
        fQueryAllScenarios= fConnection.prepareStatement(
        		"select DISTINCT SCENARIO.NAME from SCENARIO, SAMPLE, CONFIG where " +	//$NON-NLS-1$
        		"SAMPLE.CONFIG_ID = CONFIG.ID and CONFIG.NAME LIKE ? and " +	//$NON-NLS-1$
        		"SAMPLE.SCENARIO_ID = SCENARIO.ID and SCENARIO.NAME LIKE ?"	//$NON-NLS-1$
        ); 
        fQueryBuilds= fConnection.prepareStatement(
        		"select DISTINCT CONFIG.BUILD from CONFIG, SAMPLE, SCENARIO where " +	//$NON-NLS-1$
        		"SAMPLE.CONFIG_ID = CONFIG.ID and CONFIG.NAME LIKE ? and CONFIG.BUILD LIKE ? and " +	//$NON-NLS-1$
        		"SAMPLE.SCENARIO_ID = SCENARIO.ID and SCENARIO.NAME LIKE ?"	//$NON-NLS-1$
        ); 
    }
    
    void initialize() throws SQLException {
        Statement stmt= fConnection.createStatement();
        
        stmt.executeUpdate(
                "create table CONFIG (" + //$NON-NLS-1$
                    "ID int not null GENERATED ALWAYS AS IDENTITY," + //$NON-NLS-1$
                    "NAME varchar(32)," + //$NON-NLS-1$
                    "BUILD varchar(64)" + //$NON-NLS-1$
                ")" //$NON-NLS-1$
            );
        stmt.executeUpdate(
                "create table CONFIG_PROPERTIES (" + //$NON-NLS-1$
                    "CONFIG_ID int not null," + //$NON-NLS-1$
					 "NAME varchar(128)," + //$NON-NLS-1$
                    "VALUE varchar(1000)" + //$NON-NLS-1$
                ")" //$NON-NLS-1$
            );
        stmt.executeUpdate(
        		"create table SAMPLE (" + //$NON-NLS-1$
        			"ID int not null GENERATED ALWAYS AS IDENTITY," + //$NON-NLS-1$
					"CONFIG_ID int not null," + //$NON-NLS-1$
					"SCENARIO_ID int not null," + //$NON-NLS-1$
					"VARIATION varchar(128)," + //$NON-NLS-1$
					"STARTTIME timestamp" + //$NON-NLS-1$
				")" //$NON-NLS-1$
        );           
        stmt.executeUpdate(
                "create table SAMPLE_PROPERTIES (" + //$NON-NLS-1$
                    "SAMPLE_ID int not null," + //$NON-NLS-1$
					 "NAME varchar(128)," + //$NON-NLS-1$
                    "VALUE varchar(1000)" + //$NON-NLS-1$
                ")" //$NON-NLS-1$
            );
        stmt.executeUpdate(
        		"create table SCENARIO (" + //$NON-NLS-1$
                	"ID int not null GENERATED ALWAYS AS IDENTITY," + //$NON-NLS-1$
					"NAME varchar(256)" + //$NON-NLS-1$
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
        stmt.close();
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
    
    int createSample(int config_id, int scenario_id, String variation, Timestamp starttime) throws SQLException {
        fInsertSample.setInt(1, config_id);
        fInsertSample.setInt(2, scenario_id);
        if (variation == null)
            fInsertSample.setNull(3, Types.VARCHAR);
        else
            fInsertSample.setString(3, variation);
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
    
    ResultSet queryDataPoints(String configPattern, String buildPattern, String scenario, int[] dim_ids) throws SQLException {
    	PreparedStatement queryStatement= getQueryStatement(dim_ids.length);
    	queryStatement.setString(1, configPattern);
    	queryStatement.setString(2, buildPattern);
    	queryStatement.setString(3, scenario);
    	int j= 4;
    	for (int i= 0; i < dim_ids.length; i++)
    		queryStatement.setInt(j++, dim_ids[i]);
        return queryStatement.executeQuery();
    }    

    ResultSet queryScenarios(String configPattern, String scenarioPattern) throws SQLException {
        fQueryAllScenarios.setString(1, configPattern);
        fQueryAllScenarios.setString(2, scenarioPattern);
        return fQueryAllScenarios.executeQuery();
    }
    
    ResultSet queryBuildNames(String configPattern, String buildPattern, String scenarioPattern) throws SQLException {
        fQueryBuilds.setString(1, configPattern);
        fQueryBuilds.setString(2, buildPattern);
        fQueryBuilds.setString(3, scenarioPattern);
        return fQueryBuilds.executeQuery();
    }

    void insertSampleProperty(int sample_id, String name, String value) throws SQLException {
        fInsertSampleProperty.setInt(1, sample_id);
        fInsertSampleProperty.setString(2, name);
        if (value == null)
            fInsertSampleProperty.setNull(3, Types.VARCHAR);
        else  
            fInsertSampleProperty.setString(3, value);
        fInsertSampleProperty.executeUpdate();
    }

    void insertConfigProperty(int configID, String name, String value) throws SQLException {
        //System.out.println(configID + " " + name + " " + value);
        fInsertConfigProperty.setInt(1, configID);
        fInsertConfigProperty.setString(2, name);
        if (value == null)
            fInsertConfigProperty.setNull(3, Types.VARCHAR);
        else  
            fInsertConfigProperty.setString(3, value);
        fInsertConfigProperty.executeUpdate();
    }
}
