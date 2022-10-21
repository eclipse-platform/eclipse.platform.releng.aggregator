/*******************************************************************************
 * Copyright (c) 2022 Samantha Dawley and others.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: Samantha Dawley - initial API and implementation
 *******************************************************************************/
package org.eclipse.test.performance;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.test.internal.performance.data.ResultsData;

/**
 * @since 3.19
 */
public class BasicResultsTable implements IApplication{
    private static String CURRENT_BUILD, BASELINE_BUILD="";
    private static ArrayList<Path> inputFiles = new ArrayList<>();
    private static Path phpTemplateFile = null;
    private static String buildDirectory = "";

    //String formatting shorthand
    private static String EOL = System.lineSeparator();
    private static String T = "\t";
    private static String T2 = "\t\t";

    @Override
    public Object start(IApplicationContext context) {
        String[] args = (String[]) context.getArguments().get("application.args");
        //DEBUG
        if (args.length > 0) {
            System.out.println(EOL + T +"= = Raw arguments ('application.args') passed to performance import application: = =");
            for (String arg : args) {
                System.out.println(T2 + ">" + arg + "<");
            }
        }
        //Stuff
        run(args);
        return EXIT_OK;
    }

    @Override
    public void stop() {
        // Do nothing
    }

    /**
     * Reference for interpreting variable names:
     * For the test org.eclipse.jface.tests.performance.FastTreeTest.testAddFifty
     *   component = org.eclupse.jface
     *   class = FastTreeTest
     *   scenarioID = testAddFifty
     */
    public static void run(String[] args) {

        parse(args);

        //Import results data
        ResultsData results = new ResultsData(CURRENT_BUILD, BASELINE_BUILD);
        try {
      	    System.out.println("INFO: Start importing " + inputFiles.size() + " performance data files.");
      	    for (Path inputFile : inputFiles) {
			    results.importData(inputFile);
      	    }
        } catch (Exception ex) {
            System.out.println("Performance data import failed with exception!" + ex);
            System.exit(1);
        }

        //Sort all scenarios into components, then make html file per component
        Set<String> scenarioIDs = results.getCurrentScenarios();
        ArrayList<String> usedComponents = new ArrayList<>();
        HashMap<String, ArrayList<String>> componentMap = new HashMap<>(); //scenarios grouped by component

        //get components from scenario name, for simplicity I'm just grabing everthing before .test/.tests but eventually should be mapped to actual components
        for (String scenarioID : scenarioIDs) {
            String[] scenarioParts = scenarioID.split("\\.");
            String scenarioComponent = "";
            for (String part : scenarioParts) {
                if (part.equals("tests")) {
                    break;
                }
                //stw is different
                if (part.equals("test")) {
                    break;
                }
                scenarioComponent = scenarioComponent + part + ".";
            }
            //trim final .
            scenarioComponent = scenarioComponent.substring(0, scenarioComponent.length()-1);

             //check if component in used components list
            if (usedComponents.contains(scenarioComponent)) {
                //Update HashMap entry, add scenario to components list
                ArrayList<String> componentScenarios = componentMap.get(scenarioComponent);
                componentScenarios.add(scenarioID);
                componentMap.replace(scenarioComponent, componentScenarios);
            } else {
                //Add component to used components and make new entry into HashMap
                ArrayList<String> componentScenarios = new ArrayList<>();
                componentScenarios.add(scenarioID);
                componentMap.put(scenarioComponent, componentScenarios);
                usedComponents.add(scenarioComponent);
            }
        }

        createResultsTables(results, usedComponents, componentMap);
        createIndex(usedComponents);

        //copy basicPerformance.php from templatefiles
        String phpFileName = buildDirectory + "/basicPerformance.php";
        File phpFile = new File(phpFileName);

        try (OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(phpFile))){
            outputStream.write(Files.readAllBytes(phpTemplateFile));
        }
        catch (final FileNotFoundException ex) {
            System.err.println(EOL + "ERROR: File not found exception while writing: " + phpFile.getPath());
            System.exit(1);
        }
        catch (final IOException ex) {
            System.err.println(EOL + "ERROR: IOException writing: " + phpFile.getPath());
            System.exit(1);
        }

    }

    /**
     * Create basicResultsIndex.html file
     */
    private static void createIndex(ArrayList<String> usedComponents) {
        String htmlString = EOL;

        for (String component : usedComponents) {
            htmlString = htmlString + "<a href=\"./basicPerformance.php" +
                "?name=" + component +
                "&build=" + CURRENT_BUILD +
                "&baseline=" + BASELINE_BUILD +
                "\">" + component + "*</a><br>" + EOL;
        }

        //create file
        String outputFileName = buildDirectory + "/BasicResultsIndex.html";
        File outputFile = new File(outputFileName);

        try (OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(outputFile))){
            outputStream.write(htmlString.getBytes());
        }
        catch (final FileNotFoundException ex) {
            System.err.println("ERROR: File not found exception while writing: " + outputFile.getPath());
            System.exit(1);
        }
        catch (final IOException ex) {
            System.err.println("ERROR: IOException writing: " + outputFile.getPath());
            System.exit(1);
        }
    }

    /**
     * Create *_BasicResults.html files
     */
    private static void createResultsTables(ResultsData results, ArrayList<String> usedComponents, HashMap<String, ArrayList<String>> componentMap ) {
        //for checking if a test has a baseline to reference
        Set<String> baselineScenarios = results.getBaselineScenarios();

        //Make component html files
        for (String component : usedComponents) {
            ArrayList<String> scenarioList = componentMap.get(component);
            scenarioList.sort(String::compareToIgnoreCase);

            //Variables for aggregate data
            Double componentEPCurrent = 0.0;
            Double componentEPBaseline = 0.0;
            Double componentCPUCurrent = 0.0;
            Double componentCPUBaseline = 0.0;
            HashMap<String, Double[]> classMap = new HashMap<>();

            String scenarioTable = makeHeader(true);
            for (String scenario : scenarioList) {
                String[] scenarioClassName = {"", ""}; //class, name

                //swt is different
                if (scenario.contains("swt")) {
                    String[] scenarioParts = scenario.split("\\.");
                    scenarioClassName[1] = scenarioParts[scenarioParts.length - 1];
                    for (int i=0; i < (scenarioParts.length - 1); i++ ) {
                        scenarioClassName[0] = scenarioClassName[0] + scenarioParts[i] + ".";
                    }
                    //trim final .
                    scenarioClassName[0] = scenarioClassName[0].substring(0, scenarioClassName[0].length()-1);
                } else {
                    String[] scenarioParts = scenario.split("#");
                    scenarioClassName[0] = scenarioParts[0];
                    scenarioClassName[1] = scenarioParts[1];
                }

                Double[] currentData = results.getData("current", scenario);
                Double[] baselineData = {0.0, 0.0}; //0.0 in case no baseline

                if (baselineScenarios.contains(scenario)) {
                    baselineData = results.getData("baseline", scenario);

                    //only add times to aggregate totals if there's also a baseline, otherwise will inflate current times.
                    if (classMap.containsKey(scenarioClassName[0])) { //add data
                    //class
                        Double[] classData = classMap.get(scenarioClassName[0]);
                        Double classEPCurrent = classData[0] + currentData[0];
                        Double classCPUCurrent = classData[1] + currentData[1];
                        Double classEPBaseline = classData[2] + baselineData[0];
                        Double classCPUBaseline = classData[3] + baselineData[1];
                        Double[] newClassData = {classEPCurrent, classCPUCurrent, classEPBaseline, classCPUBaseline};
                        classMap.replace(scenarioClassName[0], newClassData);

                    //component
                        componentEPCurrent += currentData[0];
                        componentCPUCurrent += currentData[1];
                        componentEPBaseline += baselineData[0];
                        componentCPUBaseline += baselineData[1];
                    } else { //add to map
                    //class
                        Double[] classData = {currentData[0], currentData[1], baselineData[0], baselineData[1]}; //currentEP, currentCPU, baselineEP, baselineCPU
                        classMap.put(scenarioClassName[0], classData);
                    //component
                        componentEPCurrent += currentData[0];
                        componentCPUCurrent += currentData[1];
                        componentEPBaseline += baselineData[0];
                        componentCPUBaseline += baselineData[1];
                    }
                }

                String scenarioRow = makeTableRow(scenarioClassName, currentData, baselineData);
                scenarioTable += scenarioRow;
            }
            scenarioTable += "</table>" + EOL;

            //create class and component tables
            String componentTable = makeHeader(false);
            String componentRow = makeTableRow(new String[]{component, ""},
                new Double[]{componentEPCurrent, componentCPUCurrent},
                new Double[]{componentEPBaseline, componentCPUBaseline});
            componentTable += componentRow + "</table>" + EOL;

            String classTable = makeHeader(false);
            Set<String> classNames = classMap.keySet();
            for (String className : classNames) {
                Double[] classData = classMap.get(className);
                String classRow = makeTableRow(new String[]{className, ""},
                    new Double[]{classData[0], classData[1]},
                    new Double[]{classData[2], classData[3]});
                classTable += classRow;
            }
            classTable += "</table>" + EOL;

            String style = getStyleString(); //make pretty

            //assemble final html string
            String htmlString = style + EOL + "<p>Times are given in milliseconds.</p>" + EOL +
                "<h3>Total Component Time:</h3>" + EOL +
                componentTable + EOL +
                "<h3>Total Class Times:</h3>" + EOL +
                classTable + EOL +
                "<h3>All " + scenarioList.size() + " individual scenarios:</h3>" + EOL +
                scenarioTable + EOL;

            //create file
            String outputFileName = buildDirectory + "/" + component + "_BasicTable.html";
            File outputFile = new File(outputFileName);

            try (OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(outputFile))){
                outputStream.write(htmlString.getBytes());
            }
            catch (final FileNotFoundException ex) {
                System.err.println("ERROR: File not found exception while writing: " + outputFile.getPath());
                System.exit(1);
            }
            catch (final IOException ex) {
                System.err.println("ERROR: IOException writing: " + outputFile.getPath());
                System.exit(1);
            }
        }
    }

    private static String makeHeader(boolean scenario) {
        String htmlString = "<table cellpadding=\"5\" class=\"details\">" + EOL +
            T + "<tr>" + EOL +
            T2 + "<th>Class</th>" + EOL;
        if (scenario) {
            htmlString += T2 + "<th>Scenario</th>" + EOL;
        }
        htmlString = htmlString + T2 + "<th>Elapsed Process (Current)</th>" + EOL +
            T2 + "<th>Elapsed Process (Baseline)</th>" + EOL +
            T2 + "<th>Difference</th>" + EOL +
            T2 + "<th>CPU Time (Current)</th>" + EOL +
            T2 + "<th>CPU Time (Baseline)</th>" + EOL +
            T2 + "<th>Difference</th>" + EOL +
            T + "</tr>" + EOL;

        return htmlString;
    }

    private static String makeTableRow(String[] className, Double[] currentData, Double[] baselineData) {
        String elapsedCurrent = String.valueOf(currentData[0]);
        String cpuCurrent = String.valueOf(currentData[1]);

        //defaults
        String elapsedBaseline = "N/A";
        String cpuBaseline = "N/A";

        String elapsedPercent = "N/A";
        String cpuPercent = "N/A";

        String elapsedColor = "#4CE600";
        String cpuColor = "#4CE600";

        if (baselineData[0] != 0.0) { //if baseline data isn't 0
            elapsedBaseline = String.valueOf(baselineData[0]);
            cpuBaseline = String.valueOf(baselineData[1]);

            Double elapsedDifference = baselineData[0] - currentData[0];
            Double cpuDifference = baselineData[1] - currentData[1];

            Double elapsedPercentValue = Math.abs(elapsedDifference / baselineData[0]) * 100;
            Double cpuPercentValue = Math.abs(cpuDifference / baselineData[1]) * 100;

            elapsedPercent = String.format("%.2f", elapsedPercentValue) + "%";
            cpuPercent = String.format("%.2f", cpuPercentValue) + "%";

            if (elapsedDifference < 0) {
                elapsedColor = "D7191C";
            }
            if (cpuDifference < 0) {
                cpuColor = "D7191C";
            }
        }

        String htmlString = T + "<tr>" + EOL +
            T2 + "<td>" + className[0] + "</td>" + EOL;
        if (className[1] != "") {
            htmlString += T2 + "<td>" + className[1] + "</td>" + EOL;
        }
        htmlString += T2 + "<td>" + elapsedCurrent + "</td>" + EOL +
            T2 + "<td>" + elapsedBaseline + "</td>" + EOL +
            T2 + "<td bgcolor=\"" + elapsedColor + "\">" + elapsedPercent + "</td>" + EOL +
            T2 + "<td>" + cpuCurrent + "</td>" + EOL +
            T2 + "<td>" + cpuBaseline + "</td>" + EOL +
            T2 + "<td bgcolor=\"" + cpuColor + "\">" + cpuPercent + "</td>" + EOL +
            T + "</tr>" + EOL;

        return htmlString;
    }

    private static String getStyleString() {
        return "<style type=\"text/css\">" + EOL
                +
            T + "body {" + EOL +
            T2 + "font:normal verdana,arial,helvetica;" + EOL +
            T2 + "color:#000000;" + EOL +
            T + "}" + EOL +
            T + "table.details tr th{" + EOL +
            T2 + "font-weight: bold;" + EOL +
            T2 + "text-align:left;" + EOL +
            T2 + "background:#a6caf0;" + EOL +
            T + "}" + EOL +
            T + "table.details tr {" + EOL +
            T2 + "background:#eeeee0;" + EOL +
            T + "}" + EOL +
            T + "p {" + EOL +
            T2 + "margin-top:0.5em; margin-bottom:1.0em;" + EOL +
            T + "}" + EOL +
            T + "h3 {" + EOL +
            T2 + "margin-bottom: 0.5em; font: bold 115% verdana,arial,helvetica" + EOL +
            T + "}" + EOL +
            "</style>" + EOL;
    }

    //args = baseline, current build, input file array
    private static void parse(String[] args) {
        if (args.length == 0) {
            printUsage();
        }

        int i = 0;
        while (i < args.length) {
            String arg = args[i];
            if (arg.equals("-current")){
                CURRENT_BUILD = args[i+1];
                if (CURRENT_BUILD.startsWith("-")) {
                    System.out.println("Missing value for "+arg+" parameter");
				    printUsage();
                }
                i++;
                i++;
                continue;
            }
            if (arg.equals("-baseline")){
                BASELINE_BUILD = args[i+1];
                if (BASELINE_BUILD.startsWith("-")) {
                    System.out.println("Missing value for "+arg+" parameter");
				    printUsage();
                }
                i++;
                i++;
                continue;
            }
            if (arg.equals("-buildDirectory")){
                buildDirectory = args[i+1];
                if (buildDirectory.startsWith("-")) {
                    System.out.println("Missing value for "+arg+" parameter");
				    printUsage();
                }
                i++;
                i++;
                continue;
            }
            if (arg.equals("-phpFile")){
                String inputFile = args[i+1];

                if (inputFile.startsWith("-")) {
                    System.out.println("Missing value for "+arg+" parameter");
				    printUsage();
                }
                //check real file
                Path inputFilePath = Paths.get(inputFile);
                if (Files.isReadable(inputFilePath)) {
          		    phpTemplateFile = inputFilePath;
        	    } else {
          		    System.err.println("ERROR: invalid input argument. Cannot read file: " + inputFile);
        	    }

                i++;
                i++;
                continue;
            }
            if (arg.equals("-inputFiles")){
                for (int j=1; j < 5; j++) {
                    String inputFile = args[i+j];

                    if (inputFile.startsWith("-")) {
                        System.out.println("Missing value for "+arg+" parameter");
				        printUsage();
                    }
                    //check real file
                    //Path inputFilePath = Paths.get(buildDirectory + "/" + inputFile);
                    Path inputFilePath = Paths.get(inputFile);
                    if (Files.isReadable(inputFilePath)) {
          		        inputFiles.add(inputFilePath);
        	        } else {
          		        System.err.println("ERROR: invalid input argument. Cannot read file: " + inputFile);
        	        }
      	        }
                i = i+5;
                continue;
            }
            System.err.println("ERROR: Unrecognized argument (arg) found, with value of >" + arg + "<");
            i++;
        }

    }

    private static void printUsage() {
        System.out.println(
            "Usage:\n" +
            "-baseline: Build id for the baseline build.\n" +
            "-current: Build id for the current build.\n" +
            "-buildDirectory: Directory of performance.php file, usually /home/data/httpd/download.eclipse.org/eclipse/downloads/drops4/${BUILD_ID}/performance.\n" +
            "-phpFile: Location of the basicPerformance.php file, also known as template file.\n" +
            "-inputFiles: List of the dat files from which to extract performance data (will grab the next 4 args as filenames).\n"
            );
        System.exit(1);
    }
}
