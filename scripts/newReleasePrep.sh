#! /bin/bash

Help () {
    echo "
    Usage: $0 -v NEXT_STREAM -t NEXT_TRAIN -i PREVIOUS_RELEASE_ISSUE -p PREVIOUS_STREAM
    Example: $0 -v 4.25 -t 2022-09 -i 48 -p 4.24
    "
    exit
}

if [[ $# -lt 4 ]]; then Help; exit; fi

while [[ "$#" -gt 0 ]]; do
  case $1 in
    '-v') NEXT_STREAM=$(echo $2|tr -d ' '); shift 2;;
    '-t') NEXT_TRAIN=$(echo $2|tr -d ' '); shift 2;;
    '-i') PREV_ISSUE=$(echo $2|tr -d ' '); shift 2;;
    '-p') PREV_MAJOR="${2%.*}"; PREV_MINOR="${2#*.}"; shift 2;;
    '-h') Help; exit;;
  esac
done

TITLE="Preparation work for ${NEXT_STREAM} (${NEXT_TRAIN}) and open master for development"

BODY="This preparation work involves the following tasks. For previous bug please refer to eclipse-platform/eclipse.platform.releng.aggregator#${PREV_ISSUE}.

- [ ] Create R${PREV_MAJOR}_${PREV_MINOR}_maintenance branch
- [ ] Update R${PREV_MAJOR}_${PREV_MINOR}_maintenance branch with release version for ${PREV_MAJOR}.${PREV_MINOR}+ changes
- [ ] Update JenkinsJobs for ${NEXT_STREAM}:
- - [ ] Add ${NEXT_STREAM} to JobDSL.json to create new jobs
- - [ ] Update "Brances" in JobDSL.json to move ${PREV_MAJOR}.${PREV_MINOR}-I builds to R${PREV_MAJOR}_${PREV_MINOR}_maintenance branch
- - [ ] Add R${PREV_MAJOR}_${PREV_MINOR}_maintenance branch to parent pom and target sdk deployment jobs
- - [ ] Update I-build triggers with dates for ${NEXT_STREAM} milestone
- [ ] Splash Screen for ${NEXT_STREAM} (${NEXT_TRAIN})
- [ ] Create ${NEXT_STREAM}-I-builds repo
- [ ] Run [`Prepare Next Development Cycle`](https://ci.eclipse.org/releng/job/Releng/job/prepareNextDevCycle/) job and submit the created changes
- [ ] Update version number in Mac's Eclipse.app for ${NEXT_STREAM}
- [ ] Clean forceQualifierUpdate files for doc bundles
- [ ] Update builds and repo cleanup scripts for ${NEXT_STREAM}
- [ ] Update check composites script to verify ${NEXT_STREAM} repositories
- [ ] Update Comparator repo and eclipse run repo to ${NEXT_STREAM}-I-builds repo
- [ ] Version bumps for ${NEXT_STREAM} stream
- [ ] Update previous release version to ${PREV_MAJOR}.${PREV_MINOR} GA across build scripts
- [ ] Update build calendar for platform ${NEXT_STREAM} release
- [ ] Deploy ecj compiler from ${PREV_MAJOR}.${PREV_MINOR} GA and use it for ${NEXT_STREAM} M1 build
- [ ] Update generic repos I-builds, Y-builds, P-builds to point to ${NEXT_STREAM} repos
"

echo "Creating Issue $TITLE"
echo "$BODY"

gh issue create --title "$TITLE" --body "$BODY" --assignee @me
