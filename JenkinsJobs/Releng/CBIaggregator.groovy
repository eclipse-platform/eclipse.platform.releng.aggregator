job('Releng/CBIaggregator'){
  displayName('CBI Aggregator')
  description('Use the CBI aggregator and some scripts to mirror and condition an SDK release into a Maven-compliant repo.')

  logRotator {
    numToKeep(10)
  }

  parameters {
    choiceParam('snapshotOrRelease', ['release', 'snapshot'], null)
  }

  label('centos-latest')

  jdk('openjdk-jdk11-latest')

  scm {
    git {
      remote{
        name('origin')
        url('git@github.com:eclipse-platform/eclipse.platform.releng.git')
        credentials('github-bot-ssh')
      }
      branch('master')
      browser {
        githubWeb {
          repoUrl('https://github.com/eclipse-platform/eclipse.platform.releng')
        }
      }
      extensions {
        cloneOption {
          shallow(true)
          noTags(false)
          reference('')
          timeout(10)
        }
      }
    }
  }

  wrappers { //adds pre/post actions
    preBuildCleanup()
    timestamps()
    sshAgent('projects-storage.eclipse.org-bot-ssh', 'git.eclipse.org-bot-ssh', 'github-bot-ssh')
  }
  
  steps {
    shell('''
#!/bin/bash -x
#*******************************************************************************
# Copyright (c) 2016, 2018 GK Software SE and others.
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v10.html
#
# Contributors:
#     Stephan Herrmann - initial API and implementation
#********************************************************************************

SCRIPT=${WORKSPACE}/work/CBIaggregator.sh
mv publish-to-maven-central/ ${WORKSPACE}/work #script currently expects ${WORKSPACE}/work, we cannot use another directory
chmod +x ${SCRIPT}
${SCRIPT} ${snapshotOrRelease}
    ''')
  }

  publishers {
    downstreamParameterized {
      trigger('Releng/PublishJDTtoMaven') {
        condition('SUCCESS')
        parameters {
          predefinedProp('REPO_ID', '${BUILD_NUMBER}')
        }
      }
      trigger('Releng/PublishPDEtoMaven') {
        condition('SUCCESS')
        parameters {
          predefinedProp('REPO_ID', '${BUILD_NUMBER}')
        }
      }
      trigger('Releng/PublishPlatformtoMaven') {
        condition('SUCCESS')
        parameters {
          predefinedProp('REPO_ID', '${BUILD_NUMBER}')
        }
      }
    }
    archiveArtifacts {
      pattern('repo-*/**, baseline-next.txt')
    }
  }
}
