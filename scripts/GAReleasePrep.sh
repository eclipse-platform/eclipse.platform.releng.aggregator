#! /bin/bash

Help () {
    echo "
    Usage: $0 -v STREAM -i PREVIOUS_RELEASE_ISSUE
    Example: $0 -v 4.25 -i 273
    "
    exit
}

if [[ $# -lt 2 ]]; then Help; exit; fi

while getopts "v" opt
do
  case $opt in
    v) STREAM="${OPTARG}";;
    i) PREV_ISSUE="${OPTARG}";;
    \?) Help; exit;;
  esac
done

MAJOR="${STREAM%.*}"
MINOR="${STREAM#*.}"

TITLE="Release ${STREAM}"

BODY="Umbrella bug to track release activities for ${STREAM}
For previous bug please refer to eclipse-platform/eclipse.platform.releng.aggregator#${PREV_ISSUE}.


- [ ] New & Noteworthy
- [ ] Readme file for ${STREAM}
- [ ] ${STREAM} Acknowledgements/sravanlakkimsetti
- [ ] Tips & Tricks
- [ ] Migration Guide
- [ ] SWT Javadoc bash for ${STREAM}
- [ ] Publish ${STREAM} to Maven central
- [ ] Tag Eclipse ${STREAM} Release
- [ ] Clean up intermediate artifacts (milestones, I-builds and old releases)

@notifications:
@SDawley, @lshanmug, @SarikaSinha, @ktatavarthi, @niraj-modi, @vik-chand
"

echo "Creating Issue $TITLE"

gh issue create --title "$TITLE" --body "$BODY" --assignee @me
