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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

import org.eclipse.test.internal.performance.data.DataPoint;
import org.eclipse.test.internal.performance.Dimensions;
import org.eclipse.test.internal.performance.PerformanceTestPlugin;
import org.eclipse.test.internal.performance.data.Dimension;
import org.eclipse.test.internal.performance.data.Sample;
import org.eclipse.test.internal.performance.data.Scalar;

public class DB {
    
    public static final String DB_NAME= "perfDB";	// default db name
    
    private static final boolean DEBUG= true;
    
    private static DB fgDefault;
    
    private Connection fConnection;
    private SQL fSQL;
    private Properties fProperties;
    private int fTagID= -1;
    private int fConfigID;
    
    
    private int getTag() throws SQLException {
        if (fTagID < 0) {
            String tag= PerformanceTestPlugin.getEnvironment("setTag");
            if (tag != null)
                fTagID= fSQL.getTag(tag);
            else
                fTagID= 0;
        }
        return fTagID;
    }
    
    private int getConfig() throws SQLException {
        if (fConfigID == 0)
            fConfigID= fSQL.getConfig("burano", "MacOS X");
        return fConfigID;
    }
    
    public static void store(Sample sample) {
        getDefault().internalStore(sample);
    }
    
    public static DataPoint[] query(String refTag, String scenarioID) {
        return getDefault().internalQuery(refTag, scenarioID);
    }
    
    //---- private implementation
    
    private DB() {
        // empty implementation
    }

    private synchronized static DB getDefault() {
        if (fgDefault == null) {
            fgDefault= new DB();       
            fgDefault.connect();
            Runtime.getRuntime().addShutdownHook(
                new Thread() {
                    public void run() {
                        if (DEBUG) System.out.println("shutdownhook");
                        if (fgDefault != null) {
                            fgDefault.disconnect();
                            fgDefault= null;
                        }
                    }
                }
            );
        }
        return fgDefault;
    }
   
    private SQL getSQL() {
        return fSQL;
    }

    private void internalStore(Sample sample) {
        
        if (fSQL == null || sample == null)
            return;
        
	    try {
	        int tag_id= getTag();
            int scenario_id= fSQL.getScenario(sample.getScenarioID());
            int sample_id= fSQL.createSample(getConfig(), scenario_id, tag_id, sample.getStartTime());
            
			DataPoint[] dataPoints= sample.getDataPoints();
			for (int i= 0; i < dataPoints.length; i++) {
			    DataPoint dp= dataPoints[i];
	            int datapoint_id= fSQL.createDataPoint(sample_id, i, dp.getStep());
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

    private DataPoint[] internalQuery(String refTag, String scenarioID) {
        if (fSQL == null) 
            return null;
 
        ResultSet result= null;
        try {
            result= fSQL.query3(getConfig(), refTag, scenarioID);
            ArrayList dataPoints= new ArrayList();
            int lastDataPointId= 0;
            DataPoint dp= null;
            HashMap map= null;
            for (int i= 0; result.next(); i++) {
                
		        int sample_id= result.getInt(1);
		        int datapoint_id= result.getInt(2);
		        int step= result.getInt(3);
                int dim_id= result.getInt(4);
                long value= result.getBigDecimal(5).longValue();
                Date d= new Date(result.getBigDecimal(6).longValue());
                
                if (datapoint_id != lastDataPointId) {
                    map= new HashMap();
                    dp= new DataPoint(step, map);
                    dataPoints.add(dp);
                    lastDataPointId= datapoint_id;
                }
                Dimension dim= Dimension.getDimension(dim_id);
                if (dim != null)
                    map.put(dim, new Scalar(dim, value));
                
                //System.out.println(i + ": " + sample_id+","+datapoint_id +","+step+ " " + Dimension.getDimension(dim_id).getName() + " " + value + " " + d.toGMTString());                
            }
            
            return (DataPoint[])dataPoints.toArray(new DataPoint[dataPoints.size()]);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (result != null)
                try {
                    result.close();
                } catch (SQLException e1) {
                }
        }
        return null;
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
        
        Properties env= PerformanceTestPlugin.getEnvironmentVariables();
        
        String dbloc= env.getProperty("dbloc");
        if (dbloc == null)
            return;
                
        String dbname= env.getProperty("dbname", DB_NAME);
        
        try {            
            if (dbloc.startsWith("net://")) {
                // connect over network
                if (DEBUG) System.out.println("Trying to connect over network...");
                Class.forName("com.ibm.db2.jcc.DB2Driver");
                String url="jdbc:cloudscape:" + dbloc + "/" + dbname + ";retrieveMessagesFromServerOnGetMessage=true;deferPrepares=true;";
                String dbuser= env.getProperty("dbuser", "guest");
                String dbpassword= env.getProperty("dbpasswd", "guest");
                fConnection= DriverManager.getConnection(url, dbuser, dbpassword);
            } else {
                // embedded
                if (DEBUG) System.out.println("Loading embedded cloudscape...");
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
            if (DEBUG) System.out.println("succeeded!");
 
            fSQL= new SQL(fConnection);

            doesDBexists();

            if (DEBUG) System.out.println("start prepared statements");
            fSQL.createPreparedStatements();
            if (DEBUG) System.out.println("finish with prepared statements");
                         
        } catch (SQLException ex) {
            ex.printStackTrace();
            System.err.print("SQLException: ");
            System.err.println(ex.getMessage());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
    
    private void disconnect() {
        if (DEBUG) System.out.println("disconnecting from DB");
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
	        if (DEBUG) System.out.println("initialising DB");
	        fSQL.initialize();
	        if (DEBUG) System.out.println("end initialising DB");
        } finally {
            stmt.close();
        }
    }
    
    //---- test --------
    
    public static void main(String args[]) throws Exception {
        
        DB db= DB.getDefault();
        
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
            
            ResultSet result= sql.query1("burano", "MacOS X", "3.1", "aFirstTest", Dimensions.USER_TIME.getId());
            for (int i= 0; result.next(); i++)
                System.out.println(i + ": " + result.getString(1));
            result.close();
             
        } catch (SQLException ex) {
            System.err.print("SQLException: ");
            System.err.println(ex.getMessage());
            ex.printStackTrace();
        }
    }
}
