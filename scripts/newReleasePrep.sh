#! /bin/bash

Help () {
    echo "
    Usage: $0 -v NEXT_STREAM -t NEXT_TRAIN -i PREVIOUS_RELEASE_ISSUE -p PREVIOUS_STREAM
    Example: $0 -v 4.25 -t (2022-09) -i 48 -p 4.24
    "
    exit
}

if [[ $# -lt 4 ]]; then Help; exit; fi

while getopts "v:t:i:p:" opt
do
  case $opt in
    v) NEXT_STREAM="${OPTARG}";;
    t) NEXT_TRAIN="${OPTARG}";;
    i) PREV_ISSUE="${OPTARG}";;
    p) PREV_MAJOR="${OPTARG%.*}"; PREV_MINOR="${OPTARG#*.}";;
    \?) Help; exit;;
  esac
done

TITLE="Preparation work for ${NEXT_STREAM} (${NEXT_TRAIN}) and open master for development"

BODY="This preparation work involves the following tasks. For previous bug please refer to eclipse-platform/eclipse.platform.releng.aggregator#${PREV_ISSUE}.

- [ ] Create R${PREV_MAJOR}_${PREV_MINOR}_maintenance branch
- [ ] Move ${PREV_MAJOR}.${PREV_MINOR}-I and ${PREV_MAJOR}.${PREV_MINOR}-Y builds to R${PREV_MAJOR}_${PREV_MINOR}_maintenance branch
- [ ] Update Parent pom and target sdk deployment jobs for R${PREV_MAJOR}_${PREV_MINOR}_maintenance branch
- [ ] Create new test jobs for ${NEXT_STREAM}
- [ ] Configure SWT build scripts for ${NEXT_STREAM}
- [ ] Splash Screen for ${NEXT_STREAM} (${NEXT_TRAIN})
- [ ] Create ${NEXT_STREAM}-I-builds repo
- [ ] POM and product version changes for ${NEXT_STREAM} release
- [ ] Update product version number to ${NEXT_STREAM} across build scripts
- [ ] Move previous version to ${PREV_MAJOR}.${PREV_MINOR}RC2 across build scripts
- [ ] Update version number in Mac's Eclipse.app for ${NEXT_STREAM}
- [ ] Disable the freeze report for ${NEXT_STREAM}
- [ ] Clean forceQualifierUpdate files for doc bundles
- [ ] Cleanup approved api list
- [ ] Update builds and repo cleanup scripts for ${NEXT_STREAM}
- [ ] Create new I-build job for ${NEXT_STREAM} release
- [ ] Update check composites script to verify ${NEXT_STREAM} repositories
- [ ] Update Comparator repo and eclipse run repo to ${NEXT_STREAM}-I-builds repo
- [ ] Version bumps for ${NEXT_STREAM} stream
- [ ] Update previous release version to ${PREV_MAJOR}.${PREV_MINOR} GA across build scripts
- [ ] Update build calendar for platform ${NEXT_STREAM} release
- [ ] Deploy ecj compiler from ${PREV_MAJOR}.${PREV_MINOR} GA and use it for ${NEXT_STREAM} M1 build
- [ ] Update generic repos I-builds, Y-builds, P-builds to point to ${NEXT_STREAM} repos
"

echo "Creating Issue $TITLE"

gh issue create --title "$TITLE" --body "$BODY" --assignee @me