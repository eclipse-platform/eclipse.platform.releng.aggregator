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
    private PreparedStatement fInsertSample, fInsertDataPoint, fInsertScalar;
    private PreparedStatement fQueryConfig, fInsertConfig;
    private PreparedStatement fQueryScenario, fInsertScenario;
    private PreparedStatement fQueryTag, fInsertTag;
    private PreparedStatement fQuery1, fQuery3;
    

    SQL(Connection con) {
        fConn= con;
    }
    
    public void dispose() throws SQLException {
    }
    
    void createPreparedStatements() throws SQLException {
        fInsertTag= fConn.prepareStatement(
                "insert into TAG (NAME) values (?)", Statement.RETURN_GENERATED_KEYS);
        fInsertConfig= fConn.prepareStatement(
                "insert into CONFIG (HOST, PLATFORM) values (?, ?)", Statement.RETURN_GENERATED_KEYS);
        fInsertScenario= fConn.prepareStatement(
                "insert into SCENARIO (NAME) values (?)", Statement.RETURN_GENERATED_KEYS);
        fInsertSample= fConn.prepareStatement(
                "insert into SAMPLE (CONFIG_ID, SCENARIO_ID, TAG_ID, STARTTIME) values (?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
        fInsertDataPoint= fConn.prepareStatement(
                "insert into DATAPOINT (SAMPLE_ID, SEQ, STEP) values (?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
        fInsertScalar= fConn.prepareStatement(
                "insert into SCALAR values (?, ?, ?)");

        fQueryTag= fConn.prepareStatement(
        			"select ID from TAG where NAME = ?");
        fQueryConfig= fConn.prepareStatement(
                "select ID from CONFIG where HOST = ? and PLATFORM = ?");
        fQueryScenario= fConn.prepareStatement(
                "select ID from SCENARIO where NAME = ?");
        
        fQuery1= fConn.prepareStatement(
                "select SCALAR.VALUE from SCALAR, DATAPOINT, SAMPLE, SCENARIO, CONFIG, TAG " +
            		"where " +
                 "SAMPLE.CONFIG_ID = CONFIG.ID and CONFIG.HOST = ? and CONFIG.PLATFORM = ? and " +
          		"SAMPLE.TAG_ID = TAG.ID and TAG.NAME = ? and " +
          		"SAMPLE.SCENARIO_ID = SCENARIO.ID and SCENARIO.NAME = ? and " +
            		"SCALAR.DATAPOINT_ID = DATAPOINT.ID and " +
            		"DATAPOINT.SAMPLE_ID = SAMPLE.ID and " +
            		"SCALAR.DIM_ID = ?"
        			);
        fQuery3= fConn.prepareStatement(
                "select SAMPLE.ID,DATAPOINT.ID, DATAPOINT.STEP, SCALAR.DIM_ID, SCALAR.VALUE, STARTTIME from SCALAR, DATAPOINT, SAMPLE, SCENARIO, CONFIG, TAG " +
            		"where " +
                 "SAMPLE.CONFIG_ID = ? and " +
         		"SAMPLE.TAG_ID = TAG.ID and TAG.NAME = ? and " +
         		"SAMPLE.SCENARIO_ID = SCENARIO.ID and SCENARIO.NAME = ? and " +
            		"SCALAR.DATAPOINT_ID = DATAPOINT.ID and " +
            		"DATAPOINT.SAMPLE_ID = SAMPLE.ID " +
            		"order by SAMPLE.STARTTIME, DATAPOINT.ID, DATAPOINT.STEP"
            		);
    }
    
    void initialize() throws SQLException {
        Statement stmt= fConn.createStatement();
        
        stmt.executeUpdate(
            "create table SAMPLE (" +
                "ID int not null GENERATED ALWAYS AS IDENTITY," +
                "CONFIG_ID int not null," +
                "SCENARIO_ID int not null," +
                "TAG_ID int," +
                "STARTTIME bigint" +
            ")"
        );           
        stmt.executeUpdate(
                "create table CONFIG (" +
                    "ID int not null GENERATED ALWAYS AS IDENTITY," +
                    "HOST varchar(40)," +
                    "PLATFORM varchar(20)" +
                ")"
            );
        stmt.executeUpdate(
                "create table TAG (" +
                		"ID int not null GENERATED ALWAYS AS IDENTITY," +
                		"NAME varchar(255)" +
                ")"
            );
        stmt.executeUpdate(
            "create table SCENARIO (" +
                "ID int not null GENERATED ALWAYS AS IDENTITY," +
                "NAME varchar(255)" +
            ")"
        );     
        stmt.executeUpdate(
            "create table DATAPOINT (" +
                "ID int not null GENERATED ALWAYS AS IDENTITY," +
                "SAMPLE_ID int not null," +
                "SEQ int," +
                "STEP int" +
            ")"
        );
        stmt.executeUpdate(
            "create table SCALAR (" +
                "DATAPOINT_ID int not null," +
                "DIM_ID int not null," +
                "VALUE bigint" +
            ")"
        ); 
        stmt.close();
    }
    
    private static int create(PreparedStatement stmt) throws SQLException {
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

    void createScalar(int dataPoint_id, int dim_id, long value) throws SQLException {
        fInsertScalar.setInt(1, dataPoint_id);
        fInsertScalar.setInt(2, dim_id);
        fInsertScalar.setLong(3, value);
        create(fInsertScalar);
    }
    
    ResultSet query1(String host, String platform, String version, String scenario, int dim_id) throws SQLException {
        fQuery1.setInt(1, dim_id);
        fQuery1.setString(2, scenario);
        fQuery1.setString(3, version);
        fQuery1.setString(4, host);
        fQuery1.setString(5, platform);
        return fQuery1.executeQuery();
    }
    
    ResultSet query3(int config_id, String tag, String scenario) throws SQLException {
        fQuery3.setInt(1, config_id);
        fQuery3.setString(2, tag);
        fQuery3.setString(3, scenario);
        return fQuery3.executeQuery();
    }
}
