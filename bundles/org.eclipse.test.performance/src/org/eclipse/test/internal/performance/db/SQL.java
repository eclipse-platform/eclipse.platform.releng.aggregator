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

/*
 * Any SQL should only be used here.
 */
public class SQL {
    
    private boolean fCompatibility= false;
    
    private Connection fConnection;
    
    private PreparedStatement fInsertVariation, fInsertScenario, fInsertSample, fInsertDataPoint, fInsertScalar;
    private PreparedStatement fQueryVariation, fQueryVariations, fQueryScenario, fQueryAllScenarios, fQueryDatapoints, fQueryScalars;
    private PreparedStatement fInsertSummaryEntry, fUpdateScenarioShortName, fQuerySummaryEntry, fQuerySummaryEntries;
    

    SQL(Connection con) throws SQLException {
        fConnection= con;
        
        boolean needsUpgrade= true;
        boolean needsInitialization= true;
        
        Statement statement= fConnection.createStatement();
        ResultSet rs= statement.executeQuery("select SYS.SYSTABLES.TABLENAME from SYS.SYSTABLES where SYS.SYSTABLES.TABLENAME not like 'SYS%'"); //$NON-NLS-1$
        while (rs.next()) {
            String tablename= rs.getString(1);
            if ("SUMMARYENTRY".equals(tablename)) //$NON-NLS-1$
                needsUpgrade= false;
            else if ("CONFIG_ORG".equals(tablename)) //$NON-NLS-1$
                fCompatibility= true;
            else if ("VARIATION".equals(tablename)) //$NON-NLS-1$
                needsInitialization= false;
        }
        if (!fCompatibility) {
            // check whether table SAMPLE still has the CONFIG_ID column
            rs= statement.executeQuery("select count(*) from SYS.SYSTABLES, SYS.SYSCOLUMNS where SYS.SYSTABLES.TABLENAME = 'SAMPLE' and " + //$NON-NLS-1$
        			"SYS.SYSTABLES.TABLEID = SYS.SYSCOLUMNS.REFERENCEID and SYS.SYSCOLUMNS.COLUMNNAME = 'CONFIG_ID' "); //$NON-NLS-1$
            if (rs.next() && rs.getInt(1) == 1)
                fCompatibility= true;
        }

        if (needsInitialization)
            initialize();
        else if (needsUpgrade)
	    	upgradeDB();
    }
    
    public void dispose() throws SQLException {
        if (fInsertVariation != null) fInsertVariation.close();
        if (fInsertScenario != null) fInsertScenario.close();
        if (fInsertSample != null) fInsertSample.close();
        if (fInsertDataPoint != null) fInsertDataPoint.close();
        if (fInsertScalar != null) fInsertScalar.close();
        if (fInsertSummaryEntry != null) fInsertSummaryEntry.close();
        if (fUpdateScenarioShortName != null) fUpdateScenarioShortName.close();
        if (fQueryDatapoints != null) fQueryDatapoints.close();
        if (fQueryScalars != null) fQueryScalars.close();
        if (fQueryVariation != null) fQueryVariation.close();
        if (fQueryScenario != null) fQueryScenario.close();
        if (fQueryAllScenarios != null) fQueryAllScenarios.close();
        if (fQueryVariations != null) fQueryVariations.close();
        if (fQuerySummaryEntries != null) fQuerySummaryEntries.close();
    }
        
    private void initialize() throws SQLException {
        Statement stmt= null;
        try {
            stmt= fConnection.createStatement();
	        stmt.executeUpdate(
	                "create table VARIATION (" + //$NON-NLS-1$
	                    "ID int unique not null GENERATED ALWAYS AS IDENTITY," + //$NON-NLS-1$
	                    "KEYVALPAIRS varchar(10000) not null " + //$NON-NLS-1$
	                ")" //$NON-NLS-1$
	            );
	        stmt.executeUpdate(
	        		"create table SCENARIO (" + //$NON-NLS-1$
	                	"ID int unique not null GENERATED ALWAYS AS IDENTITY," + //$NON-NLS-1$
						"NAME varchar(256) not null," + //$NON-NLS-1$
						"SHORT_NAME varchar(40)" + //$NON-NLS-1$
					")" //$NON-NLS-1$
	        );
	        stmt.executeUpdate(
	        		"create table SAMPLE (" + //$NON-NLS-1$
	        			"ID int not null GENERATED ALWAYS AS IDENTITY," + //$NON-NLS-1$
	        			"VARIATION_ID int not null," + //$NON-NLS-1$
						"SCENARIO_ID int not null," + //$NON-NLS-1$
						"STARTTIME timestamp" + //$NON-NLS-1$
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
	        stmt.executeUpdate(
	        		"create table SUMMARYENTRY (" + //$NON-NLS-1$
	                	"VARIATION_ID int not null," + //$NON-NLS-1$
						"SCENARIO_ID int not null," + //$NON-NLS-1$
						"DIM_ID int not null," + //$NON-NLS-1$
						"IS_GLOBAL smallint not null" + //$NON-NLS-1$
					")" //$NON-NLS-1$
	        );
	        
	        // Primary/unique
	        stmt.executeUpdate("alter table VARIATION add constraint VA_KVP primary key (KEYVALPAIRS)"); //$NON-NLS-1$
	        stmt.executeUpdate("alter table SCENARIO add constraint SC_NAME primary key (NAME)"); //$NON-NLS-1$
	        stmt.executeUpdate("alter table SAMPLE add constraint SA_ID primary key (ID)"); //$NON-NLS-1$
	        stmt.executeUpdate("alter table DATAPOINT add constraint DP_ID primary key (ID)"); //$NON-NLS-1$
	        
	        // Foreign
	        stmt.executeUpdate("alter table SAMPLE add constraint SAMPLE_CONSTRAINT " + //$NON-NLS-1$
					"foreign key (VARIATION_ID) references VARIATION (ID)"); //$NON-NLS-1$
	        stmt.executeUpdate("alter table SAMPLE add constraint SAMPLE_CONSTRAINT2 " + //$NON-NLS-1$
					"foreign key (SCENARIO_ID) references SCENARIO (ID)"); //$NON-NLS-1$
	        stmt.executeUpdate("alter table DATAPOINT add constraint DP_CONSTRAINT " + //$NON-NLS-1$
	        		"foreign key (SAMPLE_ID) references SAMPLE (ID)"); //$NON-NLS-1$
	        stmt.executeUpdate("alter table SCALAR add constraint SCALAR_CONSTRAINT " + //$NON-NLS-1$
	        		"foreign key (DATAPOINT_ID) references DATAPOINT (ID)"); //$NON-NLS-1$

	        stmt.executeUpdate("alter table SUMMARYENTRY add constraint FP_CONSTRAINT " + //$NON-NLS-1$
					"foreign key (VARIATION_ID) references VARIATION (ID)"); //$NON-NLS-1$
	        stmt.executeUpdate("alter table SUMMARYENTRY add constraint FP_CONSTRAINT2 " + //$NON-NLS-1$
					"foreign key (SCENARIO_ID) references SCENARIO (ID)"); //$NON-NLS-1$

	        fConnection.commit();
	        
        } finally {
            stmt.close();
        }
    }

    private void upgradeDB() throws SQLException {
        Statement stmt= null;
        try {
            stmt= fConnection.createStatement();

	        stmt.executeUpdate(
	        		"create table SUMMARYENTRY (" + //$NON-NLS-1$
	                	"VARIATION_ID int not null," + //$NON-NLS-1$
						"SCENARIO_ID int not null," + //$NON-NLS-1$
						"DIM_ID int not null," + //$NON-NLS-1$
						"IS_GLOBAL smallint not null" + //$NON-NLS-1$
					")" //$NON-NLS-1$
	        );
	        stmt.executeUpdate("alter table SUMMARYENTRY add constraint FP_CONSTRAINT " + //$NON-NLS-1$
				"foreign key (VARIATION_ID) references VARIATION (ID)"); //$NON-NLS-1$
	        stmt.executeUpdate("alter table SUMMARYENTRY add constraint FP_CONSTRAINT2 " + //$NON-NLS-1$
				"foreign key (SCENARIO_ID) references SCENARIO (ID)"); //$NON-NLS-1$

            stmt.executeUpdate("alter table SCENARIO add column SHORT_NAME varchar(40)"); //$NON-NLS-1$

	        fConnection.commit();
	        
        } finally {
            stmt.close();
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
        if (fQueryScenario == null)
            fQueryScenario= fConnection.prepareStatement("select ID from SCENARIO where NAME = ?"); //$NON-NLS-1$
        fQueryScenario.setString(1, scenarioPattern);
        ResultSet result= fQueryScenario.executeQuery();
        while (result.next())
            return result.getInt(1);

        if (fInsertScenario == null)
            fInsertScenario= fConnection.prepareStatement(
                "insert into SCENARIO (NAME) values (?)", Statement.RETURN_GENERATED_KEYS); //$NON-NLS-1$
        fInsertScenario.setString(1, scenarioPattern);
        return create(fInsertScenario);
    }
    
    int getVariations(Variations variations) throws SQLException {
        if (fQueryVariation == null)
            fQueryVariation= fConnection.prepareStatement("select ID from VARIATION where KEYVALPAIRS = ?"); //$NON-NLS-1$
        String exactMatchString= variations.toExactMatchString();
        fQueryVariation.setString(1, exactMatchString);
        ResultSet result= fQueryVariation.executeQuery();
        while (result.next())
            return result.getInt(1);

        if (fInsertVariation == null)
            fInsertVariation= fConnection.prepareStatement(
                "insert into VARIATION (KEYVALPAIRS) values (?)", Statement.RETURN_GENERATED_KEYS); //$NON-NLS-1$
        fInsertVariation.setString(1, exactMatchString);
        return create(fInsertVariation);
    }

    int createSample(int variation_id, int scenario_id, Timestamp starttime) throws SQLException {
        if (fInsertSample == null) {
            if (fCompatibility) {
                // since we cannot remove table columns in cloudscape we have to provide a non-null value for CONFIG_ID
                fInsertSample= fConnection.prepareStatement(
                        "insert into SAMPLE (VARIATION_ID, SCENARIO_ID, STARTTIME, CONFIG_ID) values (?, ?, ?, 0)", Statement.RETURN_GENERATED_KEYS); //$NON-NLS-1$
            } else {
                fInsertSample= fConnection.prepareStatement(
                        "insert into SAMPLE (VARIATION_ID, SCENARIO_ID, STARTTIME) values (?, ?, ?)", Statement.RETURN_GENERATED_KEYS); //$NON-NLS-1$
            }
        }
        fInsertSample.setInt(1, variation_id);
        fInsertSample.setInt(2, scenario_id);
        fInsertSample.setTimestamp(3, starttime);
        return create(fInsertSample);
    }
        
    int createDataPoint(int sample_id, int seq, int step) throws SQLException {
        if (fInsertDataPoint == null)
            fInsertDataPoint= fConnection.prepareStatement(
                "insert into DATAPOINT (SAMPLE_ID, SEQ, STEP) values (?, ?, ?)", Statement.RETURN_GENERATED_KEYS); //$NON-NLS-1$
        fInsertDataPoint.setInt(1, sample_id);
        fInsertDataPoint.setInt(2, seq);
        fInsertDataPoint.setInt(3, step);
        return create(fInsertDataPoint);
    }
    
    void insertScalar(int datapoint_id, int dim_id, long value) throws SQLException {
        if (fInsertScalar == null)
            fInsertScalar= fConnection.prepareStatement("insert into SCALAR values (?, ?, ?)"); //$NON-NLS-1$
		fInsertScalar.setInt(1, datapoint_id);
		fInsertScalar.setInt(2, dim_id);
		fInsertScalar.setLong(3, value);
		fInsertScalar.executeUpdate();
    }
    
    ResultSet queryDataPoints(Variations variations, String scenarioName) throws SQLException {
        if (fQueryDatapoints == null)
            fQueryDatapoints= fConnection.prepareStatement(
        		"select DATAPOINT.ID, DATAPOINT.STEP from VARIATION, SCENARIO, SAMPLE, DATAPOINT " + //$NON-NLS-1$
        		"where " + //$NON-NLS-1$
        		"SAMPLE.VARIATION_ID = VARIATION.ID and VARIATION.KEYVALPAIRS = ? and " + //$NON-NLS-1$
        		"SAMPLE.SCENARIO_ID = SCENARIO.ID and SCENARIO.NAME LIKE ? and " + //$NON-NLS-1$
         		"DATAPOINT.SAMPLE_ID = SAMPLE.ID " //$NON-NLS-1$
            );
        fQueryDatapoints.setString(1, variations.toExactMatchString());
        fQueryDatapoints.setString(2, scenarioName);
        return fQueryDatapoints.executeQuery();
    }

    ResultSet queryScalars(int datapointId) throws SQLException {
        if (fQueryScalars == null)
            fQueryScalars= fConnection.prepareStatement(
        		"select SCALAR.DIM_ID, SCALAR.VALUE from SCALAR where SCALAR.DATAPOINT_ID = ?"); //$NON-NLS-1$
        fQueryScalars.setInt(1, datapointId);
    	return fQueryScalars.executeQuery();
    }

    /*
     * Returns SCENARIO.NAME
     */
    ResultSet queryScenarios(Variations variations, String scenarioPattern) throws SQLException {
        if (fQueryAllScenarios == null)
            fQueryAllScenarios= fConnection.prepareStatement(
        		"select distinct SCENARIO.NAME from SCENARIO, SAMPLE, VARIATION where " +	//$NON-NLS-1$
        		"SAMPLE.VARIATION_ID = VARIATION.ID and VARIATION.KEYVALPAIRS LIKE ? and " +	//$NON-NLS-1$
        		"SAMPLE.SCENARIO_ID = SCENARIO.ID and SCENARIO.NAME LIKE ?"	//$NON-NLS-1$
            ); 
        fQueryAllScenarios.setString(1, variations.toQueryPattern());
        fQueryAllScenarios.setString(2, scenarioPattern);
        return fQueryAllScenarios.executeQuery();
    }
    
    /*
     * Returns VARIATION.KEYVALPAIRS
     */
    ResultSet queryVariations(String variations, String scenarioPattern) throws SQLException {
        if (fQueryVariations == null)
            fQueryVariations= fConnection.prepareStatement(
        		"select distinct VARIATION.KEYVALPAIRS from VARIATION, SAMPLE, SCENARIO where " +	//$NON-NLS-1$
        		"SAMPLE.VARIATION_ID = VARIATION.ID and VARIATION.KEYVALPAIRS LIKE ? and " +	//$NON-NLS-1$
        		"SAMPLE.SCENARIO_ID = SCENARIO.ID and SCENARIO.NAME LIKE ?"	//$NON-NLS-1$
            ); 
        fQueryVariations.setString(1, variations);
        fQueryVariations.setString(2, scenarioPattern);
        return fQueryVariations.executeQuery();
    }
    
    void createSummaryEntry(int variation_id, int scenario_id, int dim_id, boolean isGlobal) throws SQLException {
        if (fQuerySummaryEntry == null)
            fQuerySummaryEntry= fConnection.prepareStatement(
            	"select count(*) from SUMMARYENTRY where VARIATION_ID = ? and SCENARIO_ID = ? and DIM_ID = ? and IS_GLOBAL = ?"); //$NON-NLS-1$
        fQuerySummaryEntry.setInt(1, variation_id);
        fQuerySummaryEntry.setInt(2, scenario_id);
        fQuerySummaryEntry.setInt(3, dim_id);
        fQuerySummaryEntry.setShort(4, (short) (isGlobal ? 1 : 0));
        ResultSet result= fQuerySummaryEntry.executeQuery();
        if (result.next() && result.getInt(1) > 0)
            return;
        
        if (fInsertSummaryEntry == null)
            fInsertSummaryEntry= fConnection.prepareStatement(
                "insert into SUMMARYENTRY (VARIATION_ID, SCENARIO_ID, DIM_ID, IS_GLOBAL) values (?, ?, ?, ?)"); //$NON-NLS-1$
        fInsertSummaryEntry.setInt(1, variation_id);
        fInsertSummaryEntry.setInt(2, scenario_id);
        fInsertSummaryEntry.setInt(3, dim_id);
        fInsertSummaryEntry.setShort(4, (short) (isGlobal ? 1 : 0));
        fInsertSummaryEntry.executeUpdate();
    }

    public void setScenarioShortName(int scenario_id, String shortName) throws SQLException {
        if (shortName.length() >= 40)
            shortName= shortName.substring(0, 40);
        if (fUpdateScenarioShortName == null)
            fUpdateScenarioShortName= fConnection.prepareStatement(
        		"update SCENARIO set SHORT_NAME = ? where SCENARIO.ID = ?");	//$NON-NLS-1$
        fUpdateScenarioShortName.setString(1, shortName);
        fUpdateScenarioShortName.setInt(2, scenario_id);
        fUpdateScenarioShortName.executeUpdate();
    }   

    ResultSet querySummaryEntries(Variations variations, boolean global) throws SQLException {
        if (fQuerySummaryEntries == null)
            fQuerySummaryEntries= fConnection.prepareStatement(
            		"select distinct SCENARIO.NAME, SCENARIO.SHORT_NAME, SUMMARYENTRY.DIM_ID from SUMMARYENTRY, VARIATION, SCENARIO where " +	//$NON-NLS-1$
            		"SUMMARYENTRY.VARIATION_ID = VARIATION.ID and VARIATION.KEYVALPAIRS LIKE ? and " +	//$NON-NLS-1$
            		"SUMMARYENTRY.SCENARIO_ID = SCENARIO.ID and " + //$NON-NLS-1$
            		"SUMMARYENTRY.IS_GLOBAL = ?"	//$NON-NLS-1$
            ); 
        fQuerySummaryEntries.setString(1, variations.toExactMatchString());
        fQuerySummaryEntries.setInt(2, global ? 1 : 0);
        return fQuerySummaryEntries.executeQuery();
    }
}
