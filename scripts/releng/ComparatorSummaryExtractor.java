
/*******************************************************************************
 *  Copyright (c) 2013, 2025 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Hannes Wellmann - Convert to plain Java scripts
 *******************************************************************************/

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import utilities.OS;

/**
 * This class is responsible for extracting the relevent "Debug" messages from
 * the huge maven debug log.
 *
 * @author davidw
 */
public class ComparatorSummaryExtractor {

	private static final String EOL = System.lineSeparator();

	public static void main(String[] args) throws IOException {
		ComparatorSummaryExtractor extractor = new ComparatorSummaryExtractor();
		extractor.buildDirectory = OS.readProperty("buildDirectory");
		extractor.comparatorRepo = OS.readProperty("comparatorRepo");
		extractor.processBuildfile();
	}

	private record LogEntry(String name, List<String> reasons, List<String> info) {

		static LogEntry create(String name) {
			return new LogEntry(name, new ArrayList<>(), new ArrayList<>());
		}

		public void addInfo(final String infoline) {
			info.add(infoline);
		}

		public void addReason(final String reason) {
			reasons.add(reason);
		}
	}

	private static final String BUILD_LOGS_DIRECTORY = "buildlogs";
	private static final String COMPARATOR_LOGS_DIRECTORY = "comparatorlogs";
	private String comparatorRepo = "comparatorRepo";
	private String buildDirectory;
	private static final Pattern MAIN_PATTERN = Pattern.compile(
			"^\\[WARNING\\].*eclipse.platform.releng.aggregator/(.*): baseline and build artifacts have same version but different contents");
	private static final Pattern NO_CLASSIFIER_PATTERN = Pattern.compile("^.*no-classifier:.*$");
	private static final Pattern CLASSIFIER_SOURCES_PATTERN = Pattern.compile("^.*classifier-sources:.*$");
	private static final Pattern CLASSIFIER_SOURCES_FEATURE_PATTERN = Pattern
			.compile("^.*classifier-sources-feature:.*$");

	private static final Pattern SIGN1_PATTERN = Pattern.compile("^.*META-INF/(ECLIPSE_|CODESIGN).RSA.*$");
	private static final Pattern SIGN2_PATTERN = Pattern.compile("^.*META-INF/(ECLIPSE_|CODESIGN).SF.*$");
	private static final Pattern DOC_NAME_PATTERN = Pattern.compile("^.*eclipse\\.platform\\.common.*\\.doc\\..*$");
	// jar pattern added for bug 416701
	private final Pattern JAR_PATTERN = Pattern.compile("^.*\\.jar.*$");
	private int count;
	private int countSign;
	private int countDoc;
	private int countOther;
	private int countSignPlusInnerJar;
	private int countJDTCore;

	private boolean docItem(final LogEntry newEntry) {
		boolean result = false;
		final String name = newEntry.name();
		final Matcher matcher = DOC_NAME_PATTERN.matcher(name);
		if (matcher.matches()) {
			result = true;
		}
		return result;
	}

	private String getInputFilename() {
		return buildDirectory + "/" + BUILD_LOGS_DIRECTORY + "/" + "mb220_buildSdkPatch.sh.log";
	}

	private String getOutputFilenameDoc() {
		return buildDirectory + "/" + BUILD_LOGS_DIRECTORY + "/" + COMPARATOR_LOGS_DIRECTORY + "/"
				+ "buildtimeComparatorDocBundle.log.txt";
	}

	private String getOutputFilenameFull() {
		return buildDirectory + "/" + BUILD_LOGS_DIRECTORY + "/" + COMPARATOR_LOGS_DIRECTORY + "/"
				+ "buildtimeComparatorFull.log.txt";
	}

	private String getOutputFilenameOther() {
		return buildDirectory + "/" + BUILD_LOGS_DIRECTORY + "/" + COMPARATOR_LOGS_DIRECTORY + "/"
				+ "buildtimeComparatorUnanticipated.log.txt";
	}

	private String getOutputFilenameSign() {
		return buildDirectory + "/" + BUILD_LOGS_DIRECTORY + "/" + COMPARATOR_LOGS_DIRECTORY + "/"
				+ "buildtimeComparatorSignatureOnly.log.txt";
	}

	private String getOutputFilenameSignWithInnerJar() {
		return buildDirectory + "/" + BUILD_LOGS_DIRECTORY + "/" + COMPARATOR_LOGS_DIRECTORY + "/"
				+ "buildtimeComparatorSignatureOnlyWithInnerJar.log.txt";
	}

	private String getOutputFilenameJDTCore() {
		return buildDirectory + "/" + BUILD_LOGS_DIRECTORY + "/" + COMPARATOR_LOGS_DIRECTORY + "/"
				+ "buildtimeComparatorJDTCore.log.txt";
	}

	public void processBuildfile() throws IOException {

		// Make sure directory exists
		File outputDir = new File(buildDirectory + "/" + BUILD_LOGS_DIRECTORY, COMPARATOR_LOGS_DIRECTORY);
		if (!outputDir.exists()) {
			outputDir.mkdirs();
		}

		final File infile = new File(getInputFilename());
		final File outfile = new File(getOutputFilenameFull());
		final File outfileSign = new File(getOutputFilenameSign());
		final File outfileDoc = new File(getOutputFilenameDoc());
		final File outfileOther = new File(getOutputFilenameOther());
		final File outfileSignWithInnerJar = new File(getOutputFilenameSignWithInnerJar());
		final File outfileJDTCore = new File(getOutputFilenameJDTCore());
		try (BufferedReader input = Files.newBufferedReader(infile.toPath());
				Writer out = new FileWriter(outfile);
				BufferedWriter output = new BufferedWriter(out);
				Writer outsign = new FileWriter(outfileSign);
				BufferedWriter outputSign = new BufferedWriter(outsign);
				Writer outdoc = new FileWriter(outfileDoc);
				BufferedWriter outputDoc = new BufferedWriter(outdoc);
				Writer outother = new FileWriter(outfileOther);
				BufferedWriter outputOther = new BufferedWriter(outother);
				Writer outsignWithJar = new FileWriter(outfileSignWithInnerJar);
				BufferedWriter outputSignWithJar = new BufferedWriter(outsignWithJar);
				Writer outJDTCore = new FileWriter(outfileJDTCore);
				BufferedWriter outputJDTCore = new BufferedWriter(outJDTCore);) {

			writeHeader(output);
			writeHeader(outputSign);
			writeHeader(outputSignWithJar);
			writeHeader(outputDoc);
			writeHeader(outputOther);
			writeHeader(outputJDTCore);
			count = 0;
			countSign = 0;
			countSignPlusInnerJar = 0;
			countDoc = 0;
			countOther = 0;
			countJDTCore = 0;
			String inputLine = "";

			while (inputLine != null) {
				inputLine = input.readLine();
				if (inputLine != null) {
					final Matcher matcher = MAIN_PATTERN.matcher(inputLine);
					if (matcher.matches()) {

						final LogEntry newEntry = LogEntry.create(matcher.group(1));
						// read and write differences, until next blank line
						do {
							inputLine = input.readLine();
							if ((inputLine != null) && (inputLine.length() > 0)) {
								newEntry.addReason(inputLine);
							}
						} while ((inputLine != null) && (inputLine.length() > 0));
						// //output.write(EOL);
						// now, do one more, to get the "info" that says
						// what was copied, or not.
						do {
							inputLine = input.readLine();
							if ((inputLine != null) && (inputLine.length() > 0)) {
								// except leave out the first line, which is a
								// long [INFO] line repeating what we already
								// know.
								if (!inputLine.startsWith("[INFO]")) {
									newEntry.addInfo(inputLine);
								}
							}
						} while ((inputLine != null) && (inputLine.length() > 0));
						// Write full log, for sanity check, if nothing else
						writeEntry(++count, output, newEntry);
						if (jdtCore(newEntry)) {
							writeEntry(++countJDTCore, outputJDTCore, newEntry);
						} else if (docItem(newEntry)) {
							writeEntry(++countDoc, outputDoc, newEntry);
						} else if (pureSignature(newEntry)) {
							writeEntry(++countSign, outputSign, newEntry);
						} else if (pureSignaturePlusInnerJar(newEntry)) {
							writeEntry(++countSignPlusInnerJar, outputSignWithJar, newEntry);
						} else {
							writeEntry(++countOther, outputOther, newEntry);
						}
					}
				}
			}
		}
	}

	private void writeHeader(final BufferedWriter output) throws IOException {
		output.write("Comparator differences from current build" + EOL);
		output.write("\t" + buildDirectory + EOL);
		output.write("compared to reference repo at " + EOL);
		output.write("\t" + comparatorRepo + EOL + EOL);
	}

	private boolean jdtCore(final LogEntry newEntry) {
		boolean result = false;
		final String name = newEntry.name();
		if (name.equals("eclipse.jdt.core/org.eclipse.jdt.core/pom.xml")) {
			result = true;
		}
		return result;
	}

	private boolean pureSignature(final LogEntry newEntry) {
		// if all lines match one of these critical patterns,
		// then assume "signature only" difference. If even
		// one of them does not match, assume not.
		boolean result = true;
		final List<String> reasons = newEntry.reasons();
		for (final String reason : reasons) {
			final Matcher matcher1 = NO_CLASSIFIER_PATTERN.matcher(reason);
			final Matcher matcher2 = CLASSIFIER_SOURCES_PATTERN.matcher(reason);
			final Matcher matcher3 = CLASSIFIER_SOURCES_FEATURE_PATTERN.matcher(reason);
			final Matcher matcher4 = SIGN1_PATTERN.matcher(reason);
			final Matcher matcher5 = SIGN2_PATTERN.matcher(reason);

			if (matcher1.matches() || matcher2.matches() || matcher3.matches() || matcher4.matches()
					|| matcher5.matches()) {
			} else {
				result = false;
				break;
			}
		}

		return result;
	}

	private boolean pureSignaturePlusInnerJar(final LogEntry newEntry) {
		// if all lines match one of these critical patterns,
		// then assume "signature only plus inner jar" difference. If even
		// one of them does not match, assume not.
		// TODO: refactor so less copy/paste of pureSignature method.
		boolean result = true;
		final List<String> reasons = newEntry.reasons();
		for (final String reason : reasons) {
			final Matcher matcher1 = NO_CLASSIFIER_PATTERN.matcher(reason);
			final Matcher matcher2 = CLASSIFIER_SOURCES_PATTERN.matcher(reason);
			final Matcher matcher3 = CLASSIFIER_SOURCES_FEATURE_PATTERN.matcher(reason);
			final Matcher matcher4 = SIGN1_PATTERN.matcher(reason);
			final Matcher matcher5 = SIGN2_PATTERN.matcher(reason);
			final Matcher matcher6 = JAR_PATTERN.matcher(reason);

			if (matcher1.matches() || matcher2.matches() || matcher3.matches() || matcher4.matches()
					|| matcher5.matches() || matcher6.matches()) {
			} else {
				result = false;
				break;
			}
		}

		return result;
	}

	private void writeEntry(int thistypeCount, final Writer output, final LogEntry newEntry) throws IOException {

		output.write(thistypeCount + ".  " + newEntry.name() + EOL);
		final List<String> reasons = newEntry.reasons();
		for (final String reason : reasons) {
			output.write(reason + EOL);
		}
		final List<String> infolist = newEntry.info();
		for (final String info : infolist) {
			output.write(info + EOL);
		}
		output.write(EOL);
	}
}
