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

TITLE="Release ${STREAM}"

BODY="Umbrella bug to track release activities for ${STREAM}
For previous bug please refer to eclipse-platform/eclipse.platform.releng.aggregator#${PREV_ISSUE}.

- [ ] Readme file for ${STREAM}
- [ ] Migration Guide
- [ ] Ensure the build is cleared of comparator errors in doc bundles before RC2 build
- [ ] Preparation of the subsequent development cycle
  - After RC2 was promoted, run the [`Prepare Next Development Cycle`](https://ci.eclipse.org/releng/job/Releng/job/prepareNextDevCycle/) Jenkins job and complete the tasks listed in the PR created by it in this repository.
  - [ ] Run it with final parameters as `dry-run` (ideally shortly) before and verify correctnes.

- [ ] Publish ${STREAM} to Maven central
  - The final publication should be done one or two days before the release to ensure everything is mirrored at the time of release.
- [ ] Contribute ${STREAM} to SimRel
  - Should be done one day before the release to ensure the SimRel aggregation does not contain the RC I-build repo, which is deleted on release day.
- [ ] Submit the pending PRs to update build scripts to ${STREAM} GA on master and the ${STREAM}-maintenance
  - Should be done one day before the release
- [ ] Delete the old [`I-build-${STREAM}`](https://ci.eclipse.org/releng/job/Builds/) job and its associated [Test jobs](https://ci.eclipse.org/releng/job/AutomatedTests/).
"

echo "Creating Issue $TITLE"
echo "$BODY"

gh issue create --title "$TITLE" --body "$BODY" --assignee @me
