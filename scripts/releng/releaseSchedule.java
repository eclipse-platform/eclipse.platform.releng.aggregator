/*******************************************************************************
 * Copyright (c) 2026, 2026 Hannes Wellmann and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Hannes Wellmann - initial API and implementation
 *******************************************************************************/

import static java.util.stream.Collectors.joining;

import utilities.OS;

Map<String, LocalDate> relengDates;
Optional<LocalDate> javaReleaseDate;
Path file;
String releaseVersion;
String previousReleaseVersion;

/// Considered JVM properties are:
/// - `relengDates` (required): The RelEng dates of the release as
///   comma-separated list of `name:value` pairs.
/// - `javaReleaseDate` (optional): Release date of the next Java version, if
///   released shortly after the Eclipse release (and targeted by the Y-build).
/// - `releaseVersion` (required for calendar): the prepared, upcoming release
/// - `previousReleaseVersion` (required for calendar): the previous release
/// - `simRelName` (required for calendar): the SimRel name of the release
/// - `file` (required for calendar): path to the calendar file to update
///
void main(String[] args) throws IOException {
	relengDates = Arrays.stream(OS.readProperty("relengDates").split(",")).map(e -> e.split(":", 2))
			.collect(Collectors.toMap(a -> a[0], a -> LocalDate.parse(a[1])));
	javaReleaseDate = Optional.ofNullable(System.getProperty("javaReleaseDate")).filter(s -> !s.isEmpty())
			.map(LocalDate::parse);

	switch (args[0]) {
	case "--updateCalendar" -> generateCalendar();
	case "--generateIBuildSchedule" -> printCronScheduleFrom(iBuildRecurrences());
	case "--generateYBuildSchedule" -> printCronScheduleFrom(yBuildRecurrences());
	default -> throw new IllegalArgumentException("Unknown command: " + args[0]);
	}
}

final ZonedDateTime NOW = ZonedDateTime.now().withZoneSameInstant(ZoneOffset.UTC);

List<DailyRecurrence> iBuildRecurrences() {
	OffsetTime iBuildTime = OffsetTime.of(LocalTime.of(23, 0), ZoneOffset.UTC);
	ZonedDateTime iBuildStart = NOW.plusDays(1).with(iBuildTime);
	return List.of(new DailyRecurrence(iBuildStart, rcPhaseEnd(), List.of()));
}

List<DailyRecurrence> yBuildRecurrences() {
	OffsetTime yBuildTime = OffsetTime.of(LocalTime.of(15, 0), ZoneOffset.UTC);
	List<DayOfWeek> yBuildDays = List.of(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.SATURDAY);
	ZonedDateTime yBuildStart = NOW.with(TemporalAdjusters.next(yBuildDays.getFirst())).with(yBuildTime);
	if (javaReleaseDate.isPresent()) {
		// When a java release is imminent, prolong Y-builds and add a separate RC phase
		// around the Java release with increased Y-build frequency.
		LocalDate javaRelease = javaReleaseDate.get();
		LocalDate javaRCStart = javaRelease.minusDays(7);
		LocalDate javaRCEnd = javaRelease.plusDays(2);
		ZonedDateTime javaRCBuildStart = javaRCStart.atTime(yBuildTime).atZoneSameInstant(ZoneOffset.UTC);
		return List.of( //
				new DailyRecurrence(yBuildStart, javaRCStart.minusDays(1 /* Prevent overlap */), yBuildDays),
				new DailyRecurrence(javaRCBuildStart, javaRCEnd, List.of()));
	} else {
		return List.of(new DailyRecurrence(yBuildStart, rcPhaseEnd(), yBuildDays));
	}
}

/// Regular integration builds end with the RC phase.
LocalDate rcPhaseEnd() {
	return relengDates.get("RC2").minusDays(2); // The day before RC2 sign-off
}

void generateCalendar() throws IOException {
	file = Path.of(OS.readProperty("file")).toRealPath();
	releaseVersion = OS.readProperty("releaseVersion");
	previousReleaseVersion = OS.readProperty("previousReleaseVersion");
	String simRelName = OS.readProperty("simRelName");

	IO.println("INFO: Generate RelEng calendar for Eclipse " + releaseVersion);
	IO.println("\t" + file);

	List<String> lines = updateCalendarObject(events -> {
		LocalDate rc2 = relengDates.get("RC2");
		LocalDate ga = relengDates.get("GA");

		Duration buildRuntime = Duration.ofHours(1); // expected build runtime

		// I-build
		recurringEvent("Eclipse " + releaseVersion + " I-build", iBuildRecurrences(), buildRuntime,
				"Daily integration build of the latest changes in Eclipse-Platform, JDT, Equinox and PDE build together.")
		.forEach(events::accept);

		// Y-build
		recurringEvent("Eclipse " + releaseVersion + " Y-build", yBuildRecurrences(), buildRuntime,
				"Integration build of the latest development state of JDT's support for the upcoming, not yet released, Java version.")
		.forEach(events::accept);
		if (javaReleaseDate.isPresent()) {
			events.accept(singleDayEvent("New Java Release", javaReleaseDate.get(),
					"Scheduled release of the new Java version targeted by the " + releaseVersion
					+ " Y-build:\nhttps://openjdk.org/projects/jdk"));
		}

		// RelEng events
		relengDates.forEach((title, date) -> {
			String eclipseVersion = "Eclipse " + releaseVersion + " ";
			if (title.startsWith("RC")) {
				String prefix = eclipseVersion + title;
				int freezeOffSet = "RC2".equals(title) ? 6 : 3;
				events.accept(singleEvent(prefix + " Stabilization", date.minusDays(freezeOffSet), date.minusDays(1),
						"Stabilization and quiet period before " + prefix
						+ ". Code changes are only permitted to fix new and severe bugs."));
				events.accept(singleDayEvent(prefix + " Sign-off", date.minusDays(1),
						"Sign-off of the candidate build for " + prefix + "."));
			}

			String eventTitle = eclipseVersion
					+ ("GA".equals(title) ? ("(" + simRelName + ") release") : (title + " promotion"));
			events.accept(singleDayEvent(eventTitle, date, null));
		});

		events.accept(singleEvent("Eclipse " + releaseVersion + " quiet period", rc2.plusDays(1), ga, //
				"Quiet period before " + releaseVersion
				+ " release. Only exceptionally urgent code changes are accepted."));

	});
	Files.write(file, lines);
}

final String EVENT_START = "BEGIN:VEVENT";
final String EVENT_END = "END:VEVENT";
//ISO.8601.2004 complete basic format. BASIC_ISO_DATE is not suitable
final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("uuuuMMdd['T'HHmmss'Z']").withZone(ZoneOffset.UTC);

final String ECLIPSE_RELEASE_PROPERTY = "X-ECLIPSE-RELEASE";

List<String> updateCalendarObject(Consumer<Consumer<List<String>>> events) {
	List<String> calendarLines = loadCalendarAndRemoveOldEvents(file, previousReleaseVersion);
	if (!"END:VCALENDAR".equals(calendarLines.getLast())) {
		throw new IllegalArgumentException("Malformed calendar file. Unexpected last line: " + calendarLines.getLast());
	}
	calendarLines.removeLast();

	Stream.Builder<List<String>> allEvents = Stream.builder();
	events.accept(allEvents);
	allEvents.build().flatMap(List::stream).forEach(calendarLines::add);

	calendarLines.add("END:VCALENDAR");
	return calendarLines;
}

List<String> singleDayEvent(String title, LocalDate start, String description) {
	return singleEvent(title, start, start.plusDays(1), description);
}

List<String> singleEvent(String title, Temporal start, Temporal end, String description) {
	return recurringEvent(title, start, end, null, description);
}

List<List<String>> recurringEvent(String title, List<DailyRecurrence> recurrences, Duration duration,
		String description) {
	return recurrences.stream().map(recur -> {
		LocalTime startTime = recur.start().toLocalTime();
		String rRule = recurrenceDailyUntil(recur.end().atTime(startTime), recur.weekDays());
		return recurringEvent(title, recur.start(), recur.start().plus(duration), rRule, description);
	}).toList();
}

List<String> recurringEvent(String title, Temporal start, Temporal end, String rule, String description) {
	return List.of(EVENT_START, // https://www.rfc-editor.org/info/rfc5545/#section-3.8.2
			dateTime("DTSTART", start), //
			dateTime("DTEND", end), //
			optional("RRULE:", rule), //
			"SUMMARY:" + title, //
			optional("DESCRIPTION:", escape(description)), //
			"UID:" + UUID.randomUUID(), //
			"STATUS:CONFIRMED", // https://www.rfc-editor.org/info/rfc5545/#section-3.8.1.11
			"TRANSP:TRANSPARENT", // https://www.rfc-editor.org/info/rfc5545/#section-3.8.2.7
			// https://www.rfc-editor.org/info/rfc5545/#section-3.8.7
			dateTime("CREATED", NOW), //
			dateTime("LAST-MODIFIED", NOW), //
			dateTime("DTSTAMP", NOW), // Actually it should be the time when the ical file was fetched last
			"SEQUENCE:0", //
			ECLIPSE_RELEASE_PROPERTY + ":" + releaseVersion, //
			EVENT_END).stream().filter(l -> !l.isEmpty()) // Remove absent optional values
			.<String>mapMulti((line, downstream) -> {
				int maxLength = 75; // break long lines: https://www.rfc-editor.org/info/rfc5545/#section-3.1
				String prefix = "";
				for (int i = 0; i < line.length(); i += maxLength) {
					downstream.accept(prefix + line.substring(i, Math.min(i + maxLength, line.length())));
					prefix = " ";
				}
			}).toList();
}

record DailyRecurrence(
		/// Start of this recurrence.
		ZonedDateTime start,
		/// End day of this recurrence (inclusive if matched by a weekday).
		LocalDate end,
		/// The days of a week this recurrence happens, if empty on each day.
		List<DayOfWeek> weekDays) {
}

/// @param end inclusive end of the recurrence (type must match its start time)
String recurrenceDailyUntil(Temporal end, List<DayOfWeek> weekDays) {
	String byWdayList = weekDays.stream().map(t -> t.toString().substring(0, 2)).collect(joining(","));
	return "FREQ=DAILY" + (!byWdayList.isEmpty() ? ";BYDAY=" + byWdayList : "") + ";UNTIL=" + DATE_TIME.format(end);
}

String escape(String value) {
	if (value == null) {
		return null;
	} // https://www.rfc-editor.org/info/rfc5545/#section-3.3.11
	return value.replace("\\", "\\\\").replaceAll("\\R", "\\\\n").replace(";", "\\;").replace(",", "\\,");
}

String optional(String key, String value) {
	return value != null && !value.isEmpty() ? key + value : "";
}

String dateTime(String key, Temporal temporal) {
	boolean isDate = !temporal.isSupported(ChronoField.HOUR_OF_DAY);
	return key + (isDate ? ";VALUE=DATE:" : ":") + DATE_TIME.format(temporal);
}

List<String> loadCalendarAndRemoveOldEvents(Path file, String retainedReleaseVersion) {
	String releaseMarker = ECLIPSE_RELEASE_PROPERTY + ":" + retainedReleaseVersion;
	List<String> calendarLines;
	try (var lines = Files.lines(file)) {
		calendarLines = lines.gather(Gatherer.<String, List<String>, String>ofSequential(ArrayList::new,
				Gatherer.Integrator.ofGreedy((eventLines, line, downstream) -> {
					if (!eventLines.isEmpty() || EVENT_START.equals(line)) {
						eventLines.add(line);
					} else { // line is not part of an event, just keep it
						downstream.push(line);
					}
					if (EVENT_END.equals(line)) {
						if (eventLines.contains(releaseMarker)) {
							eventLines.forEach(downstream::push);
						}
						eventLines.clear();
					}
					return true;
				}))).filter(line -> !line.isEmpty()) //
				.collect(Collectors.toCollection(ArrayList::new));
	} catch (IOException e) {
		throw new UncheckedIOException(e);
	}
	return calendarLines;
}

// --- cron schedules ---

void printCronScheduleFrom(Collection<DailyRecurrence> recurrences) {
	// Jenkins cron: https://www.jenkins.io/doc/book/pipeline/syntax/#cron-syntax
	List<String> lines = new ArrayList<>(List.of("TZ=UTC", "# Format: Minute Hour Day Month Day-of-week (1-7)"));
	for (DailyRecurrence rec : recurrences) {
		int startMonth = rec.start().getMonthValue();
		LocalTime time = rec.start().toLocalTime(); // time is always the same
		int endMonth = rec.end().getMonthValue();
		if (startMonth != endMonth) {
			lines.add(cronExpression(time, rec.start().getDayOfMonth() + "-31", startMonth, rec.weekDays()));

			String completeMonths = monthsBetween(rec.start().toLocalDate(), rec.end()).map(Month::getValue)
					.map(i -> i.toString()).collect(joining(","));
			if (!completeMonths.isEmpty()) {
				lines.add(cronExpression(time, "*", completeMonths, rec.weekDays()));
			}
			lines.add(cronExpression(time, "1-" + rec.end().getDayOfMonth(), endMonth, rec.weekDays()));
		} else {
			String daysOfMonth = rec.start().getDayOfMonth() + "-" + rec.end().getDayOfMonth();
			lines.add(cronExpression(time, daysOfMonth, startMonth, rec.weekDays()));
		}
	}
	lines.forEach(IO::println);
}

Stream<Month> monthsBetween(LocalDate start, LocalDate end) {
	return start.plusMonths(1).datesUntil(end.with(TemporalAdjusters.firstDayOfMonth()), Period.ofMonths(1))
			.map(LocalDate::getMonth);
}

String cronExpression(LocalTime time, String dayOfMonth, Object month, List<DayOfWeek> daysOfWeek) {
	String dow = daysOfWeek.stream().map(DayOfWeek::getValue).map(i -> i.toString()).collect(joining(","));
	return time.getMinute() + " " + time.getHour() + " " + dayOfMonth + " " + month + " " + (dow.isEmpty() ? "*" : dow);
}
