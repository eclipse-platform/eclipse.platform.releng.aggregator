#!/bin/bash
today=$(date "+%Y-%m-%dT00:00:00Z")
tomorrow=$(date -d "+1 days" "+%Y-%m-%dT00:00:00Z")
calendarId="prfk26fdmpru1mptlb06p0jh4s%40group.calendar.google.com"
curl "https://www.googleapis.com/calendar/v3/calendars/${calendarId}/events?timeMin=${today}&timeMax=${tomorrow}&key=${GOOGLE_API_KEY}" | grep -i -e "summary.*stabilization"
if  [[ $? == 0 ]]; then
	echo "Today is a freeze day"
	exit 1 #Exiting with non-0 makes the build fail, and Gerrit Jenkins plugin voting -1 on review
fi
echo "No freeze today"
exit 0 #Exiting with non-0 makes the build succeed, and Gerrit Jenkins plugin voting -1 on review
