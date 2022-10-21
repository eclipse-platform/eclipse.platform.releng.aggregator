job('Releng/PublishPlatformToMaven'){
  displayName('Publish Platform to Maven')
  description('Publish artifacts in groupId org.eclipse.platform to staging of OSSRH.')

  parameters {
    stringParam('REPO_ID', null, 'ID (buildNo) of repository created by CBI aggregator job.')
  }

  label('migration')

  jdk('openjdk-jdk11-latest')

  logRotator {
    numToKeep(5)
  }
 
  wrappers { //adds pre/post actions
    preBuildCleanup()
    credentialsBinding {
      file('KEYRING', 'secret-subkeys.asc (secret-subkeys.asc fpr JDT')
    }
    timestamps()
    sshAgent('git.eclipse.org-bot-ssh', 'github-bot-ssh')
  }
  
  steps {
    shell('''
#!/bin/bash -x
#*******************************************************************************
# Copyright (c) 2016 GK Software AG and others.
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v10.html
#
# Contributors:
#     Stephan Herrmann - initial API and implementation
#********************************************************************************

unset JAVA_TOOL_OPTIONS
unset _JAVA_OPTIONS
# Trust GPG keys
gpg --batch --import "${KEYRING}"
for fpr in $(gpg --list-keys --with-colons  | awk -F: '/fpr:/ {print $10}' | sort -u)
do
  echo -e "5\\ny\\n" |  gpg --batch --command-fd 0 --expert --edit-key ${fpr} trust
done

# BOOTSTRAP:

GIT_URL=git@github.com:eclipse-platform/eclipse.platform.releng.git
GIT_REL_PATH=eclipse.platform.releng/publish-to-maven-central
SCRIPT=publishPlatform.sh
BASELINE=baseline.txt
POM=platform-pom.xml

# -------- fetch script & pom: -------------
git clone -b master ${GIT_URL}

/bin/mv ${GIT_REL_PATH}/${SCRIPT} ${WORKSPACE}/
/bin/mv ${GIT_REL_PATH}/${BASELINE} ${WORKSPACE}/
/bin/mv ${GIT_REL_PATH}/${POM} ${WORKSPACE}/
/bin/rmdir -p ${GIT_REL_PATH}

env

chmod 744 ${SCRIPT}
bash -x ./${SCRIPT}

    ''')
  }

  publishers {
    archiveArtifacts {
      pattern('.log/*')
    }
  }
}
