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
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.framework.Assert;

import org.eclipse.test.internal.performance.InternalPerformanceMeter;
import org.eclipse.test.internal.performance.PerformanceTestPlugin;
import org.eclipse.test.internal.performance.data.DataPoint;
import org.eclipse.test.internal.performance.data.Dim;
import org.eclipse.test.internal.performance.data.Sample;
import org.eclipse.test.internal.performance.data.Scalar;
import org.eclipse.test.performance.Dimension;

public class DB {
    
    private static final boolean DEBUG= false;
    private static final boolean AGGREGATE= true;
    
    // the two supported DB types
    private static final String DERBY= "derby"; //$NON-NLS-1$
    private static final String CLOUDSCAPE= "cloudscape"; //$NON-NLS-1$
        
    private static DB fgDefault;
    
    private Connection fConnection;
    private SQL fSQL;
    private int fStoredSamples;
    private boolean fStoreCalled;
    private boolean fIsEmbedded;
    private String fDBType;	// either "derby" or "cloudscape"
    
    
    // Datapaoints
    public static DataPoint[] queryDataPoints(Variations variations, String scenarioName, Set dims) {
        return getDefault().internalQueryDataPoints(variations, scenarioName, dims);
    }
   
    // Scenarios
    /**
     * Return all Scenarios that match the given config, build, and scenario name.
     * @param configName
     * @param buildPattern
     * @param scenarioPattern
     * @return array of scenarios
     * @deprecated Use queryScenarios(Variations variations, ...) instead
     */
    public static Scenario[] queryScenarios(String configName, String buildPattern, String scenarioPattern) {
        Variations variations= new Variations();
        variations.put(PerformanceTestPlugin.CONFIG, configName);       
        variations.put(PerformanceTestPlugin.BUILD, buildPattern);  
        return queryScenarios(variations, scenarioPattern, PerformanceTestPlugin.BUILD, null);
    }

    /**
     * @param configName
     * @param buildPatterns
     * @param scenarioPattern
     * @param dimensions
     * @return array of scenarios
     * @deprecated Use queryScenarios(Variations variations, ...) instead
     */
    public static Scenario[] queryScenarios(String configName, String[] buildPatterns, String scenarioPattern, Dim[] dimensions) {
        Variations variations= new Variations();
        variations.put(PerformanceTestPlugin.CONFIG, configName);       
        variations.put(PerformanceTestPlugin.BUILD, buildPatterns);  
        return queryScenarios(variations, scenarioPattern, PerformanceTestPlugin.BUILD, dimensions);
    }

    /**
     * @param configName
     * @param buildPatterns
     * @param scenarioName
     * @return Scenario
     * @deprecated Use queryScenarios(Variations variations, ...) instead
     */
    public static Scenario queryScenario(String configName, String[] buildPatterns, String scenarioName) {
        Variations variations= new Variations();
        variations.put(PerformanceTestPlugin.CONFIG, configName);
        variations.put(PerformanceTestPlugin.BUILD, buildPatterns);
        return new Scenario(scenarioName, variations, PerformanceTestPlugin.BUILD, null);
    }
    
    /**
     * Returns all Scenarios that match the given variation and scenario pattern.
     * Every Scenario returned contains a series of datapoints specified by the seriesKey.
     *      
     * For example to get the datapoints for 
     * For every Scenario only the specified Diemnsions are retrieved from the database.
     * @param variations
     * @param scenarioPattern
     * @param seriesKey
     * @param dimensions
     * @return array of scenarios or <code>null</code> if an error occured.
     */
    public static Scenario[] queryScenarios(Variations variations, String scenarioPattern, String seriesKey, Dim[] dimensions) {
        String[] scenarioNames= getDefault().internalQueryScenarioNames(variations, scenarioPattern); // get all Scenario names
        if (scenarioNames == null)
            return new Scenario[0];
        Scenario[] tables= new Scenario[scenarioNames.length];
        for (int i= 0; i < scenarioNames.length; i++)
            tables[i]= new Scenario(scenarioNames[i], variations, seriesKey, dimensions);
        return tables;
    }

    /**
     * Returns all summaries that match the given variation and scenario patterns.
     * If scenarioPattern is null, all summary scenarios are returned that are marked as "global".
     * If scenarioPattern is not null, it is used to filter the scenarios and only scenarios marked as "local" are returned.
     * @param variationPatterns
     * @param scenarioPattern
     * @return array of summaries or <code>null</code> if an error occured.
     */
    public static SummaryEntry[] querySummaries(Variations variationPatterns, String scenarioPattern) {
        return getDefault().internalQuerySummaries(variationPatterns, scenarioPattern);
    }

    /**
     * @param names
     * @param variationPatterns
     * @param scenarioPattern
     * @deprecated Use queryDistinctValues instead
     */
    public static void queryBuildNames(List names, Variations variationPatterns, String scenarioPattern) {
        getDefault().internalQueryDistinctValues(names, PerformanceTestPlugin.BUILD, variationPatterns, scenarioPattern);
    }

    public static void queryDistinctValues(List values, String key, Variations variationPatterns, String scenarioPattern) {
        getDefault().internalQueryDistinctValues(values, key, variationPatterns, scenarioPattern);
    }
 
    public static String[] querySeriesValues(String scenarioName, Variations v, String seriesKey) {
        return getDefault().internalQuerySeriesValues(v, scenarioName, seriesKey);
    }
    
    public static Scenario getScenarioSeries(String scenarioName, Variations v, String seriesKey, String startBuild, String endBuild, Dim[] dims) {
        
        v= (Variations) v.clone();
        v.put(seriesKey, new String[] { startBuild, endBuild });
        Scenario scenario= new Scenario(scenarioName, v, seriesKey, dims);
        TimeSeries ts= scenario.getTimeSeries(dims[0]);
        if (ts.getLength() < 2) {
            v.put(seriesKey, "%"); //$NON-NLS-1$
            String[] names= DB.querySeriesValues(scenarioName, v, seriesKey);
            if (names.length >= 2) {
                String start= findClosest(names, startBuild);
                String end= findClosest(names, endBuild);
                v.put(seriesKey, new String[] { start, end });
                scenario= new Scenario(scenarioName, v, seriesKey, dims);
            }
        }
        return scenario;
    }
    
    private static String findClosest(String[] names, String name) {
        for (int i= 0; i < names.length; i++)
            if (names[i].equals(name))
                return name;
            
        Pattern pattern= Pattern.compile("200[3-9][01][0-9][0-3][0-9]"); //$NON-NLS-1$
        Matcher matcher= pattern.matcher(name); //$NON-NLS-1$
        
        if (!matcher.find())
            return name;
            
        int x= Integer.parseInt(name.substring(matcher.start(), matcher.end()));
        int ix= -1;
        int mind= 0;
            
        for (int i= 0; i < names.length; i++) {
            matcher.reset(names[i]);
            if (matcher.find()) {
                int y= Integer.parseInt(names[i].substring(matcher.start(), matcher.end()));
                int d= Math.abs(y-x);
                if (ix < 0 || d < mind) {
                    mind= d;
                    ix= i;
                }
            }
         }
        
        if (ix >= 0)
            return names[ix];
        return name;
    }

    /**
     * Store the data contained in the given sample in the database.
     * The data is tagged with key/value pairs from variations.
     * @param variations used to tag the data in the database
     * @param sample the sample to store
     * @return returns true if data could be stored successfully
     */
    public static boolean store(Variations variations, Sample sample) {
        return getDefault().internalStore(variations, sample);
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

    synchronized static DB getDefault() {
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
   
    SQL getSQL() {
        return fSQL;
    }
    
    private boolean internalStore(Variations variations, Sample sample) {
        
        if (fSQL == null || sample == null)
            return false;
        
		DataPoint[] dataPoints= sample.getDataPoints();
	    int n= dataPoints.length;
		if (n <= 0)
		    return false;

		Scalar[] sc= null;
		long[] averages= null;
	    if (AGGREGATE) {

		    sc= dataPoints[0].getScalars();
		    averages= new long[sc.length];

			Set set= new HashSet();
			for (int j= 0; j < dataPoints.length; j++) {
			    DataPoint dp= dataPoints[j];
			    set.add(new Integer(dp.getStep()));
			}
			switch (set.size()) {
			case 2:	// BEFORE/AFTER pairs -> calculate deltas
				for (int i= 0; i < dataPoints.length-1; i+= 2) {
				    Scalar[] s1= dataPoints[i].getScalars();
				    Scalar[] s2= dataPoints[i+1].getScalars();
				    for (int j= 0; j < sc.length; j++)
				        averages[j] += s2[j].getMagnitude() - s1[j].getMagnitude();
				}
				n= n/2;
				break;
				
			case 1:	// single values
				for (int i= 0; i < dataPoints.length; i++) {
				    Scalar[] s= dataPoints[i].getScalars();
				    for (int j= 0; j < sc.length; j++)
				        averages[j] += s[j].getMagnitude();
				}
				break;
				
			default:	// not expected
	            PerformanceTestPlugin.logError("DB.internalStore: too many steps in DataPoint"); //$NON-NLS-1$
			    return false;
			}
	    }
		
		//System.out.println("store started..."); //$NON-NLS-1$
	    try {
            //long l= System.currentTimeMillis();
            int variation_id= fSQL.getVariations(variations);
            int scenario_id= fSQL.getScenario(sample.getScenarioID());
            if (sample.isSummary()) {
                boolean isGlobal= sample.isGlobal();
                Dimension[] summaryDimensions= sample.getSummaryDimensions();
                for (int i= 0; i < summaryDimensions.length; i++) {
                    Dimension dimension= summaryDimensions[i];
                    if (dimension instanceof Dim)
                        fSQL.createSummaryEntry(variation_id, scenario_id, ((Dim)dimension).getId(), isGlobal);
                }
                fSQL.setScenarioShortName(scenario_id, sample.getShortname());
            }
            int sample_id= fSQL.createSample(variation_id, scenario_id, new Timestamp(sample.getStartTime()));

            if (sc != null) {
	            int datapoint_id= fSQL.createDataPoint(sample_id, 0, InternalPerformanceMeter.AVERAGE);
				for (int k= 0; k < sc.length; k++) {
			        int dim_id= sc[k].getDimension().getId();
					fSQL.insertScalar(datapoint_id, dim_id, averages[k] / n);
				}
		    } else {
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
		    }
			
			fConnection.commit();
            fStoredSamples++;
            fStoreCalled= true;

			//System.err.println(System.currentTimeMillis()-l);

        } catch (SQLException e) {
            PerformanceTestPlugin.log(e);
            try {
                fConnection.rollback();
            } catch (SQLException e1) {
                PerformanceTestPlugin.log(e1);
            }
        }
        return true;
    }
    
    private DataPoint[] internalQueryDataPoints(Variations variations, String scenarioName, Set dimSet) {
        if (fSQL == null)
            return null;
        
        ResultSet rs= null;
        try {
            ArrayList dataPoints= new ArrayList(); 
            rs= fSQL.queryDataPoints(variations, scenarioName);
	        while (rs.next()) {
	            int datapoint_id= rs.getInt(1);
	            int step= rs.getInt(2);

	            HashMap map= new HashMap();      
	            ResultSet rs2= fSQL.queryScalars(datapoint_id);
		        while (rs2.next()) {
	                int dim_id= rs2.getInt(1);
	                long value= rs2.getBigDecimal(2).longValue();		            
	                Dim dim= Dim.getDimension(dim_id);
	                if (dim != null) {
	                    if (dimSet == null || dimSet.contains(dim))
	                        map.put(dim, new Scalar(dim, value));
	                }
		        }
		        if (map.size() > 0)
		            dataPoints.add(new DataPoint(step, map));
		        
	            rs2.close();
	        }			       
	        rs.close();
        	
            int n= dataPoints.size();
            if (DEBUG) System.out.println("query resulted in " + n + " datapoints from DB"); //$NON-NLS-1$ //$NON-NLS-2$
            return (DataPoint[])dataPoints.toArray(new DataPoint[n]);

        } catch (SQLException e) {
            PerformanceTestPlugin.log(e);

        } finally {
            if (rs != null)
                try {
                    rs.close();
                } catch (SQLException e1) {
                	// ignored
                }
        }
        return null;
    }
    
    /*
     * Returns array of scenario names matching the given pattern.
     */
    private String[] internalQueryScenarioNames(Variations variations, String scenarioPattern) {
        if (fSQL == null)
            return null;
        ResultSet result= null;
        try {
            result= fSQL.queryScenarios(variations, scenarioPattern);
            ArrayList scenarios= new ArrayList();
            for (int i= 0; result.next(); i++)
		        scenarios.add(result.getString(1));
            return (String[])scenarios.toArray(new String[scenarios.size()]);

        } catch (SQLException e) {
	        PerformanceTestPlugin.log(e);

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
     * 
     */
    private void internalQueryDistinctValues(List values, String seriesKey, Variations variations, String scenarioPattern) {
        if (fSQL == null)
            return;
        ResultSet result= null;
        try {        	
            result= fSQL.queryVariations(variations.toExactMatchString(), scenarioPattern);
            for (int i= 0; result.next(); i++) {
                Variations v= new Variations();
                v.parseDB(result.getString(1));
                String build= v.getProperty(seriesKey);
                if (build != null && !values.contains(build))
                    values.add(build);
            }
        } catch (SQLException e) {
	        PerformanceTestPlugin.log(e);

        } finally {
            if (result != null)
                try {
                    result.close();
                } catch (SQLException e1) {
                	// ignored
                }
        }
    }
    
    private SummaryEntry[] internalQuerySummaries(Variations variationPatterns, String scenarioPattern) {
        if (fSQL == null)
            return null;
        ResultSet result= null;
        try {
            List fingerprints= new ArrayList();
            ResultSet rs;
            if (scenarioPattern != null)
                rs= fSQL.querySummaryEntries(variationPatterns, scenarioPattern);
            else
                rs= fSQL.queryGlobalSummaryEntries(variationPatterns);
            while (rs.next()) {
                String scenarioName= rs.getString(1);
                String shortName= rs.getString(2);
                int dim_id= rs.getInt(3);
                boolean isGlobal= rs.getShort(4) == 1;
                fingerprints.add(new SummaryEntry(scenarioName, shortName, Dim.getDimension(dim_id), isGlobal));
            }
            return (SummaryEntry[])fingerprints.toArray(new SummaryEntry[fingerprints.size()]);
        } catch (SQLException e) {
	        PerformanceTestPlugin.log(e);

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
    
    private String[] internalQuerySeriesValues(Variations v, String scenarioName, String seriesKey) {
        
        boolean isCloned= false;
        
        String[] seriesPatterns= null;        
        Object object= v.get(seriesKey);
        if (object instanceof String[])
            seriesPatterns= (String[]) object;
        else if (object instanceof String)
            seriesPatterns= new String[] { (String) object };
        else
            Assert.assertTrue(false);
        
        ArrayList values= new ArrayList();
        for (int i= 0; i < seriesPatterns.length; i++) {
            if (seriesPatterns[i].indexOf('%') >= 0) {
                if (! isCloned) {
                    v= (Variations) v.clone();
                    isCloned= true;
                }
                v.put(seriesKey, seriesPatterns[i]);
                internalQueryDistinctValues(values, seriesKey, v, scenarioName);
            } else
                values.add(seriesPatterns[i]);
        }
        
        String[] names= (String[])values.toArray(new String[values.size()]);
        
        boolean sort= true;
        Pattern pattern= Pattern.compile("200[3-9][01][0-9][0-3][0-9]"); //$NON-NLS-1$
        final Matcher matcher= pattern.matcher(""); //$NON-NLS-1$
        for (int i= 0; i < names.length; i++) {
            matcher.reset(names[i]);
            if (! matcher.find()) {
                sort= false;
                break;
            }
        }
        if (sort) {
	        Arrays.sort(names,
	            new Comparator() {
	            	public int compare(Object o1, Object o2) {
	            	    String s1= (String)o1;
	            	    String s2= (String)o2;
	            	    
	            	    matcher.reset(s1);
	            	    if (matcher.find())
	            	        s1= s1.substring(matcher.start());

		            	matcher.reset(s2);
		            	if (matcher.find())
		            	    s2= s2.substring(matcher.start());

	            	    return s1.compareTo(s2);
	            	}
	        	}
	        );
        }
        return names;
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


        String dbloc= PerformanceTestPlugin.getDBLocation();
        if (dbloc == null)
            return;
                   
        String dbname= PerformanceTestPlugin.getDBName();
        String url= null;
        java.util.Properties info= new java.util.Properties();
        
        fDBType= DERBY;	// assume we are using Derby
        try {            
            if (dbloc.startsWith("net://")) { //$NON-NLS-1$
                // remote
                fIsEmbedded= false;
                // connect over network
                if (DEBUG) System.out.println("Trying to connect over network..."); //$NON-NLS-1$
                Class.forName("com.ibm.db2.jcc.DB2Driver"); //$NON-NLS-1$
                info.put("user", PerformanceTestPlugin.getDBUser());	//$NON-NLS-1$
                info.put("password", PerformanceTestPlugin.getDBPassword());	//$NON-NLS-1$
                info.put("retrieveMessagesFromServerOnGetMessage", "true"); //$NON-NLS-1$ //$NON-NLS-2$
                url= dbloc + "/" + dbname;  //$NON-NLS-1$//$NON-NLS-2$
            } else {
                
                // workaround for Derby issue: http://nagoya.apache.org/jira/browse/DERBY-1
                if ("Mac OS X".equals(System.getProperty("os.name")))  //$NON-NLS-1$//$NON-NLS-2$
                    System.setProperty("derby.storage.fileSyncTransactionLog", "true"); //$NON-NLS-1$ //$NON-NLS-2$

                // embedded
                fIsEmbedded= true;
                try {
                    Class.forName("org.apache.derby.jdbc.EmbeddedDriver"); //$NON-NLS-1$
                } catch (ClassNotFoundException e) {
                    Class.forName("com.ihost.cs.jdbc.CloudscapeDriver"); //$NON-NLS-1$
                    fDBType= CLOUDSCAPE;
                }
                if (DEBUG) System.out.println("Loaded embedded " + fDBType); //$NON-NLS-1$
                File f;
                if (dbloc.length() == 0) {
                    String user_home= System.getProperty("user.home"); //$NON-NLS-1$
                    if (user_home == null)
                        return;
                    f= new File(user_home, fDBType);
                } else
                    f= new File(dbloc);
                url= new File(f, dbname).getAbsolutePath();
            }
            info.put("create", "true"); //$NON-NLS-1$ //$NON-NLS-2$
            try {
                fConnection= DriverManager.getConnection("jdbc:" + fDBType + ":" + url, info); //$NON-NLS-1$ //$NON-NLS-2$
            } catch (SQLException e) {
                if (DERBY.equals(fDBType)) {
                    if (DEBUG) System.out.println("DriverManager.getConnection failed; retrying for cloudscape"); //$NON-NLS-1$
                    // try Cloudscape
                    fDBType= CLOUDSCAPE;
                    fConnection= DriverManager.getConnection("jdbc:" + fDBType + ":" + url, info); //$NON-NLS-1$ //$NON-NLS-2$
                } else
                    throw e;
            }
            if (DEBUG) System.out.println("connect succeeded!"); //$NON-NLS-1$
 
            fConnection.setAutoCommit(false);
            fSQL= new SQL(fConnection);            
            fConnection.commit();

        } catch (SQLException ex) {
            PerformanceTestPlugin.logError(ex.getMessage());

        } catch (ClassNotFoundException e) {
	        PerformanceTestPlugin.log(e);
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
    	        PerformanceTestPlugin.log(e1);
            }
            fSQL= null;
        }
        if (fConnection != null) {
            try {
                fConnection.commit();
            } catch (SQLException e) {
    	        PerformanceTestPlugin.log(e);
            }
            try {
                 fConnection.close();
            } catch (SQLException e) {
    	        PerformanceTestPlugin.log(e);
            }
            fConnection= null;
        }
        
        if (fIsEmbedded) {
	        try {
	            DriverManager.getConnection("jdbc:" + fDBType + ":;shutdown=true"); //$NON-NLS-1$ //$NON-NLS-2$
	        } catch (SQLException e) {
	            String message= e.getMessage();
	            if (message.indexOf("system shutdown.") < 0) //$NON-NLS-1$
	                e.printStackTrace();
	        }
        }
    }
}
