/*******************************************************************************
 * Copyright (c) 2000, 2014 IBM Corporation and others. All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.test.internal.performance.db;

import java.io.File;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.test.internal.performance.InternalPerformanceMeter;
import org.eclipse.test.internal.performance.PerformanceTestPlugin;
import org.eclipse.test.internal.performance.data.DataPoint;
import org.eclipse.test.internal.performance.data.Dim;
import org.eclipse.test.internal.performance.data.Sample;
import org.eclipse.test.internal.performance.data.Scalar;
import org.eclipse.test.internal.performance.eval.StatisticsSession;
import org.eclipse.test.performance.Dimension;
import org.eclipse.test.performance.Performance;
import org.junit.Assert;

public class DB {

    private static final boolean DEBUG      = false;
    private static final boolean INFO       = true;
    private static final boolean AGGREGATE  = true;

    // the two supported DB types
    private static final String  DERBY      = "derby";     //$NON-NLS-1$
    private static final String  CLOUDSCAPE = "cloudscape"; //$NON-NLS-1$

    private static DB            fgDefault;

    private static String findClosest(final String[] names, final String name) {
        for (int i = 0; i < names.length; i++) {
            if (names[i].equals(name)) {
                return name;
            }
        }

        final Pattern pattern = Pattern.compile("200[3-9][01][0-9][0-3][0-9]"); //$NON-NLS-1$
        final Matcher matcher = pattern.matcher(name);

        if (!matcher.find()) {
            return name;
        }

        final int x = Integer.parseInt(name.substring(matcher.start(), matcher.end()));
        int ix = -1;
        int mind = 0;

        for (int i = 0; i < names.length; i++) {
            matcher.reset(names[i]);
            if (matcher.find()) {
                final int y = Integer.parseInt(names[i].substring(matcher.start(), matcher.end()));
                final int d = Math.abs(y - x);
                if ((ix < 0) || (d < mind)) {
                    mind = d;
                    ix = i;
                }
            }
        }

        if (ix >= 0) {
            return names[ix];
        }
        return name;
    }

    public static Connection getConnection() {
        return getDefault().fConnection;
    }

    synchronized static DB getDefault() {
        if (fgDefault == null) {
            fgDefault = new DB();
            fgDefault.connect();
            if (PerformanceTestPlugin.getDefault() == null) {
                // not started as plugin
                Runtime.getRuntime().addShutdownHook(new Thread() {

                    public void run() {
                        shutdown();
                    }
                });
            }
        }
        return fgDefault;
    }

    public static Scenario getScenarioSeries(final String scenarioName, Variations v, final String seriesKey,
            final String startBuild, final String endBuild, final Dim[] dims) {
        v = (Variations) v.clone();
        v.put(seriesKey, new String[] { startBuild, endBuild });
        final Scenario.SharedState ss = new Scenario.SharedState(v, scenarioName, seriesKey, dims);
        Scenario scenario = new Scenario(scenarioName, ss);
        final TimeSeries ts = scenario.getTimeSeries(dims[0]);
        if (ts.getLength() < 2) {
            v.put(seriesKey, "%"); //$NON-NLS-1$
            final String[] names = DB.querySeriesValues(scenarioName, v, seriesKey);
            if (names.length >= 2) {
                final String start = findClosest(names, startBuild);
                final String end = findClosest(names, endBuild);
                v.put(seriesKey, new String[] { start, end });
                scenario = new Scenario(scenarioName, ss);
            }
        }
        return scenario;
    }

    public static boolean isActive() {
        return (fgDefault != null) && (fgDefault.getSQL() != null);
    }

    /**
     * @param variations
     *            used to tag the data in the database
     * @param sample
     *            the sample maked as failed
     * @param failMesg
     *            the reason of the failure
     */
    public static void markAsFailed(final Variations variations, final Sample sample, final String failMesg) {
        getDefault().internalMarkAsFailed(variations, sample, failMesg);
    }

    /**
     * @param names
     * @param variationPatterns
     * @param scenarioPattern
     * @deprecated Use queryDistinctValues instead
     */
    public static void queryBuildNames(final List names, final Variations variationPatterns, final String scenarioPattern) {
        getDefault().internalQueryDistinctValues(names, PerformanceTestPlugin.BUILD, variationPatterns, scenarioPattern);
    }

    // Datapaoints
    public static DataPoint[] queryDataPoints(final Variations variations, final String scenarioName, final Set dims) {
        return getDefault().internalQueryDataPoints(variations, scenarioName, dims);
    }

    public static void queryDistinctValues(final List values, final String key, final Variations variationPatterns,
            final String scenarioPattern) {
        getDefault().internalQueryDistinctValues(values, key, variationPatterns, scenarioPattern);
    }

    public static Map queryFailure(final String scenarioPattern, final Variations variations) {
        return getDefault().internalQueryFailure(scenarioPattern, variations);
    }

    /**
     * @param configName
     * @param buildPatterns
     * @param scenarioName
     * @return Scenario
     * @deprecated Use queryScenarios(Variations variations, ...) instead
     */
    public static Scenario queryScenario(final String configName, final String[] buildPatterns, final String scenarioName) {
        final Variations variations = new Variations();
        variations.put(PerformanceTestPlugin.CONFIG, configName);
        variations.put(PerformanceTestPlugin.BUILD, buildPatterns);
        return new Scenario(scenarioName, variations, PerformanceTestPlugin.BUILD, null);
    }

    // Scenarios
    /**
     * Return all Scenarios that match the given config, build, and scenario name.
     *
     * @param configName
     * @param buildPattern
     * @param scenarioPattern
     * @return array of scenarios
     * @deprecated Use queryScenarios(Variations variations, ...) instead
     */
    public static Scenario[] queryScenarios(final String configName, final String buildPattern, final String scenarioPattern) {
        final Variations variations = new Variations();
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
    public static Scenario[] queryScenarios(final String configName, final String[] buildPatterns, final String scenarioPattern,
            final Dim[] dimensions) {
        final Variations variations = new Variations();
        variations.put(PerformanceTestPlugin.CONFIG, configName);
        variations.put(PerformanceTestPlugin.BUILD, buildPatterns);
        return queryScenarios(variations, scenarioPattern, PerformanceTestPlugin.BUILD, dimensions);
    }

    /**
     * Returns all Scenarios that match the given variation and scenario pattern. Every Scenario returned contains a series of
     * datapoints specified by the seriesKey.
     *
     * For example to get the datapoints for For every Scenario only the specified Diemnsions are retrieved from the database.
     *
     * @param variations
     * @param scenarioPattern
     * @param seriesKey
     * @param dimensions
     * @return array of scenarios or <code>null</code> if an error occured.
     */
    public static Scenario[] queryScenarios(final Variations variations, final String scenarioPattern, final String seriesKey,
            final Dim[] dimensions) {
        // get all Scenario names
        final String[] scenarioNames = getDefault().internalQueryScenarioNames(variations, scenarioPattern);
        if (scenarioNames == null) {
            return new Scenario[0];
        }
        final Scenario.SharedState ss = new Scenario.SharedState(variations, scenarioPattern, seriesKey, dimensions);
        final Scenario[] tables = new Scenario[scenarioNames.length];
        for (int i = 0; i < scenarioNames.length; i++) {
            tables[i] = new Scenario(scenarioNames[i], ss);
        }
        return tables;
    }

    public static String[] querySeriesValues(final String scenarioName, final Variations v, final String seriesKey) {
        return getDefault().internalQuerySeriesValues(v, scenarioName, seriesKey);
    }

    /**
     * Returns all summaries that match the given variation and scenario patterns. If scenarioPattern is null, all summary scenarios
     * are returned that are marked as "global". If scenarioPattern is not null, it is used to filter the scenarios and only
     * scenarios marked as "local" are returned.
     *
     * @param variationPatterns
     * @param scenarioPattern
     * @return array of summaries or <code>null</code> if an error occured.
     */
    public static SummaryEntry[] querySummaries(final Variations variationPatterns, final String scenarioPattern) {
        return getDefault().internalQuerySummaries(variationPatterns, scenarioPattern);
    }

    public static void shutdown() {
        if (DEBUG) {
            System.out.println("DB.shutdown"); //$NON-NLS-1$
        }
        if (fgDefault != null) {
            fgDefault.disconnect();
            fgDefault = null;
        }
    }

    /**
     * Store the data contained in the given sample in the database. The data is tagged with key/value pairs from variations.
     *
     * @param variations
     *            used to tag the data in the database
     * @param sample
     *            the sample to store
     * @return returns true if data could be stored successfully
     */
    public static boolean store(final Variations variations, final Sample sample) {
        return getDefault().internalStore(variations, sample);
    }

    private Connection fConnection;

    private SQL        fSQL;

    private int        fStoredSamples;

    private boolean    fStoreCalled;

    // ---- private implementation

    private boolean    fIsEmbedded;

    /* fDBType is either "derby" or "cloudscape" */
    private String     fDBType;

    /**
     * Private constructor to block instance creation.
     */
    private DB() {
        // empty implementation
    }

    /**
     * dbloc= embed in home directory dbloc=/tmp/performance embed given location dbloc=net://localhost connect to local server
     * dbloc=net://www.eclipse.org connect to remove server
     */
    private void connect() {

        if (fConnection != null) {
            return;
        }

        if (DEBUG) {
            DriverManager.setLogWriter(new PrintWriter(System.out));
        }
        final String dbloc = PerformanceTestPlugin.getDBLocation();
        if (dbloc == null) {
            return;
        }

        final String dbname = PerformanceTestPlugin.getDBName();
        String url = null;
        final java.util.Properties info = new java.util.Properties();

        fDBType = DERBY; // assume we are using Derby
        try {
            if (dbloc.startsWith("net://")) { //$NON-NLS-1$
                // remote
                fIsEmbedded = false;
                // connect over network
                // connect over network
                final String driverName = "org.apache.derby.jdbc.ClientDriver"; //$NON-NLS-1$
                if (INFO) {
                    System.out.println("Trying to connect over network, with net:// protocol; " + driverName + " ..."); //$NON-NLS-1$ //$NON-NLS-2$
                }
                Class.forName(driverName);
                //Class.forName("com.ibm.db2.jcc.DB2Driver"); //$NON-NLS-1$
                info.put("user", PerformanceTestPlugin.getDBUser()); //$NON-NLS-1$
                info.put("password", PerformanceTestPlugin.getDBPassword()); //$NON-NLS-1$
                info.put("retrieveMessagesFromServerOnGetMessage", "true"); //$NON-NLS-1$ //$NON-NLS-2$
                url = dbloc + "/" + dbname + ";create=true"; //$NON-NLS-1$//$NON-NLS-2$
            } else if (dbloc.startsWith("//")) { //$NON-NLS-1$
                // remote
                fIsEmbedded = false;
                // connect over network
                final String driverName = "org.apache.derby.jdbc.ClientDriver"; //$NON-NLS-1$
                if (INFO) {
                    System.out
                            .println("Trying to connect over network with // jdbc protocol;  " + driverName + " to " + dbloc + '/' + dbname + " ..."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                }
                Class.forName(driverName);
                info.put("user", PerformanceTestPlugin.getDBUser()); //$NON-NLS-1$
                info.put("password", PerformanceTestPlugin.getDBPassword()); //$NON-NLS-1$
                info.put("create", "true"); //$NON-NLS-1$ //$NON-NLS-2$
                url = dbloc + '/' + dbname;
            } else {

                // workaround for Derby issue:
                // http://nagoya.apache.org/jira/browse/DERBY-1
                if ("Mac OS X".equals(System.getProperty("os.name"))) { //$NON-NLS-1$ //$NON-NLS-2$
                    System.setProperty("derby.storage.fileSyncTransactionLog", "true"); //$NON-NLS-1$ //$NON-NLS-2$
                }

                // embedded
                fIsEmbedded = true;
                try {
                    Class.forName("org.apache.derby.jdbc.EmbeddedDriver"); //$NON-NLS-1$
                }
                catch (final ClassNotFoundException e) {
                    Class.forName("com.ihost.cs.jdbc.CloudscapeDriver"); //$NON-NLS-1$
                    fDBType = CLOUDSCAPE;
                }
                if (DEBUG) {
                    System.out.println("Loaded embedded " + fDBType); //$NON-NLS-1$
                }
                File f;
                if (dbloc.length() == 0) {
                    final String user_home = System.getProperty("user.home"); //$NON-NLS-1$
                    if (user_home == null) {
                        return;
                    }
                    f = new File(user_home, fDBType);
                } else {
                    f = new File(dbloc);
                }
                url = new File(f, dbname).getAbsolutePath();
                info.put("user", PerformanceTestPlugin.getDBUser()); //$NON-NLS-1$
                info.put("password", PerformanceTestPlugin.getDBPassword()); //$NON-NLS-1$
                info.put("create", "true"); //$NON-NLS-1$ //$NON-NLS-2$
            }
            try {
                fConnection = DriverManager.getConnection("jdbc:" + fDBType + ":" + url, info); //$NON-NLS-1$ //$NON-NLS-2$
            }
            catch (final SQLException e) {
                System.out.println("SQLException: " + e); //$NON-NLS-1$
                if ("08001".equals(e.getSQLState()) && DERBY.equals(fDBType)) { //$NON-NLS-1$
                    if (DEBUG) {
                        System.out.println("DriverManager.getConnection failed; retrying for cloudscape"); //$NON-NLS-1$
                    }
                    // try Cloudscape
                    fDBType = CLOUDSCAPE;
                    fConnection = DriverManager.getConnection("jdbc:" + fDBType + ":" + url, info); //$NON-NLS-1$ //$NON-NLS-2$
                } else {
                    throw e;
                }
            }
            if (INFO) {
                System.out.println("connect succeeded!"); //$NON-NLS-1$
            }

            fConnection.setAutoCommit(false);
            fSQL = new SQL(fConnection);
            fConnection.commit();

        }
        catch (final SQLException ex) {
            PerformanceTestPlugin.logError(ex.getMessage());

        }
        catch (final ClassNotFoundException e) {
            PerformanceTestPlugin.log(e);
        }
    }

    private void disconnect() {
        if (INFO) {
            if (fStoreCalled) {
                System.out.println("stored " + fStoredSamples + " new datapoints in DB"); //$NON-NLS-1$ //$NON-NLS-2$
            } else {
                System.out.println("no new datapoints in DB"); //$NON-NLS-1$
            }
            System.out.println("disconnecting from DB"); //$NON-NLS-1$
        }
        if (fSQL != null) {
            try {
                fSQL.dispose();
            }
            catch (final SQLException e1) {
                PerformanceTestPlugin.log(e1);
            }
            fSQL = null;
        }
        if (fConnection != null) {
            try {
                fConnection.commit();
            }
            catch (final SQLException e) {
                PerformanceTestPlugin.log(e);
            }
            try {
                fConnection.close();
            }
            catch (final SQLException e) {
                PerformanceTestPlugin.log(e);
            }
            fConnection = null;
        }

        if (fIsEmbedded) {
            try {
                DriverManager.getConnection("jdbc:" + fDBType + ":;shutdown=true"); //$NON-NLS-1$ //$NON-NLS-2$
            }
            catch (final SQLException e) {
                final String message = e.getMessage();
                if (message.indexOf("system shutdown.") < 0) { //$NON-NLS-1$
                    e.printStackTrace();
                }
            }
        }
    }

    SQL getSQL() {
        return fSQL;
    }

    private void internalMarkAsFailed(final Variations variations, final Sample sample, final String failMesg) {

        if (fSQL == null) {
            return;
        }

        try {
            final int variation_id = fSQL.getVariations(variations);
            final int scenario_id = fSQL.getScenario(sample.getScenarioID());

            fSQL.insertFailure(variation_id, scenario_id, failMesg);

            fConnection.commit();

        }
        catch (final SQLException e) {
            PerformanceTestPlugin.log(e);
            try {
                fConnection.rollback();
            }
            catch (final SQLException e1) {
                PerformanceTestPlugin.log(e1);
            }
        }
    }

    private DataPoint[] internalQueryDataPoints(final Variations variations, final String scenarioName, final Set dimSet) {
        if (fSQL == null) {
            return null;
        }

        long start = System.currentTimeMillis();
        if (DEBUG) {
            System.out.print("	- query data points from DB for scenario " + scenarioName + "..."); //$NON-NLS-1$ //$NON-NLS-2$
        }
        ResultSet rs = null;
        try {
            final ArrayList dataPoints = new ArrayList();
            rs = fSQL.queryDataPoints(variations, scenarioName);
            if (DEBUG) {
                final long time = System.currentTimeMillis();
                System.out.println("done in " + (time - start) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$
                start = time;
            }
            while (rs.next()) {
                final int datapoint_id = rs.getInt(1);
                final int step = rs.getInt(2);

                final HashMap map = new HashMap();
                final ResultSet rs2 = fSQL.queryScalars(datapoint_id);
                while (rs2.next()) {
                    final int dim_id = rs2.getInt(1);
                    final long value = rs2.getBigDecimal(2).longValue();
                    final Dim dim = Dim.getDimension(dim_id);
                    if (dim != null) {
                        if ((dimSet == null) || dimSet.contains(dim)) {
                            map.put(dim, new Scalar(dim, value));
                        }
                    }
                }
                if (map.size() > 0) {
                    dataPoints.add(new DataPoint(step, map));
                }

                rs2.close();
            }
            rs.close();

            final int n = dataPoints.size();
            if (DEBUG) {
                final long time = System.currentTimeMillis();
                System.out.println("		+ " + n + " datapoints created in " + (time - start) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            }
            return (DataPoint[]) dataPoints.toArray(new DataPoint[n]);

        }
        catch (final SQLException e) {
            PerformanceTestPlugin.log(e);

        }
        finally {
            if (rs != null) {
                try {
                    rs.close();
                }
                catch (final SQLException e1) {
                    // ignored
                }
            }
        }
        return null;
    }

    /*
     *
     */
    private void internalQueryDistinctValues(final List values, final String seriesKey, final Variations variations,
            final String scenarioPattern) {
        if (fSQL == null) {
            return;
        }
        final long start = System.currentTimeMillis();
        if (DEBUG) {
            System.out.print("	- query distinct values from DB for scenario pattern '" + scenarioPattern + "'..."); //$NON-NLS-1$ //$NON-NLS-2$
        }
        ResultSet result = null;
        try {
            result = fSQL.queryVariations(variations.toExactMatchString(), scenarioPattern);
            while (result.next()) {
                final Variations v = new Variations();
                v.parseDB(result.getString(1));
                final String build = v.getProperty(seriesKey);
                if ((build != null) && !values.contains(build)) {
                    values.add(build);
                }
            }
        }
        catch (final SQLException e) {
            PerformanceTestPlugin.log(e);

        }
        finally {
            if (result != null) {
                try {
                    result.close();
                }
                catch (final SQLException e1) {
                    // ignored
                }
            }
            if (DEBUG) {
                final long time = System.currentTimeMillis();
                System.out.println("done in " + (time - start) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }
    }

    private Map internalQueryFailure(final String scenarioPattern, final Variations variations) {
        if (fSQL == null) {
            return null;
        }
        final long start = System.currentTimeMillis();
        if (DEBUG) {
            System.out.print("	- query failure from DB for scenario pattern '" + scenarioPattern + "'..."); //$NON-NLS-1$ //$NON-NLS-2$
        }
        ResultSet result = null;
        try {
            final Map map = new HashMap();
            result = fSQL.queryFailure(variations, scenarioPattern);
            while (result.next()) {
                final String scenario = result.getString(1);
                final String message = result.getString(2);
                map.put(scenario, message);
            }
            return map;
        }
        catch (final SQLException e) {
            PerformanceTestPlugin.log(e);

        }
        finally {
            if (result != null) {
                try {
                    result.close();
                }
                catch (final SQLException e1) {
                    // ignored
                }
            }
            if (DEBUG) {
                final long time = System.currentTimeMillis();
                System.out.println("done in " + (time - start) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }
        return null;
    }

    /*
     * Returns array of scenario names matching the given pattern.
     */
    private String[] internalQueryScenarioNames(final Variations variations, final String scenarioPattern) {
        if (fSQL == null) {
            return null;
        }
        final long start = System.currentTimeMillis();
        if (DEBUG) {
            System.out.print("	- query scenario names from DB for scenario pattern '" + scenarioPattern + "'..."); //$NON-NLS-1$ //$NON-NLS-2$
        }
        ResultSet result = null;
        try {
            result = fSQL.queryScenarios(variations, scenarioPattern);
            final ArrayList scenarios = new ArrayList();
            while (result.next()) {
                scenarios.add(result.getString(1));
            }
            return (String[]) scenarios.toArray(new String[scenarios.size()]);

        }
        catch (final SQLException e) {
            PerformanceTestPlugin.log(e);

        }
        finally {
            if (result != null) {
                try {
                    result.close();
                }
                catch (final SQLException e1) {
                    // ignored
                }
            }
            if (DEBUG) {
                final long time = System.currentTimeMillis();
                System.out.println("done in " + (time - start) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }
        return null;
    }

    private String[] internalQuerySeriesValues(Variations v, final String scenarioName, final String seriesKey) {

        boolean isCloned = false;

        String[] seriesPatterns = null;
        final Object object = v.get(seriesKey);
        if (object instanceof String[]) {
            seriesPatterns = (String[]) object;
        } else if (object instanceof String) {
            seriesPatterns = new String[] { (String) object };
        } else {
            Assert.assertTrue(false);
        }

        final ArrayList values = new ArrayList();
        for (int i = 0; i < seriesPatterns.length; i++) {
            if (seriesPatterns[i].indexOf('%') >= 0) {
                if (!isCloned) {
                    v = (Variations) v.clone();
                    isCloned = true;
                }
                v.put(seriesKey, seriesPatterns[i]);
                internalQueryDistinctValues(values, seriesKey, v, scenarioName);
            } else {
                values.add(seriesPatterns[i]);
            }
        }

        final String[] names = (String[]) values.toArray(new String[values.size()]);

        boolean sort = true;
        final Pattern pattern = Pattern.compile("20[0-9][3-9][01][0-9][0-3][0-9]"); //$NON-NLS-1$
        final Matcher matcher = pattern.matcher(""); //$NON-NLS-1$
        for (int i = 0; i < names.length; i++) {
            matcher.reset(names[i]);
            if (!matcher.find()) {
                sort = false;
                break;
            }
        }
        if (sort) {
            Arrays.sort(names, new Comparator() {

                public int compare(final Object o1, final Object o2) {
                    String s1 = (String) o1;
                    String s2 = (String) o2;

                    matcher.reset(s1);
                    if (matcher.find()) {
                        s1 = s1.substring(matcher.start());
                    }

                    matcher.reset(s2);
                    if (matcher.find()) {
                        s2 = s2.substring(matcher.start());
                    }

                    return s1.compareTo(s2);
                }
            });
        }
        return names;
    }

    private SummaryEntry[] internalQuerySummaries(final Variations variationPatterns, final String scenarioPattern) {
        if (fSQL == null) {
            return null;
        }
        final long start = System.currentTimeMillis();
        if (DEBUG) {
            System.out.print("	- query summaries from DB for scenario pattern '" + scenarioPattern + "'..."); //$NON-NLS-1$ //$NON-NLS-2$
        }
        ResultSet result = null;
        try {
            final List fingerprints = new ArrayList();
            if (scenarioPattern != null) {
                result = fSQL.querySummaryEntries(variationPatterns, scenarioPattern);
            } else {
                result = fSQL.queryGlobalSummaryEntries(variationPatterns);
            }
            while (result.next()) {
                final String scenarioName = result.getString(1);
                final String shortName = result.getString(2);
                final int dim_id = result.getInt(3);
                final boolean isGlobal = result.getShort(4) == 1;
                final int comment_id = result.getInt(5);
                int commentKind = 0;
                String comment = null;
                if (comment_id != 0) {
                    final ResultSet rs2 = fSQL.getComment(comment_id);
                    if (rs2.next()) {
                        commentKind = rs2.getInt(1);
                        comment = rs2.getString(2);
                    }
                }
                if (dim_id != 0) {
                    fingerprints.add(new SummaryEntry(scenarioName, shortName, Dim.getDimension(dim_id), isGlobal, commentKind,
                            comment));
                }
            }
            return (SummaryEntry[]) fingerprints.toArray(new SummaryEntry[fingerprints.size()]);
        }
        catch (final SQLException e) {
            PerformanceTestPlugin.log(e);
        }
        finally {
            if (result != null) {
                try {
                    result.close();
                }
                catch (final SQLException e1) {
                    // ignored
                }
            }
            if (DEBUG) {
                final long time = System.currentTimeMillis();
                System.out.println("done in " + (time - start) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }
        return null;
    }

    private boolean internalStore(final Variations variations, final Sample sample) {

        if ((fSQL == null) || (sample == null)) {
            return false;
        }

        final DataPoint[] dataPoints = sample.getDataPoints();
        final int n = dataPoints.length;
        if (n <= 0) {
            return false;
        }

        //System.out.println("store started..."); //$NON-NLS-1$
        try {
            // long l= System.currentTimeMillis();
            final int variation_id = fSQL.getVariations(variations);
            final int scenario_id = fSQL.getScenario(sample.getScenarioID());
            final String comment = sample.getComment();
            if (sample.isSummary()) {
                final boolean isGlobal = sample.isGlobal();

                int commentId = 0;
                final int commentKind = sample.getCommentType();
                if ((commentKind == Performance.EXPLAINS_DEGRADATION_COMMENT) && (comment != null)) {
                    commentId = fSQL.getCommentId(commentKind, comment);
                }

                final Dimension[] summaryDimensions = sample.getSummaryDimensions();
                for (int i = 0; i < summaryDimensions.length; i++) {
                    final Dimension dimension = summaryDimensions[i];
                    if (dimension instanceof Dim) {
                        fSQL.createSummaryEntry(variation_id, scenario_id, ((Dim) dimension).getId(), isGlobal, commentId);
                    }
                }
                final String shortName = sample.getShortname();
                if (shortName != null) {
                    fSQL.setScenarioShortName(scenario_id, shortName);
                }
            } else if (comment != null) {
                int commentId = 0;
                final int commentKind = sample.getCommentType();
                if (commentKind == Performance.EXPLAINS_DEGRADATION_COMMENT) {
                    commentId = fSQL.getCommentId(commentKind, comment);
                }
                fSQL.createSummaryEntry(variation_id, scenario_id, 0, false, commentId); // use
                // special
                // dim
                // id
                // '0'
                // to
                // identify
                // summary
                // entry
                // created
                // to
                // only
                // handle
                // a
                // comment
            }
            final int sample_id = fSQL.createSample(variation_id, scenario_id, new Timestamp(sample.getStartTime()));

            if (AGGREGATE) {
                final StatisticsSession stats = new StatisticsSession(dataPoints);
                final Dim[] dims = dataPoints[0].getDimensions();

                int datapoint_id = fSQL.createDataPoint(sample_id, 0, InternalPerformanceMeter.AVERAGE);
                for (int i = 0; i < dims.length; i++) {
                    final Dim dim = dims[i];
                    fSQL.insertScalar(datapoint_id, dim.getId(), (long) stats.getAverage(dim));
                }

                datapoint_id = fSQL.createDataPoint(sample_id, 0, InternalPerformanceMeter.STDEV);
                for (int i = 0; i < dims.length; i++) {
                    final Dim dim = dims[i];
                    // see StatisticsSession
                    final long value = Double.doubleToLongBits(stats.getStddev(dim));
                    fSQL.insertScalar(datapoint_id, dim.getId(), value);
                }

                datapoint_id = fSQL.createDataPoint(sample_id, 0, InternalPerformanceMeter.SIZE);
                for (int i = 0; i < dims.length; i++) {
                    final Dim dim = dims[i];
                    fSQL.insertScalar(datapoint_id, dim.getId(), stats.getCount(dim));
                }
            } else {
                for (int i = 0; i < dataPoints.length; i++) {
                    final DataPoint dp = dataPoints[i];
                    final int datapoint_id = fSQL.createDataPoint(sample_id, i, dp.getStep());
                    final Scalar[] scalars = dp.getScalars();
                    for (int j = 0; j < scalars.length; j++) {
                        final Scalar scalar = scalars[j];
                        final int dim_id = scalar.getDimension().getId();
                        final long value = scalar.getMagnitude();
                        fSQL.insertScalar(datapoint_id, dim_id, value);
                    }
                }
            }

            fConnection.commit();
            fStoredSamples++;
            fStoreCalled = true;

            // System.err.println(System.currentTimeMillis()-l);

        }
        catch (final SQLException e) {
            PerformanceTestPlugin.log(e);
            try {
                fConnection.rollback();
            }
            catch (final SQLException e1) {
                PerformanceTestPlugin.log(e1);
            }
        }
        return true;
    }
}
