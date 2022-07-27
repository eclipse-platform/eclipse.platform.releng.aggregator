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

public class BasicResultsTable implements IApplication{
    private static String CURRENT_BUILD, BASELINE_BUILD=null;
    private static ArrayList<Path> inputFiles = new ArrayList<>();
    private static Path phpTemplateFile = null;
    private static String EOL = System.lineSeparator();
    private static String buildDirectory = "";

    @Override
    public Object start(IApplicationContext context) {
        String[] args = (String[]) context.getArguments().get("application.args");
        //DEBUG
        if (args.length > 0) {
            System.out.println("\n\t= = Raw arguments ('application.args') passed to performance import application: = =");
            for (String arg : args) {
                System.out.println("\t\t>" + arg + "<");
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

    public static void run(String[] args) {

        parse(args);

        //Initialize results data
        ResultsData results = new ResultsData(CURRENT_BUILD, BASELINE_BUILD);
        try {
		    // import data
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
        //group scenarios by component for making tables
        HashMap<String, ArrayList<String>> componentMap = new HashMap<>();

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

        //for checking if a test has a baseline to reference
        Set<String> baselineScenarios = results.getBaselineScenarios();

        //Make component html files
        for (String component : usedComponents) {
            ArrayList<String> scenarioList = componentMap.get(component);
            scenarioList.sort(String::compareToIgnoreCase);

            //set up html string
            String htmlString = "";
            //htmlString = htmlString + EOL + "<h2>Performance of " + component + ": " + CURRENT_BUILD + " relative to " + BASELINE_BUILD + "</h2>" + EOL;
            //htmlString = htmlString + EOL + "<a href=\"performance.php?fp_type=0\">Back to global results</a>" + EOL;
            htmlString = htmlString + EOL + "<h3>All " + scenarioList.size() + " scenarios:</h3>" + EOL;
            htmlString = htmlString + EOL + "<p>Times are given in milliseconds.</p>" + EOL;
            htmlString = htmlString + "<table border=\"1\">" + EOL + "<tr>" + EOL;
            htmlString = htmlString + "<td><h4>Class</h4></td>" + EOL;
            htmlString = htmlString + "<td><h4>Name</h4></td>" + EOL;
            htmlString = htmlString + "<td><h4>Elapsed Process (Current)</h4></td>" + EOL;
            htmlString = htmlString + "<td><h4>Elapsed Process (Baseline)</h4></td>" + EOL;
            htmlString = htmlString + "<td><h4>Difference</h4></td>" + EOL;
            htmlString = htmlString + "<td><h4>CPU Time (Current)</h4></td>" + EOL;
            htmlString = htmlString + "<td><h4>CPU Time (Baseline)</h4>" + EOL;
            htmlString = htmlString + "<td><h4>Difference</h4>" + EOL;

            for (String scenario : scenarioList) {
                String[] scenarioParts = null;
                String componentClass = null;
                String componentName = null;
                //swt is different
                if (scenario.contains("swt")) {
                    scenarioParts = scenario.split("\\.");
                    componentName = scenarioParts[scenarioParts.length - 1];
                    for (int i=0; i < (scenarioParts.length - 1); i++ ) {
                        componentClass = componentClass + scenarioParts[i] + ".";
                    }
                    //trim final .
                    componentClass = componentClass.substring(0, componentClass.length()-1);
                } else {
                    scenarioParts = scenario.split("#");
                    componentClass = scenarioParts[0];
                    componentName = scenarioParts[1];
                }

                double[] currentData = results.getData("current", scenario);
                String elapsedCurrent = String.valueOf(currentData[0]);
                String cpuCurrent = String.valueOf(currentData[1]);

                String elapsedBaseline = "N/A";
                String cpuBaseline = "N/A";

                String elapsedPercent = "N/A";
                String cpuPercent = "N/A";

                String elapsedColor = "#4CE600";
                String cpuColor = "#4CE600";

                if (baselineScenarios.contains(scenario)) {
                    double[] baselineData = results.getData("baseline", scenario);

                    elapsedBaseline = String.valueOf(baselineData[0]);
                    cpuBaseline = String.valueOf(baselineData[1]);

                    double elapsedDifference = baselineData[0] - currentData[0];
                    double cpuDifference = baselineData[1] - currentData[1];

                    double elapsedPercentValue = Math.abs(elapsedDifference / baselineData[0]) * 100;
                    double cpuPercentValue = Math.abs(cpuDifference / baselineData[1]) * 100;

                    elapsedPercent = String.format("%.2f", elapsedPercentValue) + "%";
                    cpuPercent = String.format("%.2f", cpuPercentValue) + "%";

                    if (elapsedDifference < 0) {
                        elapsedColor = "D7191C";
                    }
                    if (cpuDifference < 0) {
                        cpuColor = "D7191C";
                    }

                }

                htmlString = htmlString + "<tr>" + EOL;
                htmlString = htmlString + "<td>" + componentClass + EOL;
                htmlString = htmlString + "<td>" + componentName + EOL;
                htmlString = htmlString + "<td>" + elapsedCurrent + EOL;
                htmlString = htmlString + "<td>" + elapsedBaseline + EOL;
                htmlString = htmlString + "<td bgcolor=\"" + elapsedColor + "\">" + elapsedPercent + EOL;
                htmlString = htmlString + "<td>" + cpuCurrent + EOL;
                htmlString = htmlString + "<td>" + cpuBaseline + EOL;
                htmlString = htmlString + "<td bgcolor=\"" + cpuColor + "\">" + cpuPercent + EOL;
            }

            htmlString = htmlString + "</table>" + EOL;

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

        //make basicResultsIndex.html file
        String htmlString = "";
        htmlString = htmlString + EOL;

        for (String component : usedComponents) {
            htmlString = htmlString + "<a href=\"./basicPerformance.php/" +
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
            continue;
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
