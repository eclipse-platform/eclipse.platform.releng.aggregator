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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

import org.eclipse.test.internal.performance.data.DataPoint;
import org.eclipse.test.internal.performance.PerformanceTestPlugin;
import org.eclipse.test.internal.performance.data.Dim;
import org.eclipse.test.internal.performance.data.Sample;
import org.eclipse.test.internal.performance.data.Scalar;

public class DB {
    
    public static final String DB_NAME= "perfDB";	// default db name //$NON-NLS-1$
    
    private static final boolean DEBUG= false;
    private static final DateFormat DATE_FORMAT= new SimpleDateFormat();
    
    private static DB fgDefault;
    
    private Connection fConnection;
    private SQL fSQL;
    private int fTagID= -1;
    private int fConfigID;
    private int fStoredSamples;
    private boolean fStoreCalled;
    
    
    public static boolean store(Sample sample) {
        return getDefault().internalStore(sample);
    }
    
    public static DataPoint[] query(String refTag, String scenarioID, Dim[] dims) {
        return getDefault().internalQuery(refTag, scenarioID, dims);
    }
    
    public static Connection getConnection() {
        return getDefault().fConnection;
    }
    
    public static boolean isActive() {
        return fgDefault != null && fgDefault.getSQL() != null;
    }
    
    //---- private implementation
    
	/**
	 * Private constructor to block instance creation.
	 */
    private DB() {
        // empty implementation
    }

    private synchronized static DB getDefault() {
        if (fgDefault == null) {
            fgDefault= new DB();       
            fgDefault.connect();
            if (PerformanceTestPlugin.getDefault() == null) {
            	// not started as plugin
	            Runtime.getRuntime().addShutdownHook(
	                new Thread() {
	                    public void run() {
	                    	shutdown();
	                    }
	                }
	            );
            }
        }
        return fgDefault;
    }
    
    public static void shutdown() {
        if (DEBUG) System.out.println("DB.shutdown"); //$NON-NLS-1$
        if (fgDefault != null) {
            fgDefault.disconnect();
            fgDefault= null;
        }
    }
   
    private SQL getSQL() {
        return fSQL;
    }

    private int getTag() throws SQLException {
        if (fTagID < 0) {
            String tag= PerformanceTestPlugin.getEnvironment("setTag"); //$NON-NLS-1$
            if (tag != null)
                fTagID= fSQL.getTag(tag);
            else
                fTagID= 0;
        }
        return fTagID;
    }
    
    private int getConfig() throws SQLException {
        if (fConfigID == 0) {
            String name= System.getProperty("os.name", "?"); //$NON-NLS-1$ //$NON-NLS-2$
            String version= System.getProperty("os.version", "?"); //$NON-NLS-1$ //$NON-NLS-2$
            String arch= System.getProperty("os.arch", "?"); //$NON-NLS-1$ //$NON-NLS-2$
            String conf= name + '/' + version + '/' + arch;
            fConfigID= fSQL.getConfig("localhost", conf); //$NON-NLS-1$
        }
        return fConfigID;
    }
    
    private boolean internalStore(Sample sample) {
        
        if (fSQL == null || sample == null)
            return false;
        
        fStoreCalled= true;
        
        //System.out.println("store started..."); //$NON-NLS-1$
	    try {
	        int tag_id= getTag();
            int scenario_id= fSQL.getScenario(sample.getScenarioID());
            int sample_id= fSQL.createSample(getConfig(), scenario_id, tag_id, sample.getStartTime());
            fStoredSamples++;
            
			DataPoint[] dataPoints= sample.getDataPoints();
			for (int i= 0; i < dataPoints.length; i++) {
		        // System.out.println(" dp " + i); //$NON-NLS-1$ //$NON-NLS-2$
			    DataPoint dp= dataPoints[i];
	            int datapoint_id= fSQL.createDataPoint(sample_id, i, dp.getStep());
			    Scalar[] scalars= dp.getScalars();
				PreparedStatement is= fSQL.getScalarInsertStatement(scalars.length);
				int col= 1;
			    for (int j= 0; j < scalars.length; j++) {
			        Scalar scalar= scalars[j];
			        int id= scalar.getDimension().getId();
			        long value= scalar.getMagnitude();
					is.setInt(col, datapoint_id);
					is.setInt(col+1, id);
					is.setLong(col+2, value);
					col+= 3;
                }
			    SQL.create(is);
			}
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        //System.out.println("store ended..."); //$NON-NLS-1$
        return true;
    }

    private DataPoint[] internalQuery(String refTag, String scenarioID, Dim[] dims) {
        if (fSQL == null)
            return null;
 
        ResultSet result= null;
        try {
        	
        	int[] dim_ids= null;
        	if (dims != null) {
        		dim_ids= new int[dims.length];
        		for (int i= 0; i < dims.length; i++)
        			dim_ids[i]= dims[i].getId();
        	}
        	if (dim_ids == null)
        		dim_ids= new int[0];
            result= fSQL.query(getConfig(), refTag, scenarioID, dim_ids);
            
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
                Dim dim= Dim.getDimension(dim_id);
                if (dim != null)
                    map.put(dim, new Scalar(dim, value));
                
                if (DEBUG)
                	System.out.println(i + ": " + sample_id+','+datapoint_id +','+step+' '+ Dim.getDimension(dim_id).getName() + ' ' + value + ' ' + DATE_FORMAT.format(d));                 //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
            }
            int n= dataPoints.size();
            if (DEBUG) System.out.println("query resulted in " + n + " datapoints from DB"); //$NON-NLS-1$ //$NON-NLS-2$
            return (DataPoint[])dataPoints.toArray(new DataPoint[n]);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (result != null)
                try {
                    result.close();
                } catch (SQLException e1) {
                	// ignored
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
        
        String dbloc= env.getProperty("dbloc"); //$NON-NLS-1$
        if (dbloc == null)
            return;
                
        String dbname= env.getProperty("dbname", DB_NAME); //$NON-NLS-1$
        
        try {            
            if (dbloc.startsWith("net://")) { //$NON-NLS-1$
                // connect over network
                if (DEBUG) System.out.println("Trying to connect over network..."); //$NON-NLS-1$
                Class.forName("com.ibm.db2.jcc.DB2Driver"); //$NON-NLS-1$
                String url="jdbc:cloudscape:" + dbloc + "/" + dbname + ";create=true;retrieveMessagesFromServerOnGetMessage=true;deferPrepares=true;";  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
                String dbuser= env.getProperty("dbuser", "guest"); //$NON-NLS-1$ //$NON-NLS-2$
                String dbpassword= env.getProperty("dbpasswd", "guest"); //$NON-NLS-1$ //$NON-NLS-2$
                fConnection= DriverManager.getConnection(url, dbuser, dbpassword);
            } else {
                // embedded
                if (DEBUG) System.out.println("Loading embedded cloudscape..."); //$NON-NLS-1$
                Class.forName("com.ihost.cs.jdbc.CloudscapeDriver"); //$NON-NLS-1$
                File f;
                if (dbloc.length() == 0) {
                    String user_home= System.getProperty("user.home"); //$NON-NLS-1$
                    if (user_home == null)
                        return;
                    f= new File(user_home, "cloudscape"); //$NON-NLS-1$
                } else
                    f= new File(dbloc);
                String dbpath= new File(f, dbname).getAbsolutePath();
                String url= "jdbc:cloudscape:" + dbpath + ";create=true"; //$NON-NLS-1$ //$NON-NLS-2$
                fConnection= DriverManager.getConnection(url);
            }
            if (DEBUG) System.out.println("succeeded!"); //$NON-NLS-1$
 
            fSQL= new SQL(fConnection);

            doesDBexists();

            if (DEBUG) System.out.println("start prepared statements"); //$NON-NLS-1$
            fSQL.createPreparedStatements();
            if (DEBUG) System.out.println("finish with prepared statements"); //$NON-NLS-1$
                         
        } catch (SQLException ex) {
            ex.printStackTrace();
            System.err.print("SQLException: "); //$NON-NLS-1$
            System.err.println(ex.getMessage());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
    
    private void disconnect() {
        if (DEBUG && fStoreCalled)
            System.out.println("stored " + fStoredSamples + " new datapoints in DB"); //$NON-NLS-1$ //$NON-NLS-2$
        if (DEBUG) System.out.println("disconnecting from DB"); //$NON-NLS-1$
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
	        ResultSet rs= stmt.executeQuery("select count(*) from sys.systables"); //$NON-NLS-1$
	        while (rs.next())
	            if (rs.getInt(1) > 16)
	                return;
	        if (DEBUG) System.out.println("initialising DB"); //$NON-NLS-1$
	        fSQL.initialize();
	        if (DEBUG) System.out.println("end initialising DB"); //$NON-NLS-1$
        } finally {
            stmt.close();
        }
    }
}
