#! /bin/bash

Help () {
    echo "
    Usage: $0 -v STREAM -i PREVIOUS_RELEASE_ISSUE
    Example: $0 -v 4.25 -i 273
    "
    exit
}

if [[ $# -lt 2 ]]; then Help; exit; fi

while [[ "$#" -gt 0 ]]; do
  case $1 in
    '-v') STREAM=$(echo $2|tr -d ' '); shift 2;;
    '-i') PREV_ISSUE=$(echo $2|tr -d ' '); shift 2;;
    '-h') Help; exit;;
  esac
done

MAJOR="${STREAM%.*}"
MINOR="${STREAM#*.}"

TITLE="Release ${STREAM}"

BODY="Umbrella bug to track release activities for ${STREAM}
For previous bug please refer to eclipse-platform/eclipse.platform.releng.aggregator#${PREV_ISSUE}.


- [ ] Readme file for ${STREAM}
- [ ] ${STREAM} Acknowledgements
- [ ] Migration Guide
- [ ] SWT Javadoc bash for ${STREAM}
- [ ] Publish ${STREAM} to Maven central
- [ ] Contribute ${STREAM} to SimRel
- [ ] Clean up intermediate artifacts (milestones, I-builds and old releases)

@notifications:
@SDawley, @lshanmug, @SarikaSinha, @ktatavarthi, @niraj-modi
"

echo "Creating Issue $TITLE"
echo "$BODY"

gh issue create --title "$TITLE" --body "$BODY" --assignee @me
