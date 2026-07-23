/*******************************************************************************
 * Copyright (c) 2026 Hannes Wellmann and others.
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

import java.time.LocalDate
import java.time.ZonedDateTime

@groovy.transform.Field
def boolean _CALENDAR_API_IS_DRY_RUN = true

def setDryRun(boolean isDryRun) {
	_CALENDAR_API_IS_DRY_RUN = isDryRun
}

def initialize(Object utilities) {
	def gcloudInstall = utilities.installDownloadableTool('gcloud', 'https://dl.google.com/dl/cloudsdk/channels/rapid/downloads/google-cloud-cli-linux-x86_64.tar.gz')
	env.GCLOUD = "${gcloudInstall}/bin/gcloud"
	// https://docs.cloud.google.com/sdk/docs/configurations
	env.CLOUDSDK_CONFIG = "${gcloudInstall}/../config"
	// https://docs.cloud.google.com/sdk/docs/initializing
	sh '''#!/bin/bash -xe
		# Skip default initialization since this is a non-interactive session
		$GCLOUD config set disable_prompts true
		$GCLOUD auth activate-service-account --key-file=${GCLOULD_KEY_FILE}
	'''
}

// Calendar events: https://developers.google.com/workspace/calendar/api/v3/reference/events#resource

def createEvent(String title, LocalDate start, LocalDate end = null) {
	if (end == null) {
		end = start.plusDays(1) // Assume one day event
	}
	def parameters = [summary: title, start: [date: formatDate(start)], end: [date: formatDate(end)] ]
	insertEvent(parameters)
}

// Recurrence Rules: https://datatracker.ietf.org/doc/html/rfc5545#section-3.3.10

def createRecurrenceRRule(String frequency, LocalDate until) {
	// See https://datatracker.ietf.org/doc/html/rfc5545#section-3.8.5.3
	return "RRULE:FREQ=${frequency.toUpperCase()};UNTIL=${java.time.format.DateTimeFormatter.BASIC_ISO_DATE.format(until)}"
}
// createRecurrenceExRule(), createRecurrenceRDate() and createRecurrenceExDate() currently not needed

def createEvent(String title, ZonedDateTime start, ZonedDateTime end, recurrenceRules = null) {
	def parameters = [summary: title]
	parameters.start = [dateTime: formatDateTime(start), timeZone: start.zone.id]
	parameters.end = [dateTime: formatDateTime(end), timeZone: end.zone.id]
	parameters.recurrence = recurrenceRules
	insertEvent(parameters)
}

private String formatDate(LocalDate date) {
	return java.time.format.DateTimeFormatter.ISO_LOCAL_DATE.format(date)
}

private String formatDateTime(ZonedDateTime dateTime) {
	return java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(dateTime)
}

// https://developers.google.com/workspace/calendar/api/v3/reference/events/insert
private void insertEvent(Map<String, Object> parameters) {
	def response = queryGoogleCalendarAPI('-X POST', 'events', parameters)
	if (response != null && !isConfirmedEvent(response)) {
		error "Response contains errors:\n${response}"
	}
}

private boolean isConfirmedEvent(Map response) {
	return response.kind == 'calendar#event' && response.status == 'confirmed'
}

// See documentation of the Google Calendar API
// - https://developers.google.com/workspace/calendar/api/guides/overview

private Map<String, String> queryGoogleCalendarAPI(String method, String endpoint, Map<String, Object> parameters = null) {
	def calendarId = 'prfk26fdmpru1mptlb06p0jh4s@group.calendar.google.com'
	def query = """\
		GC_API_TOKEN=\$(\$GCLOUD auth print-access-token --scopes=https://www.googleapis.com/auth/calendar)
		curl -L ${method} \
			'https://www.googleapis.com/calendar/v3/calendars/${calendarId}/${endpoint}' \
			-H "Authorization: Bearer \${GC_API_TOKEN}" \
			-H 'Accept: application/json' \
			-H 'Content-Type: application/json'
		""".replace('\t','').trim()
	if (parameters) {
		def data = writeJSON(json: parameters, returnText: true)
		data = data.replace("'", "'\\''") // Escape single quotes by '\''
		query += " --data '${data}'"
	}
	if (_CALENDAR_API_IS_DRY_RUN && !method.isEmpty()) {
		echo "Query (not send):\n${query}"
		return null
	}
	echo "Execute:\n${query}"
	// Ensure the commands are not printed to prevent exposure of (temporary credentials)
	def response = sh(script: "#!/bin/bash -e\n${query}", returnStdout: true)
	if (response == null || response.isEmpty()) {
		error 'Response is null or empty. This commonly indicates: HTTP/1.1 500 Internal Server Error'
	}
	return readJSON(text: response)
}

return this
