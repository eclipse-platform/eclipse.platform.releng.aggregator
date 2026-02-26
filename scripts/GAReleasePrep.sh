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

// TODO: mention the jobs to run here and when to run them exactly? Or just in the general doc. And add links to the items in the general doc here with current commit-ids?!
- [ ] Readme file for ${STREAM}
- [ ] ${STREAM} Acknowledgements
  Just submit the generated PR.
- [ ] Migration Guide
- [ ] SWT Javadoc bash for ${STREAM}
- [ ] Publish ${STREAM} to Maven central
  - Should be done one day before the release to ensure everything is mirrored at the time of release
- [ ] Contribute ${STREAM} to SimRel
  - Should be done one day before the release to the SimRel aggregation does not contain the RC I-build repo deleted on release day.
<!-- TODO: check variables! -->
- Preparation of the subsequent development cycle
  - [ ] Run [`Prepare Next Development Cycle`](https://ci.eclipse.org/releng/job/Releng/job/prepareNextDevCycle/) job and submit the created changes
    - Run the preparation after RC1? RC2 would be build from maintenance branches
    - [ ] Run it with final parameters as `dry-run` (ideally a day) before and verify correctnes.

@notifications:
@SDawley, @lshanmug, @SarikaSinha, @ktatavarthi, @niraj-modi
"

echo "Creating Issue $TITLE"
echo "$BODY"

gh issue create --title "$TITLE" --body "$BODY" --assignee @me
