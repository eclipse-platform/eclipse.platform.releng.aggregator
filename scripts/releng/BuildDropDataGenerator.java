
/*******************************************************************************
 *  Copyright (c) 2025, 2025 Hannes Wellmann and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     Hannes Wellmann - initial API and implementation
 *******************************************************************************/

import java.util.Map.Entry;

import utilities.JSON;
import utilities.JSON.Object;
import utilities.OS;
import utilities.OS.FileInfo;

Path DIRECTORY;

/**
 * Considered JVM properties are:
 * <ul>
 * <li>{@code directory} (required) -- path to the drop directory root.</li>
 * <li>{@code gitBaselineTag} (required for {@code mainEclipse}) -- the Git
 * baseline tag.</li>
 * <li>{@code gitReposChanged} (required for {@code mainEclipse}) -- The comma
 * separated list of URLs of Git repositories that changed since the
 * baseline.</li>
 * </ul>
 * The generated data files (mainly JSON) are written at locations within the
 * specified directory where the website pages expect them.
 */
void main(String[] args) throws IOException {
	DIRECTORY = Path.of(OS.readProperty("dropDirectory")).toRealPath();
	IO.println("INFO: Generating drop data file for");
	IO.println("\t" + DIRECTORY);

	switch (args[0]) {
	case "mainEclipse" -> mainEclipsePageData();
	case "mainEquinox" -> mainEquinoxPageData();
	case "buildLogs" -> buildLogsPageData();
	default -> throw new IllegalArgumentException("Unexpected value: " + args[0]);
	}
}

final Pattern COMMA = Pattern.compile(",");
final String FILENAME = "name";
final String FILE_SIZE = "size";
final String FILE_HASH_TYPE = "sha512";

void mainEclipsePageData() throws IOException {
	String gitBaselineTag = OS.readProperty("gitBaselineTag").trim();
	List<String> gitReposChanged = List.of(OS.readProperty("gitReposChanged").trim().split(","));

	Map<Path, FileInfo> files = OS.listFileTree(DIRECTORY, 1);
	Map<String, String> properties = OS.loadProperties(DIRECTORY.resolve("buildproperties.properties"));
	String buildId = properties.get("BUILD_ID");
	ZonedDateTime buildDate = buildTimestamp(buildId);

	JSON.Object buildProperties = JSON.Object.create();
	// basic data
	buildProperties.add("identifier", JSON.String.create(buildId));
	buildProperties.add("label", JSON.String.create(buildId));
	buildProperties.add("kind", JSON.String.create(properties.get("BUILD_TYPE_NAME")));
	buildProperties.add("release", JSON.String.create(properties.get("STREAM")));
	buildProperties.add("releaseShort", JSON.String.create(properties.get("RELEASE_VER")));
	buildProperties.add("previousReleaseAPILabel", JSON.String.create(previousReleaseAPILabel(properties)));
	buildProperties.add("timestamp", JSON.String.create(buildDate.toString()));

	// git log
	buildProperties.add("gitTag", JSON.String.create(buildId));
	buildProperties.add("gitBaselineTag", JSON.String.create(gitBaselineTag));
	if (!gitBaselineTag.isEmpty()) {
		buildProperties.add("gitReposChanged",
				gitReposChanged.stream().map(JSON.String::create).collect(JSON.Array.toJSONArray()));
	} else {
		IO.println("Git log not generated because a reasonable previous tag could not be found.");
	}

	// tests
	JSON.Array testConfigurations = COMMA.splitAsStream(properties.get("TEST_CONFIGURATIONS_EXPECTED")).map(c -> {
		var config = c.split("-");
		return config[2] + "-" + config[3] + "-" + config[4].substring(0, config[4].indexOf('_'));
	}).map(JSON.String::new).collect(JSON.Array.toJSONArray());
	buildProperties.add("expectedTests", testConfigurations);

	// files
	buildProperties.add("p2Repository", JSON.Array.create(//
			createFileEntry("repository-" + buildId + ".zip", files, "All")));

	buildProperties.add("sdkProducts",
			collectFileEntries(files, filename -> filename.startsWith("eclipse-SDK-") && !OS.isMacTarGZ(filename)));

	buildProperties.add("eclipseTests", JSON.Array.create( //
			createFileEntry("eclipse-Automated-Tests-" + buildId + ".zip", files, "All")));

	JSON.Array platformProducts = collectFileEntries(files,
			filename -> filename.startsWith("eclipse-platform-") && !OS.isMacTarGZ(filename));
	platformProducts.add(createFileEntry( // Add separate entry for eclipse-platform product p2-repository
			"org.eclipse.platform-" + buildId + ".zip", files, "Platform Runtime Repo"));
	buildProperties.add("platformProducts", platformProducts);

	buildProperties.add("jdtCompiler", JSON.Array.create( //
			createFileEntry("ecj-" + buildId + ".jar", files, "All"),
			createFileEntry("ecjsrc-" + buildId + ".jar", files, "All")));

	buildProperties.add("swtBinaries", collectFileEntries(files, filename -> filename.startsWith("swt-")));

	writeChecksumsSummaryFile(buildProperties, buildId, "eclipse");

	Path file = DIRECTORY.resolve("buildproperties.json");
	IO.println("Write Eclipse drop main data to: " + file);
	JSON.write(buildProperties, file);
}

void mainEquinoxPageData() throws IOException {
	Map<Path, FileInfo> files = OS.listFileTree(DIRECTORY, 1);
	Map<String, String> properties = OS.loadProperties(DIRECTORY.resolve("buildproperties.properties"));
	String buildId = properties.get("BUILD_ID");
	ZonedDateTime buildDate = buildTimestamp(buildId);

	JSON.Object buildProperties = JSON.Object.create();
	// basic data
	buildProperties.add("identifier", JSON.String.create(buildId));
	buildProperties.add("label", JSON.String.create(buildId));
	buildProperties.add("kind", JSON.String.create(properties.get("BUILD_TYPE_NAME")));
	buildProperties.add("timestamp", JSON.String.create(buildDate.toString()));

	// files
	buildProperties.add("equinoxRepository",
			collectFileEntries(files, filename -> filename.startsWith("equinox-SDK-")));

	buildProperties.add("equinoxFramework",
			collectFileEntries(files, filename -> filename.startsWith("org.eclipse.osgi_")));

	Predicate<String> isAddonBundle = filename -> (filename.startsWith("org.eclipse.equinox.")
			|| filename.startsWith("org.eclipse.osgi.")) && !filename.startsWith("org.eclipse.equinox.p2.");
	Predicate<String> isJar = filename -> filename.endsWith(".jar");

	buildProperties.add("addonBundles", collectFileEntries(files, isAddonBundle.and(isJar)));
	buildProperties.add("otherBundles", collectFileEntries(files, isAddonBundle.negate().and(isJar)));

	buildProperties.add("launchers", collectFileEntries(files, filename -> filename.startsWith("launchers-")));

	buildProperties.add("starterKits", collectFileEntries(files,
			filename -> filename.startsWith("EclipseRT-OSGi-StarterKit-") && !OS.isMacTarGZ(filename)));

	writeChecksumsSummaryFile(buildProperties, buildId, "equinox");

	Path file = DIRECTORY.resolve("buildproperties.json");
	IO.println("Write Equinox drop main data to: " + file);
	JSON.write(buildProperties, file);
}

void buildLogsPageData() throws IOException {
	Path comparatorLogsDirectory = Path.of("comparatorlogs");
	Map<Path, FileInfo> files = OS.listFileTree(DIRECTORY.resolve("buildlogs"), 2);

	JSON.Object logFiles = JSON.Object.create();
	JSON.Array buildLogs = collectFileEntries(files, filename -> filename.startsWith("s"));
	logFiles.add("build", buildLogs);
	JSON.Array comparatorLogs = collectFilesInDirectory(files, comparatorLogsDirectory, _ -> true);
	logFiles.add("comparator", comparatorLogs);

	Path file = DIRECTORY.resolve("buildlogs/logs.json");
	IO.println("Write RelEng logs data to: " + file);
	JSON.write(logFiles, file);
}

// --- data conversion/collection ---

String previousReleaseAPILabel(Map<String, String> buildProps) {
	int major = Integer.parseInt(buildProps.get("STREAMMajor"));
	int minor = Integer.parseInt(buildProps.get("STREAMMinor"));
	if (minor == 0) {
		throw new IllegalStateException("New major version not yet handled");
	}
	return major + "." + (minor - 1);
}

JSON.Array collectFileEntries(Map<Path, FileInfo> buildFiles, Predicate<String> filenameFilter) {
	return collectFilesInDirectory(buildFiles, null, filenameFilter);
}

JSON.Array collectFilesInDirectory(Map<Path, FileInfo> buildFiles, Path inDirectory, Predicate<String> filenameFilter) {
	Function<Entry<Path, FileInfo>, String> getFilename = f -> f.getKey().getFileName().toString();
	int expectedNameCount = inDirectory != null ? inDirectory.getNameCount() + 1 : 1;
	return buildFiles.entrySet().stream().filter(f -> {
		Path file = f.getKey();
		if (file.getNameCount() != expectedNameCount || (inDirectory != null && !file.startsWith(inDirectory))) {
			return false;
		}
		return filenameFilter.test(getFilename.apply(f));
	}).sorted(Comparator.comparing(getFilename, BY_OS_ARCH_FILE_NAME))
			.map(e -> createFileDescription(e.getKey().getFileName(), e.getValue())).collect(JSON.Array.toJSONArray());
}

final Comparator<String> BY_OS_ARCH_FILE_NAME = Comparator.comparing((String filename) -> { // first sort by OS
	if (OS.isWindowsArtifact(filename)) {
		return 1;
	} else if (OS.isLinuxArtifact(filename)) {
		return 2;
	} else if (OS.isMacOSArtifact(filename)) {
		return 3;
	}
	return 100;
}).thenComparing(Comparator.comparing(filename -> { // then sort by arch
	if (filename.contains("-x86_64")) {
		return 1;
	}
	return 100;
})).thenComparing(Comparator.naturalOrder());

JSON.Object createFileEntry(String filename, Map<Path, FileInfo> buildFiles, String platform) {
	Path filePath = Path.of(filename);
	JSON.Object file = createFileDescription(filePath, buildFiles.get(filePath));
	if (platform != null) {
		file.add("platform", JSON.String.create(platform));
	}
	return file;
}

void writeChecksumsSummaryFile(JSON.Object buildProperties, String buildId, String client) throws IOException {
	Path checksumSummary = DIRECTORY.resolve(client + "-" + buildId + "-checksums");
	List<String> lines = buildProperties.members().values().stream() //
			.filter(JSON.Array.class::isInstance).map(JSON.Array.class::cast) //
			.map(JSON.Array::elements).flatMap(List::stream) //
			.filter(JSON.Object.class::isInstance).map(JSON.Object.class::cast) //
			.map(Object::members).filter(o -> o.containsKey(FILENAME) && o.containsKey(FILE_HASH_TYPE))
			.sorted(Comparator.comparing((Map<String, JSON.Value> o) -> getStringValue(o, FILENAME)))
			.map(o -> getStringValue(o, FILE_HASH_TYPE) + " *" + getStringValue(o, FILENAME)).toList();
	IO.println("Write checksum summary file to: " + checksumSummary);
	Files.write(checksumSummary, lines);
}

String getStringValue(Map<String, JSON.Value> o, String key) {
	return ((JSON.String) o.get(key)).characters();
}

JSON.Object createFileDescription(Path file, FileInfo fileInfo) {
	JSON.Object fileEntry = JSON.Object.create();
	fileEntry.add(FILENAME, JSON.String.create(file.toString().replace(File.separatorChar, '/')));
	fileEntry.add(FILE_SIZE, JSON.String.create(OS.fileSizeAsString(fileInfo.size())));
	fileEntry.add(FILE_HASH_TYPE, JSON.String.create(fileInfo.hashSHA512()));
	return fileEntry;
}

final Pattern INTEGRATION_BUILD_ID = Pattern.compile("(I|Y)(?<date>\\d{8})-(?<time>\\d{4})");
final ZoneId BUILD_TIMEZONE = ZoneId.of("America/New_York");
final DateTimeFormatter BASIC_LOCAL_TIME = DateTimeFormatter.ofPattern("HHmm");

ZonedDateTime buildTimestamp(String buildId) {
	Matcher buildIDMatcher = INTEGRATION_BUILD_ID.matcher(buildId);
	if (!buildIDMatcher.matches()) {
		throw new IllegalArgumentException("Unexpected BUILD_ID: " + buildId);
	}
	LocalDate date = LocalDate.parse(buildIDMatcher.group("date"), DateTimeFormatter.BASIC_ISO_DATE);
	LocalTime time = LocalTime.parse(buildIDMatcher.group("time"), BASIC_LOCAL_TIME);
	return date.atTime(time).atZone(BUILD_TIMEZONE).withZoneSameInstant(ZoneOffset.UTC);
}
