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

public class SQL {
    
    private Connection fConn;
    private PreparedStatement fInsertSample, fInsertDataPoint;
    private PreparedStatement fQueryConfig, fInsertConfig;
    private PreparedStatement fQueryScenario, fQueryAllScenarios, fInsertScenario;
    private PreparedStatement fQueryTag, fQueryTags, fInsertTag;
    private PreparedStatement[] fInsertScalar= new PreparedStatement[20];
    private PreparedStatement[] fQueries= new PreparedStatement[20];
    

    SQL(Connection con) {
        fConn= con;
    }
    
    public void dispose() throws SQLException {
    	// TODO: close all prepared statements
    	if (fInsertScalar != null) {
    		for (int i= 0; i < fInsertScalar.length; i++) {
    			if (fInsertScalar[i] != null)
    				fInsertScalar[i].close();
    		}
    		fInsertScalar= null;
    	}
    	if (fQueries != null) {
    		for (int i= 0; i < fQueries.length; i++) {
    			if (fQueries[i] != null)
    				fQueries[i].close();
    		}
    		fQueries= null;
    	}
    }
    
    PreparedStatement getScalarInsertStatement(int n) throws SQLException {
    	if (n < 1 || n > 20)
    		return null;
    	if (fInsertScalar[n-1] == null) {
    		StringBuffer sb= new StringBuffer("insert into SCALAR values (?, ?, ?)"); //$NON-NLS-1$
    		for (int i= 1; i < n; i++)
    			sb.append(", (?, ?, ?)"); //$NON-NLS-1$
            fInsertScalar[n-1]= fConn.prepareStatement(sb.toString());
    	}
    	return fInsertScalar[n-1];
    }
    
    PreparedStatement getQueryStatement(int n) throws SQLException {
    	if (n < 0 || n > 20)
    		return null;
    	if (fQueries[n] == null) {
    		StringBuffer sb= new StringBuffer(
    				"select SAMPLE.ID,DATAPOINT.ID, DATAPOINT.STEP, SCALAR.DIM_ID, SCALAR.VALUE, STARTTIME from SCALAR, DATAPOINT, SAMPLE, SCENARIO, CONFIG, TAG " + //$NON-NLS-1$
					"where " + //$NON-NLS-1$
					"SAMPLE.CONFIG_ID = ? and " + //$NON-NLS-1$
					"SAMPLE.TAG_ID = TAG.ID and TAG.NAME = ? and " + //$NON-NLS-1$
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
    		
			fQueries[n]= fConn.prepareStatement(sb.toString());
    	}
    	return fQueries[n];
    }
    
    void createPreparedStatements() throws SQLException {
        fInsertTag= fConn.prepareStatement(
                "insert into TAG (NAME) values (?)", Statement.RETURN_GENERATED_KEYS); //$NON-NLS-1$
        fInsertConfig= fConn.prepareStatement(
                "insert into CONFIG (HOST, PLATFORM) values (?, ?)", Statement.RETURN_GENERATED_KEYS); //$NON-NLS-1$
        fInsertScenario= fConn.prepareStatement(
                "insert into SCENARIO (NAME) values (?)", Statement.RETURN_GENERATED_KEYS); //$NON-NLS-1$
        fInsertSample= fConn.prepareStatement(
                "insert into SAMPLE (CONFIG_ID, SCENARIO_ID, TAG_ID, STARTTIME) values (?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS); //$NON-NLS-1$
        fInsertDataPoint= fConn.prepareStatement(
                "insert into DATAPOINT (SAMPLE_ID, SEQ, STEP) values (?, ?, ?)", Statement.RETURN_GENERATED_KEYS); //$NON-NLS-1$

        fQueryTag= fConn.prepareStatement(
        		"select ID from TAG where NAME = ?"); //$NON-NLS-1$
        fQueryConfig= fConn.prepareStatement(
                "select ID from CONFIG where HOST = ? and PLATFORM = ?"); //$NON-NLS-1$
        fQueryScenario= fConn.prepareStatement(
                "select ID from SCENARIO where NAME = ?"); //$NON-NLS-1$
        fQueryAllScenarios= fConn.prepareStatement(
        		"select DISTINCT SCENARIO.NAME from SCENARIO, SAMPLE where " +	//$NON-NLS-1$
        		"SAMPLE.CONFIG_ID = ? and " +	//$NON-NLS-1$
        		"SAMPLE.SCENARIO_ID = SCENARIO.ID"	//$NON-NLS-1$
        ); 
        fQueryTags= fConn.prepareStatement(
        		"select TAG.NAME from TAG, SAMPLE, SCENARIO where " +	//$NON-NLS-1$
        		"SAMPLE.CONFIG_ID = ? and " +	//$NON-NLS-1$
        		"SAMPLE.SCENARIO_ID = SCENARIO.ID and SCENARIO.NAME = ? and " +	//$NON-NLS-1$
        		"SAMPLE.TAG_ID = TAG.ID"	//$NON-NLS-1$
        ); 
    }
    
    void initialize() throws SQLException {
        Statement stmt= fConn.createStatement();
        
        stmt.executeUpdate(
        		"create table SAMPLE (" + //$NON-NLS-1$
        			"ID int not null GENERATED ALWAYS AS IDENTITY," + //$NON-NLS-1$
					"CONFIG_ID int not null," + //$NON-NLS-1$
					"SCENARIO_ID int not null," + //$NON-NLS-1$
					"TAG_ID int," + //$NON-NLS-1$
					"STARTTIME bigint" + //$NON-NLS-1$
				")" //$NON-NLS-1$
        );           
        stmt.executeUpdate(
                "create table CONFIG (" + //$NON-NLS-1$
                    "ID int not null GENERATED ALWAYS AS IDENTITY," + //$NON-NLS-1$
                    "HOST varchar(40)," + //$NON-NLS-1$
                    "PLATFORM varchar(20)" + //$NON-NLS-1$
                ")" //$NON-NLS-1$
            );
        stmt.executeUpdate(
                "create table TAG (" + //$NON-NLS-1$
                	"ID int not null GENERATED ALWAYS AS IDENTITY," + //$NON-NLS-1$
                	"NAME varchar(255)" + //$NON-NLS-1$
                ")" //$NON-NLS-1$
            );
        stmt.executeUpdate(
        		"create table SCENARIO (" + //$NON-NLS-1$
                	"ID int not null GENERATED ALWAYS AS IDENTITY," + //$NON-NLS-1$
					"NAME varchar(255)" + //$NON-NLS-1$
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

    public int getTag(String tag)  throws SQLException {
        fQueryTag.setString(1, tag);
        ResultSet result= fQueryTag.executeQuery();
        while (result.next())
            return result.getInt(1);
        
        fInsertTag.setString(1, tag);
        return create(fInsertTag);
    }

    int getConfig(String host, String platform) throws SQLException {
        fQueryConfig.setString(1, host);
        fQueryConfig.setString(2, platform);
        ResultSet result= fQueryConfig.executeQuery();
        while (result.next())
            return result.getInt(1);

        fInsertConfig.setString(1, host);
        fInsertConfig.setString(2, platform);
        return create(fInsertConfig);
    }
    
    int getScenario(String name) throws SQLException {
        fQueryScenario.setString(1, name);
        ResultSet result= fQueryScenario.executeQuery();
        while (result.next())
            return result.getInt(1);

        fInsertScenario.setString(1, name);
        return create(fInsertScenario);
    }
    
    int createSample(int config_id, int scenario_id, int tag_id, long starttime) throws SQLException {
        fInsertSample.setInt(1, config_id);
        fInsertSample.setInt(2, scenario_id);
        fInsertSample.setInt(3, tag_id);
        fInsertSample.setBigDecimal(4, new BigDecimal(starttime));
        return create(fInsertSample);
    }
        
    int createDataPoint(int sample_id, int seq, int step) throws SQLException {
        fInsertDataPoint.setInt(1, sample_id);
        fInsertDataPoint.setInt(2, seq);
        fInsertDataPoint.setInt(3, step);
        return create(fInsertDataPoint);
    }
    
    ResultSet query(int config_id, String tag, String scenario, int[] dim_ids) throws SQLException {
    	PreparedStatement queryStatement= getQueryStatement(dim_ids.length);
    	queryStatement.setInt(1, config_id);
    	queryStatement.setString(2, tag);
    	queryStatement.setString(3, scenario);
    	int j= 4;
    	for (int i= 0; i < dim_ids.length; i++)
    		queryStatement.setInt(j++, dim_ids[i]);
        return queryStatement.executeQuery();
    }    

    ResultSet queryScenarios(int config_id) throws SQLException {
        fQueryAllScenarios.setInt(1, config_id);
        return fQueryAllScenarios.executeQuery();
    }
    
    ResultSet queryTags(int config_id, String scenario) throws SQLException {
        fQueryTags.setInt(1, config_id);
        fQueryTags.setString(2, scenario);
        return fQueryTags.executeQuery();
    }
}
