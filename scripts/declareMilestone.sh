#! /bin/bash

Help () {
    echo "
    Usage: $0 -v STREAM -i IBUILD -m MILESTONE -d DATE_OF_RELEASE
    Example: $0 -v 4.25 -i I20220824-1800 -m RC1 -d 2022-August-26
    "
    exit
}

if [[ $# -lt 4 ]]; then Help; exit; fi

while [[ "$#" -gt 0 ]]; do
  case $1 in
    '-v') STREAM=$(echo $2|tr -d ' '); shift 2;;
    '-i') IBUILD=$(echo $2|tr -d ' '); shift 2;;
    '-m') MILESTONE=$(echo $2|tr -d ' '); shift 2;;
    '-d') DATE=$(echo $2|tr -d ' '); shift 2;;
    '-h') Help; exit;;
  esac
done

TITLE="Declare ${STREAM} ${MILESTONE}"

BODY="A \"go\" is needed from all the components below to declare this milestone.

Current Candidate

Eclipse downloads:
https://download.eclipse.org/eclipse/downloads/drops4/${IBUILD}

Build logs and/or test results (eventually):
https://download.eclipse.org/eclipse/downloads/drops4/${IBUILD}/testResults.php

Software site repository:
https://download.eclipse.org/eclipse/updates/${STREAM}-I-builds

Specific (simple) site repository:
https://download.eclipse.org/eclipse/updates/${STREAM}-I-builds/${IBUILD}

Equinox downloads:
https://download.eclipse.org/equinox/drops/${IBUILD}

---
Deadlines: 
- Friday, ${DATE}, around 1 AM (Eastern): deadline for sign-off (or, by then, comment in this issue when you expect to sign-off).
- Friday, ${DATE}, around 3 AM (Eastern): promote approved build to S-${STREAM}${MILESTONE}-*, contribute to simultaneous release repo, and announce to mailing lists.

Remember to investigate and document here any failing JUnit tests. 

---

- [ ] Platform:
- - [ ] Resources
- - [ ] UI
- - [ ] Debug
- - [ ] Ant
- - [ ] SWT
- - [ ] Releng

- [ ] JDT:
- - [ ] Core
- - [ ] Debug
- - [ ] UI

- [ ] PDE
- [ ] Equinox
"

echo "Creating Issue $TITLE"
echo "$BODY"

gh issue create --title "$TITLE" --body "$BODY" --assignee @me
