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
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

import org.eclipse.test.internal.performance.Constants;
import org.eclipse.test.internal.performance.PerformanceTestPlugin;
import org.eclipse.test.internal.performance.data.DataPoint;
import org.eclipse.test.internal.performance.data.Dim;
import org.eclipse.test.internal.performance.data.Sample;
import org.eclipse.test.internal.performance.data.Scalar;

public class DB {
    
    public static final String DB_NAME= "perfDB";	// default db name //$NON-NLS-1$
    
    private static final boolean DEBUG= false;
    
    private static final String LOCALHOST= "localhost";  //$NON-NLS-1$
    
    private static DB fgDefault;
    
    private Connection fConnection;
    private SQL fSQL;
    private int fConfigID;
    private int fStoredSamples;
    private boolean fStoreCalled;
    private boolean fIsEmbedded;
    
    
    public static boolean store(Sample sample) {
        return getDefault().internalStore(sample);
    }
    
    public static DataPoint[] queryDataPoints(String configPattern, String buildPattern, String scenarioPattern, Dim[] dims) {
        return getDefault().internalQueryDataPoints(configPattern, buildPattern, scenarioPattern, dims);
    }
   
    public static Scenario[] queryScenarios(String configPattern, String buildPattern, String scenarioPattern) {
        String[] scenarios= getDefault().internalQueryScenarioNames(configPattern, scenarioPattern); // get all Scenario names
        Scenario[] tables= new Scenario[scenarios.length];
        for (int i= 0; i < scenarios.length; i++)
            tables[i]= new Scenario(configPattern, buildPattern, scenarios[i]);
        return tables;
    }

    public static String[] queryBuildNames(String configPattern, String buildPattern, String scenarioPattern) {
        return getDefault().internalQueryBuildNames(configPattern, buildPattern, scenarioPattern);
    }

    public static Connection getConnection() {
        return getDefault().fConnection;
    }
    
    public static void dump() {
        try {
            getDefault().internalDump();
        } catch (SQLException e) {
            e.printStackTrace();
        }
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
    
    private String getConfigName() {
        String configName= PerformanceTestPlugin.getEnvironment(Constants.CONFIG);
        if (configName != null)
            return configName;
        if (fIsEmbedded)
            return LOCALHOST;
        return PerformanceTestPlugin.getHostName();
    }
    
    private int getConfig() throws SQLException {
        if (fConfigID == 0) {

            String configName= getConfigName();
            String buildName= PerformanceTestPlugin.getEnvironment(Constants.BUILD);
            fConfigID= fSQL.createConfig(configName, buildName);
            
            String osname= System.getProperty("os.name"); //$NON-NLS-1$
            if (osname != null)
                fSQL.insertConfigProperty(fConfigID, "os.name", osname); //$NON-NLS-1$

            String osversion= System.getProperty("os.version"); //$NON-NLS-1$
            if (osversion != null)
                fSQL.insertConfigProperty(fConfigID, "os.version", osversion); //$NON-NLS-1$

            String arch= System.getProperty("os.arch"); //$NON-NLS-1$
            if (arch != null)
                fSQL.insertConfigProperty(fConfigID, "os.arch", arch); //$NON-NLS-1$

    		String version= System.getProperty("java.fullversion"); //$NON-NLS-1$
    		if (version == null)
    		    version= System.getProperty("java.runtime.version"); //$NON-NLS-1$
    	    if (version != null)
    	        fSQL.insertConfigProperty(fConfigID, "jvm", version); //$NON-NLS-1$
    	    
    	    String vmargs= System.getProperty("eclipse.vmargs"); //$NON-NLS-1$
    	    if (vmargs != null)
    	        fSQL.insertConfigProperty(fConfigID, "eclipse.vmargs", vmargs); //$NON-NLS-1$
    	    
    	    String commands= System.getProperty("eclipse.commands"); //$NON-NLS-1$
    	    if (commands != null) {
    	        if (commands.length() >= 1000)
    	            commands= commands.substring(0, 1000);
    	        fSQL.insertConfigProperty(fConfigID, "eclipse.commands", commands); //$NON-NLS-1$
    	    }
        }
        return fConfigID;
    }
        
    private boolean internalStore(Sample sample) {
        
        if (fSQL == null || sample == null)
            return false;
        
        fStoreCalled= true;
        
        //System.out.println("store started..."); //$NON-NLS-1$
	    try {
            int scenario_id= fSQL.getScenario(sample.getScenarioID());
            int sample_id= fSQL.createSample(getConfig(), scenario_id, null, new Timestamp(sample.getStartTime()));
            
            /*
            String[] propertyKeys= sample.getPropertyKeys();
            for (int i= 0; i < propertyKeys.length; i++) {
                String key= propertyKeys[i];
                fSQL.insertSampleProperty(sample_id, key, sample.getProperty(key));
            }
            */
            fStoredSamples++;
            
            //System.err.println(PerformanceTestPlugin.getBuildId());
            
            //long l= System.currentTimeMillis();
			DataPoint[] dataPoints= sample.getDataPoints();
			for (int i= 0; i < dataPoints.length; i++) {
			    DataPoint dp= dataPoints[i];
	            int datapoint_id= fSQL.createDataPoint(sample_id, i, dp.getStep());
			    Scalar[] scalars= dp.getScalars();		    
			    for (int j= 0; j < scalars.length; j++) {
			        Scalar scalar= scalars[j];
			        int dim_id= scalar.getDimension().getId();
			        long value= scalar.getMagnitude();					
					fSQL.insertScalar(datapoint_id, dim_id, value);
			    }
			}
			//System.err.println(System.currentTimeMillis()-l);
			fConnection.commit();
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        //System.out.println("store ended..."); //$NON-NLS-1$
        return true;
    }

    private DataPoint[] internalQueryDataPoints(String configPattern, String buildPattern, String scenario, Dim[] dims) {
        if (fSQL == null)
            return null;
 
        ResultSet result= null;
        try {
        	
        	if (configPattern == null)
        		configPattern= getConfigName();
        	
        	int[] dim_ids= null;
        	if (dims != null) {
        		dim_ids= new int[dims.length];
        		for (int i= 0; i < dims.length; i++)
        			dim_ids[i]= dims[i].getId();
        	}
        	if (dim_ids == null)
        		dim_ids= new int[0];
            result= fSQL.queryDataPoints(configPattern, buildPattern, scenario, dim_ids);
            
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
                	System.out.println(i + ": " + sample_id+','+datapoint_id +','+step+' '+ Dim.getDimension(dim_id).getName() + ' ' + value);                 //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
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
    
    /*
     * Returns array of build names
     */
    private String[] internalQueryScenarioNames(String configPattern, String scenarioPattern) {
        if (fSQL == null)
            return null;
        ResultSet result= null;
        try {        	
            result= fSQL.queryScenarios(configPattern, scenarioPattern);
            ArrayList scenarios= new ArrayList();
            for (int i= 0; result.next(); i++)
		        scenarios.add(result.getString(1));
            return (String[])scenarios.toArray(new String[scenarios.size()]);
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
    
    private String[] internalQueryBuildNames(String hostPattern, String buildPattern, String scenarioPattern) {
        if (fSQL == null)
            return null;
        ResultSet result= null;
        try {        	
            result= fSQL.queryBuildNames(hostPattern, buildPattern, scenarioPattern);
            ArrayList buildNames= new ArrayList();
            for (int i= 0; result.next(); i++)
                buildNames.add(result.getString(1));
            return (String[])buildNames.toArray(new String[buildNames.size()]);
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
        
        String dbloc= env.getProperty(Constants.DB_LOCATION);
        if (dbloc == null)
            return;
                
        String dbname= env.getProperty(Constants.DB_NAME, DB_NAME); //$NON-NLS-1$
        
        try {            
            if (dbloc.startsWith("net://")) { //$NON-NLS-1$
                // connect over network
                if (DEBUG) System.out.println("Trying to connect over network..."); //$NON-NLS-1$
                Class.forName("com.ibm.db2.jcc.DB2Driver"); //$NON-NLS-1$
                String url="jdbc:cloudscape:" + dbloc + "/" + dbname + ";create=true;retrieveMessagesFromServerOnGetMessage=true;";  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
                String dbuser= env.getProperty(Constants.DB_USER, "guest"); //$NON-NLS-1$
                String dbpassword= env.getProperty(Constants.DB_PASSWD, "guest"); //$NON-NLS-1$
                fConnection= DriverManager.getConnection(url, dbuser, dbpassword);
            } else {
                // embedded
                fIsEmbedded= true;
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
 
            fConnection.setAutoCommit(false);
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
                fConnection.commit();
            } catch (SQLException e) {
                e.printStackTrace();
            }
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
	        ResultSet rs= stmt.executeQuery("SELECT count(*) FROM sys.systables WHERE sys.systables.tablename NOT LIKE 'SYS%' "); //$NON-NLS-1$
	        while (rs.next())
	            if (rs.getInt(1) >= 6)
	                return;
	        if (DEBUG) System.out.println("initialising DB"); //$NON-NLS-1$
	        fSQL.initialize();
			fConnection.commit();
	        if (DEBUG) System.out.println("end initialising DB"); //$NON-NLS-1$
        } finally {
            stmt.close();
        }
    }
    
    private void internalDump() throws SQLException {
        if (fConnection == null)
            return;
        Statement stmt= fConnection.createStatement();
        try {
            System.out.println("CONFIG(ID, NAME, BUILD):"); //$NON-NLS-1$
	        ResultSet rs= stmt.executeQuery("SELECT ID, NAME, BUILD FROM CONFIG"); //$NON-NLS-1$
 	        while (rs.next()) {
	            System.out.print(" " + rs.getInt(1)); //$NON-NLS-1$
	            System.out.print(" " + rs.getString(2)); //$NON-NLS-1$
	            System.out.print(" " + rs.getString(3)); //$NON-NLS-1$
	            System.out.println();
	        }
            System.out.println();
            System.out.println("CONFIG_PROPERTIES(CONFIG_ID, NAME, VALUE):"); //$NON-NLS-1$
	        rs= stmt.executeQuery("SELECT CONFIG_ID, NAME, VALUE FROM CONFIG_PROPERTIES"); //$NON-NLS-1$
 	        while (rs.next()) {
	            System.out.print(" " + rs.getInt(1)); //$NON-NLS-1$
	            System.out.print(" " + rs.getString(2)); //$NON-NLS-1$
	            System.out.print(" " + rs.getString(3)); //$NON-NLS-1$
	            System.out.println();
	        }
            System.out.println();
            System.out.println("SCENARIO(ID, NAME):"); //$NON-NLS-1$
	        rs= stmt.executeQuery("SELECT ID, NAME FROM SCENARIO"); //$NON-NLS-1$
 	        while (rs.next()) {
	            System.out.print(" " + rs.getInt(1)); //$NON-NLS-1$
	            System.out.print(" " + rs.getString(2)); //$NON-NLS-1$
	            System.out.println();
	        }
            System.out.println();
            System.out.println("SAMPLE(ID, CONFIG_ID, SCENARIO_ID, VARIATION, STARTTIME):"); //$NON-NLS-1$
	        rs= stmt.executeQuery("SELECT ID, CONFIG_ID, SCENARIO_ID, VARIATION, STARTTIME FROM SAMPLE"); //$NON-NLS-1$
 	        while (rs.next()) {
	            System.out.print(" " + rs.getInt(1)); //$NON-NLS-1$
	            System.out.print(" " + rs.getInt(2)); //$NON-NLS-1$
	            System.out.print(" " + rs.getInt(3)); //$NON-NLS-1$
	            System.out.print(" " + rs.getInt(4)); //$NON-NLS-1$
	            System.out.print(" " + rs.getTimestamp(5).toString()); //$NON-NLS-1$
	            System.out.println();
	        }
            System.out.println();
            System.out.println("SAMPLE_PROPERTIES(CONFIG_ID, KEY_ID, VALUE):"); //$NON-NLS-1$
	        rs= stmt.executeQuery("SELECT SAMPLE_ID, NAME, VALUE FROM SAMPLE_PROPERTIES"); //$NON-NLS-1$
 	        while (rs.next()) {
	            System.out.print(" " + rs.getInt(1)); //$NON-NLS-1$
	            System.out.print(" " + rs.getString(2)); //$NON-NLS-1$
	            System.out.print(" " + rs.getString(3)); //$NON-NLS-1$
	            System.out.println();
	        }
            System.out.println();
            System.out.println("DATAPOINT(ID, SAMPLE_ID, SEQ, STEP):"); //$NON-NLS-1$
	        rs= stmt.executeQuery("SELECT ID, SAMPLE_ID, SEQ, STEP FROM DATAPOINT"); //$NON-NLS-1$
 	        while (rs.next()) {
	            System.out.print(" " + rs.getInt(1)); //$NON-NLS-1$
	            System.out.print(" " + rs.getInt(2)); //$NON-NLS-1$
	            System.out.print(" " + rs.getInt(3)); //$NON-NLS-1$
	            System.out.print(" " + rs.getInt(4)); //$NON-NLS-1$
	            System.out.println();
	        }
            System.out.println();
            System.out.println("SCALAR(DATAPOINT_ID, DIM_ID, VALUE):"); //$NON-NLS-1$
	        rs= stmt.executeQuery("SELECT DATAPOINT_ID, DIM_ID, VALUE FROM SCALAR"); //$NON-NLS-1$
 	        while (rs.next()) {
	            System.out.print(" " + rs.getInt(1)); //$NON-NLS-1$
	            System.out.print(" " + rs.getInt(2)); //$NON-NLS-1$
	            System.out.print(" " + rs.getBigDecimal(3).toString()); //$NON-NLS-1$
	            System.out.println();
	        }
            System.out.println();
        } finally {
            stmt.close();
        }
    }
    
    public static void main(String args[]) {
        DB.dump();
    }
}
