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

import java.io.File;
import java.sql.*;
import java.util.Properties;

import org.eclipse.test.internal.performance.data.DataPoint;
import org.eclipse.test.internal.performance.Dimensions;
import org.eclipse.test.internal.performance.PerformanceTestPlugin;
import org.eclipse.test.internal.performance.data.Sample;
import org.eclipse.test.internal.performance.data.Scalar;

public class DB {
    
    public static final String DB_NAME= "perfDB";	// default db name
    
    private static DB fgDefault;
    
    private int fRefCount;
    private Connection fConnection;
    private SQL fSQL;
    
    
    public synchronized static DB acquire() {
        if (fgDefault == null) {
            fgDefault= new DB();          
            fgDefault.connect();
        }
        fgDefault.fRefCount++;
        return fgDefault;
    }
   
    public synchronized static void release(DB db) {
        if (db == null)
            return;
        if (db != fgDefault)
            System.err.println("DB.release: stale db");
        db.fRefCount--;
        if (db.fRefCount <= 0) {
            db.disconnect();
            if (db == fgDefault)
                fgDefault= null;
        }
    }

    public SQL getSQL() {
        return fSQL;
    }
    
    public void store(Sample sample) {
        
        if (fSQL == null || sample == null)
            return;
                
	    try {
            int config_id= fSQL.getConfig("burano", "MacOS X");
            int session_id= fSQL.addSession("3.1", config_id);
            int scenario_id= fSQL.getScenario(sample.getScenarioID());
            int sample_id= fSQL.createSample(session_id, scenario_id, 0);
            int draw_id= fSQL.createDraw(sample_id);
            int datapoint_id= fSQL.createDataPoint(draw_id);
            
			DataPoint[] dataPoints= sample.getDataPoints();
			for (int i= 0; i < dataPoints.length; i++) {
			    DataPoint dp= dataPoints[i];
			    Scalar[] scalars= dp.getScalars();
			    for (int j= 0; j < scalars.length; j++) {
			        Scalar scalar= scalars[j];
			        long value= scalar.getMagnitude();
			        int id= scalar.getDimension().getId();
			        fSQL.createScalar(datapoint_id, id, value);
                }
			}
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void query(String scenarioID, String refID) {
        if (fSQL == null) 
            return;
 
        ResultSet result= null;
        try {
            result= fSQL.query("burano", "MacOS X", refID, scenarioID, Dimensions.USER_TIME.getId());
            for (int i= 0; result.next(); i++)
                System.out.println(i + ": " + result.getString(1));
            result.close();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (result != null)
                try {
                    result.close();
                } catch (SQLException e1) {
                }
        }
    }

    //---- private implementation
    
    private DB() {
        // empty implementation
    }

    /**
     * dbloc=						embed in home directory
     * dbloc=/tmp/performance			embed given location
     * dbloc=net://localhost			connect to local server
     * dbloc=net://www.eclipse.org	connect to remove server
     */
    private void connect() {

        if (fConnection != null)
            return;
        
        Properties p= new Properties();
        PerformanceTestPlugin.getEnvironmentVariables(p);
        
        String dbloc= p.getProperty("dbloc");
        if (dbloc == null)
            return;
                
        String dbname= p.getProperty("dbname", DB_NAME);
        
        try {            
            if (dbloc.startsWith("net://")) {
                // connect over network
                System.out.println("Trying to connect over network...");
                Class.forName("com.ibm.db2.jcc.DB2Driver");
                String url="jdbc:cloudscape:" + dbloc + "/" + dbname + ";retrieveMessagesFromServerOnGetMessage=true;deferPrepares=true;";
                String dbuser= p.getProperty("dbuser", "guest");
                String dbpassword= p.getProperty("dbpasswd", "guest");
                fConnection= DriverManager.getConnection(url, dbuser, dbpassword);
            } else {
                // embedded
                System.out.println("Loading embedded cloudscape...");
                Class.forName("com.ihost.cs.jdbc.CloudscapeDriver");
                File f;
                if (dbloc.length() == 0) {
                    String user_home= System.getProperty("user.home");
                    if (user_home == null)
                        return;
                    f= new File(user_home, "cloudscape");
                } else
                    f= new File(dbloc);
                String dbpath= new File(f, dbname).getAbsolutePath();
                String url= "jdbc:cloudscape:" + dbpath + ";create=true";
                fConnection= DriverManager.getConnection(url);
            }
            System.out.println("succeeded!");
 
            fSQL= new SQL(fConnection);

            doesDBexists();

            System.out.println("start prepared statements");
            fSQL.createPreparedStatements();
            System.out.println("finish with prepared statements");
                         
        } catch (SQLException ex) {
            ex.printStackTrace();
            System.err.print("SQLException: ");
            System.err.println(ex.getMessage());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
    
    private void disconnect() {
        System.out.println("disconnecting from DB");
        if (fSQL != null) {
            try {
                fSQL.dispose();
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
            fSQL= null;
        }
        if (fConnection != null) {
            try {
                 fConnection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            fConnection= null;
        }
    }
    
    private void doesDBexists() throws SQLException {
        Statement stmt= fConnection.createStatement();
        try {
	        ResultSet rs= stmt.executeQuery("select count(*) from sys.systables");
	        while (rs.next())
	            if (rs.getInt(1) > 16)
	                return;
	        System.out.println("initialising DB");
	        fSQL.initialize();
	        System.out.println("end initialising DB");
        } finally {
            stmt.close();
        }
    }
    
    //---- test --------
    
    public static void main(String args[]) throws Exception {
        
        DB db= DB.acquire();
        
        try {            
            
            SQL sql= db.getSQL();
            
            /*
            System.out.println("adding to DB");
            int config_id= sql.getConfig("burano", "MacOS X");
            int session_id= sql.addSession("3.1", config_id);
            int scenario_id= sql.getScenario("foo.bar.Test.testFoo()");
            int sample_id= sql.createSample(session_id, scenario_id, 0);
            
            int time_id= sql.getDimension("time");
            int memory_id= sql.getDimension("memory");
            
            for (int r= 0; r < 10; r++) {	// runs
	            int draw_id= sql.createDraw(sample_id);
	            for (int d= 0; d < 2; d++) {	// start, end
	                int datapoint_id= sql.createDataPoint(draw_id);
	                sql.createScalar(datapoint_id, time_id, 12345*(d+1));		// time
	                sql.createScalar(datapoint_id, memory_id, 14*(d+1));		// memory
	            }
            }
            System.out.println("end adding to DB");
            */
            
            ResultSet result= sql.query("burano", "MacOS X", "3.1", "aFirstTest", Dimensions.USER_TIME.getId());
            for (int i= 0; result.next(); i++)
                System.out.println(i + ": " + result.getString(1));
            result.close();
             
        } catch (SQLException ex) {
            System.err.print("SQLException: ");
            System.err.println(ex.getMessage());
            ex.printStackTrace();
        }
        
        DB.release(db);       
    }
}
