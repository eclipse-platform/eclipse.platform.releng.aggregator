/*******************************************************************************
 *  Copyright (c) 2026 Vogella GmbH and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     Lars Vogel - initial API and implementation
 *******************************************************************************/

/// Measures how long an installed Eclipse IDE takes to start, repeatedly, and
/// reports wall-clock and CPU time per run.
///
/// ```
/// xvfb-run -a java StartupTimeBenchmark.java --eclipse <launcher> --runs 10 --csv results.csv
/// ```
///
/// Options:
///
/// - `--eclipse <path>` -- Eclipse launcher executable (required)
/// - `--runs <n>` -- number of launches, default 10
/// - `--workdir <dir>` -- scratch directory, default a temporary directory
/// - `--csv <file>` -- additionally write the results as CSV
/// - `--timeout <sec>` -- per-launch timeout, default 300
/// - `--no-initialize` -- skip priming, see below
///
/// A display is required. On a headless Linux machine wrap the whole benchmark,
/// not the individual launches, as shown above.
///
/// ## What is measured
///
/// Wall-clock time is read from the `Startup complete: <n>ms` line that
/// `org.eclipse.ui.internal.misc.UIStats` prints when the platform runs with
/// `org.eclipse.core.runtime/debug=true`. It spans from the timestamp the native
/// launcher records in `eclipse.startTime` until the workbench event loop starts
/// running. CPU time is sampled from the launcher process tree just before it is
/// stopped, and covers all threads, so it exceeds wall-clock time on multi-core
/// machines.
///
/// Both are reported because they fail in opposite ways. Wall-clock time is what
/// users perceive but also counts everything else the machine was doing, so it
/// degrades badly on a loaded or shared agent. CPU time is insensitive to that
/// competition but cannot see I/O waits or work moved onto background threads,
/// and it spreads wider between runs because it sums all threads.
///
/// Compare medians, never single runs. A wall-clock `spread` above roughly 10%
/// means the machine was too busy for the numbers to support a comparison.
///
/// ## Why the installation is primed first
///
/// Each measured launch is stopped as soon as it reports its startup time, so it
/// never shuts down in an orderly way and `ExtensionRegistry.stop()` never runs,
/// which is the only place the extension registry cache is written. Without that
/// cache every launch re-parses all `plugin.xml` files, which costs about a
/// second on an Eclipse SDK and is not what a repeat start looks like.
///
/// The benchmark therefore primes the installation with `eclipse -initialize`,
/// which starts and stops the framework in an orderly way, and reports whether
/// the cache is in place so it is always clear which path was measured. Use
/// `--no-initialize` to measure the re-parse path deliberately, which is what
/// users get on the first start after an install or an update.
void main(String[] args) throws Exception {
	if (!parseArguments(args)) {
		printUsage();
		System.exit(1);
	}
	System.exit(run());
}

final Pattern STARTUP_COMPLETE = Pattern.compile("Startup complete: (\\d+)ms");

/// Debug options that make UIStats emit the startup timing.
final String DEBUG_OPTIONS = "org.eclipse.core.runtime/debug=true\n";

Path eclipse;
Path workDir;
Path csv;
int runs = 10;
int timeoutSeconds = 300;
boolean initialize = true;

record Result(int run, boolean cold, long wallMillis, long cpuMillis) {
}

boolean parseArguments(String[] args) {
	for (int i = 0; i < args.length; i++) {
		String option = args[i];
		switch (option) {
		case "--eclipse" -> eclipse = Path.of(requireValue(args, ++i, option));
		case "--runs" -> runs = Integer.parseInt(requireValue(args, ++i, option));
		case "--workdir" -> workDir = Path.of(requireValue(args, ++i, option));
		case "--csv" -> csv = Path.of(requireValue(args, ++i, option));
		case "--timeout" -> timeoutSeconds = Integer.parseInt(requireValue(args, ++i, option));
		case "--no-initialize" -> initialize = false;
		default -> {
			System.err.println("Unknown option: " + option);
			return false;
		}
		}
	}
	if (eclipse == null) {
		System.err.println("Missing required option: --eclipse");
		return false;
	}
	if (!Files.isExecutable(eclipse)) {
		System.err.println("Not an executable Eclipse launcher: " + eclipse);
		return false;
	}
	if (runs < 1) {
		System.err.println("--runs must be at least 1");
		return false;
	}
	return true;
}

String requireValue(String[] args, int index, String option) {
	if (index >= args.length) {
		throw new IllegalArgumentException("Missing value for " + option);
	}
	return args[index];
}

void printUsage() {
	System.err.println("""
			Usage: java StartupTimeBenchmark.java --eclipse <launcher> [options]

			  --eclipse <path>   Eclipse launcher executable (required)
			  --runs <n>         number of launches, default 10
			  --workdir <dir>    scratch directory, default a temporary directory
			  --csv <file>       additionally write the results as CSV
			  --timeout <sec>    per-launch timeout, default 300
			  --no-initialize    skip priming, and measure the registry re-parse path instead

			Needs a display. Headless: xvfb-run -a java StartupTimeBenchmark.java ...
			""");
}

int run() throws IOException, InterruptedException {
	if (workDir == null) {
		workDir = Files.createTempDirectory("eclipse-startup-benchmark");
	} else {
		Files.createDirectories(workDir);
	}
	Path options = workDir.resolve("startup.options");
	Files.writeString(options, DEBUG_OPTIONS);
	Path workspace = workDir.resolve("workspace");

	IO.println("Eclipse:   " + eclipse);
	IO.println("Work dir:  " + workDir);
	IO.println("Runs:      " + runs + " (run 1 starts with a fresh workspace)");
	if (initialize) {
		IO.println("Priming:   eclipse -initialize");
		initializeConfiguration();
	}
	IO.println("Registry cache: " + (hasRegistryCache() ? "present" : "ABSENT, measuring the re-parse path"));
	IO.println("");

	List<Result> results = new ArrayList<>();
	for (int run = 1; run <= runs; run++) {
		Result result = measure(run, options, workspace);
		if (result == null) {
			IO.println("run %2d: FAILED, see %s".formatted(run, logFile(run)));
		} else {
			IO.println("run %2d: %s wall %5d ms   cpu %5d ms".formatted(run, result.cold() ? "cold" : "warm",
					result.wallMillis(), result.cpuMillis()));
			results.add(result);
		}
	}

	IO.println("");
	report(results);
	if (csv != null) {
		writeCsv(results);
		IO.println("CSV written to " + csv);
	}
	return results.size() == runs ? 0 : 1;
}

/// Runs `eclipse -initialize`, which starts and stops the framework in an
/// orderly way and therefore writes the extension registry cache.
void initializeConfiguration() throws IOException, InterruptedException {
	Process process = new ProcessBuilder(eclipse.toString(), "-initialize", "-consoleLog") //
			.redirectErrorStream(true) //
			.redirectOutput(workDir.resolve("initialize.log").toFile()) //
			.start();
	if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
		process.destroyForcibly();
		System.err.println("Warning: -initialize timed out, see " + workDir.resolve("initialize.log"));
	}
}

/// The registry cache lives in the configuration area of the installation, next
/// to the launcher, and is written by ExtensionRegistry.stop().
boolean hasRegistryCache() {
	Path cache = eclipse.toAbsolutePath().getParent().resolve("configuration").resolve("org.eclipse.core.runtime");
	try (Stream<Path> files = Files.list(cache)) {
		return files.anyMatch(f -> f.getFileName().toString().startsWith(".mainData"));
	} catch (IOException e) {
		return false;
	}
}

/// Returns null if the launch did not report a startup time in time.
Result measure(int run, Path options, Path workspace) throws IOException, InterruptedException {
	boolean cold = !Files.exists(workspace);

	ProcessBuilder builder = new ProcessBuilder(eclipse.toString(), //
			"-data", workspace.toString(), //
			"-debug", options.toString(), //
			"-nosplash", //
			"-consoleLog");
	builder.redirectErrorStream(true);
	Process process = builder.start();

	Thread watchdog = watchdog(process);
	OptionalLong wallMillis;
	long cpuMillis;
	try (BufferedReader reader = new BufferedReader(
			new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
			Writer log = Files.newBufferedWriter(logFile(run))) {
		wallMillis = readUntilStartupComplete(reader, log);
		// Must be sampled while the tree is alive, the counters vanish on exit.
		cpuMillis = cpuMillisOf(process.toHandle());
	} finally {
		watchdog.interrupt();
		destroyTree(process);
	}
	// A surviving process keeps the workspace locked and would fail every later run.
	if (!process.waitFor(30, TimeUnit.SECONDS)) {
		process.destroyForcibly().waitFor(30, TimeUnit.SECONDS);
	}

	return wallMillis.isPresent() ? new Result(run, cold, wallMillis.getAsLong(), cpuMillis) : null;
}

OptionalLong readUntilStartupComplete(BufferedReader reader, Writer log) throws IOException {
	String line;
	while ((line = reader.readLine()) != null) {
		log.write(line);
		log.write(System.lineSeparator());
		Matcher matcher = STARTUP_COMPLETE.matcher(line);
		if (matcher.find()) {
			return OptionalLong.of(Long.parseLong(matcher.group(1)));
		}
	}
	return OptionalLong.empty();
}

/// Stops the launch if it never reports a startup time, which unblocks the reader.
Thread watchdog(Process process) {
	Thread watchdog = new Thread(() -> {
		try {
			Thread.sleep(Duration.ofSeconds(timeoutSeconds));
			destroyTree(process);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}, "startup-benchmark-watchdog");
	watchdog.setDaemon(true);
	watchdog.start();
	return watchdog;
}

long cpuMillisOf(ProcessHandle handle) {
	return Stream.concat(Stream.of(handle), handle.descendants()) //
			.map(ProcessHandle::info) //
			.map(ProcessHandle.Info::totalCpuDuration) //
			.flatMap(Optional::stream) //
			.mapToLong(Duration::toMillis) //
			.sum();
}

void destroyTree(Process process) {
	process.toHandle().descendants().forEach(ProcessHandle::destroy);
	process.destroy();
}

Path logFile(int run) {
	return workDir.resolve("run-" + run + ".log");
}

void report(List<Result> results) {
	if (results.isEmpty()) {
		IO.println("No successful runs.");
		return;
	}
	// The first run pays for creating the workspace and for a cold page cache,
	// so it is reported but kept out of the statistics.
	List<Result> steady = results.stream().filter(r -> !r.cold()).toList();
	if (steady.isEmpty()) {
		IO.println("Only a cold run, no steady-state statistics. Use --runs 10 or more.");
		return;
	}
	summarize("wall", steady.stream().mapToLong(Result::wallMillis).sorted().toArray());
	summarize("cpu ", steady.stream().mapToLong(Result::cpuMillis).sorted().toArray());
	IO.println("");
	IO.println(steady.size() + " steady-state runs. Compare medians, never single runs."
			+ " A wall-clock spread above roughly 10% means the machine was too busy to judge small"
			+ " changes on. CPU time normally spreads wider because it sums all threads.");
}

void summarize(String label, long[] sorted) {
	long median = median(sorted);
	long min = sorted[0];
	long max = sorted[sorted.length - 1];
	double spread = median == 0 ? 0 : (max - min) * 100.0 / median;
	IO.println(String.format(Locale.ROOT, "%s  median %5d ms   min %5d ms   max %5d ms   spread %4.1f%%", label,
			median, min, max, spread));
}

long median(long[] sorted) {
	int middle = sorted.length / 2;
	return sorted.length % 2 == 1 ? sorted[middle] : (sorted[middle - 1] + sorted[middle]) / 2;
}

void writeCsv(List<Result> results) {
	try (Writer writer = Files.newBufferedWriter(csv)) {
		writer.write("run,workspace,wall_ms,cpu_ms\n");
		for (Result result : results.stream().sorted(Comparator.comparingInt(Result::run)).toList()) {
			writer.write("%d,%s,%d,%d%n".formatted(result.run(), result.cold() ? "cold" : "warm",
					result.wallMillis(), result.cpuMillis()));
		}
	} catch (IOException e) {
		throw new UncheckedIOException(e);
	}
}
