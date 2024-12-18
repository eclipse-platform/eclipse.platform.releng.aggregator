def config = new groovy.json.JsonSlurper().parseText(readFileFromWorkspace('JenkinsJobs/JobDSL.json'))
def STREAMS = config.Streams

for (STREAM in STREAMS){
  def BRANCH = config.Branches[STREAM]
  def MAJOR = STREAM.split('\\.')[0]
  def MINOR = STREAM.split('\\.')[1]

  pipelineJob('Builds/I-build-' + STREAM){
    description('Daily Eclipse Integration builds.')

    properties {
      pipelineTriggers {
        triggers {
          cron {
            spec("""TZ=America/Toronto
# format: Minute Hour Day Month Day of the week (0-7)

# - - - Integration Eclipse SDK builds - - - 
# 2025-03 Release Schedule
# Normal : 6 PM every day (1/6 - 2/9)
0 18 * * *


# Milestone/RC Schedule 
# Post M1, no nightlies, I-builds only. (Be sure to "turn off" for tests and sign off days)
# 0 6 14-26 2 5-7,1-3
# 0 18 14-26 2 5-7,1-3
            """)
          }
        }
      }
    }

    logRotator {
      numToKeep(25)
    }

    definition {
      cpsScm {
        lightweight(true)
        scm {
          github('https://github.com/eclipse-platform/eclipse.platform.releng.aggregator/')
        }
      }

      cps {
        sandbox()
        script('''
pipeline {
	options {
		timeout(time: 360, unit: 'MINUTES')
		timestamps()
		buildDiscarder(logRotator(numToKeepStr:'25'))
	}
  agent {
    kubernetes {
      inheritFrom 'ubuntu-2404'
      yaml """
apiVersion: v1
kind: Pod
spec:
  containers:
  - name: "jnlp"
    resources:
      limits:
        memory: "10Gi"
        cpu: "4000m"
      requests:
        memory: "6144Mi"
        cpu: "2000m"
"""
    }
  }
  tools {
      jdk 'temurin-jdk21-latest'
  }
  environment {
      MAVEN_OPTS = "-Xmx6G"
      CJE_ROOT = "${WORKSPACE}/eclipse.platform.releng.aggregator/eclipse.platform.releng.aggregator/cje-production"
      PATH = "$PATH:/opt/tools/apache-maven/latest/bin"
      logDir = "$CJE_ROOT/buildlogs"
    }
  stages {
      stage('Clean Workspace'){
          steps {
              container('jnlp') {
                cleanWs()
                }
            }
	    }
	  stage('Setup intial configuration'){
          steps {
              container('jnlp') {
                  sshagent(['github-bot-ssh']) {
                      dir ('eclipse.platform.releng.aggregator') {
                        sh \'\'\'
                            git clone -b ''' + BRANCH + ''' git@github.com:eclipse-platform/eclipse.platform.releng.aggregator.git
                        \'\'\'
                      }
                    }
                    sh \'\'\'
                        cd ${WORKSPACE}/eclipse.platform.releng.aggregator/eclipse.platform.releng.aggregator/cje-production
                        chmod +x mbscripts/*
                        mkdir -p $logDir
                    \'\'\'
                }
            }
		}
	  stage('Generate environment variables'){
          steps {
              container('jnlp') {
                sh \'\'\'
                    cd ${WORKSPACE}/eclipse.platform.releng.aggregator/eclipse.platform.releng.aggregator/cje-production/mbscripts
                    ./mb010_createEnvfiles.sh $CJE_ROOT/buildproperties.shsource 2>&1 | tee $logDir/mb010_createEnvfiles.sh.log
                    if [[ ${PIPESTATUS[0]} -ne 0 ]]
                    then
                        echo "Failed in Generate environment variables stage"
                        exit 1
                    fi
                \'\'\'
                }
            }
		}
	  stage('Load PGP keys'){
          environment {
                KEYRING = credentials('secret-subkeys-releng.asc')
                KEYRING_PASSPHRASE = credentials('secret-subkeys-releng.acs-passphrase')
          }
          steps {
              container('jnlp') {
                sh \'\'\'
                    cd ${WORKSPACE}/eclipse.platform.releng.aggregator/eclipse.platform.releng.aggregator/cje-production/mbscripts
                    ./mb011_loadPGPKeys.sh 2>&1 | tee $logDir/mb011_loadPGPKeys.sh.log
                    if [[ ${PIPESTATUS[0]} -ne 0 ]]
                    then
                        echo "Failed in Load PGP keys"
                        exit 1
                    fi
                \'\'\'
                }
            }
		}
	  stage('Export environment variables stage 1'){
          steps {
              container('jnlp') {
                script {
                    env.BUILD_IID = sh(script:'echo $(source $CJE_ROOT/buildproperties.shsource;echo $BUILD_TYPE$TIMESTAMP)', returnStdout: true)
                    env.BUILD_VERSION = sh(script:'echo $(source $CJE_ROOT/buildproperties.shsource;echo $RELEASE_VER)', returnStdout: true)
                    env.STREAM = sh(script:'echo $(source $CJE_ROOT/buildproperties.shsource;echo $STREAM)', returnStdout: true)
                    env.EBUILDER_HASH = sh(script:'echo $(source $CJE_ROOT/buildproperties.shsource;echo $EBUILDER_HASH)', returnStdout: true)
                    env.RELEASE_VER = sh(script:'echo $(source $CJE_ROOT/buildproperties.shsource;echo $RELEASE_VER)', returnStdout: true)
                  }
                }
            }
        }
	  stage('Create Base builder'){
          steps {
              container('jnlp') {
		      sshagent(['projects-storage.eclipse.org-bot-ssh']) {
		              withAnt(installation: 'apache-ant-latest') {
		                sh \'\'\'
		                    cd ${WORKSPACE}/eclipse.platform.releng.aggregator/eclipse.platform.releng.aggregator/cje-production/mbscripts
		                    ./mb020_createBaseBuilder.sh $CJE_ROOT/buildproperties.shsource 2>&1 | tee $logDir/mb020_createBaseBuilder.sh.log
		                    if [[ ${PIPESTATUS[0]} -ne 0 ]]
		                    then
		                        echo "Failed in Create Base builder stage"
		                        exit 1
		                    fi
		                \'\'\'
		              }
		        }
		      }
		}
	  }
	  stage('Download reference repo for repo reports'){
          steps {
              container('jnlp') {
                  sshagent(['projects-storage.eclipse.org-bot-ssh']) {
                    sh \'\'\'
                        cd ${WORKSPACE}/eclipse.platform.releng.aggregator/eclipse.platform.releng.aggregator/cje-production/mbscripts
                        ./mb030_downloadBuildToCompare.sh $CJE_ROOT/buildproperties.shsource 2>&1 | tee $logDir/mb030_downloadBuildToCompare.sh.log
                        if [[ ${PIPESTATUS[0]} -ne 0 ]]
                        then
                            echo "Failed in Download reference repo for repo reports stage"
                            exit 1
                        fi
                        cd ${WORKSPACE}
                    \'\'\'
                  }
                }
            }
		}
	  stage('Clone Repositories'){
          steps {
              container('jnlp') {
                  sshagent(['git.eclipse.org-bot-ssh', 'github-bot-ssh']) {
                    sh \'\'\'
                        git config --global user.email "eclipse-releng-bot@eclipse.org"
                        git config --global user.name "Eclipse Releng Bot"
                        cd ${WORKSPACE}/eclipse.platform.releng.aggregator/eclipse.platform.releng.aggregator/cje-production/mbscripts
                        ./mb100_cloneRepos.sh $CJE_ROOT/buildproperties.shsource 2>&1 | tee $logDir/mb100_cloneRepos.sh.log
                        if [[ ${PIPESTATUS[0]} -ne 0 ]]
                        then
                            echo "Failed in Clone Repositories stage"
                            exit 1
                        fi
                    \'\'\'
                  }
                }
            }
		}
	  stage('Tag Build Inputs'){
          steps {
              container('jnlp') {
                  sshagent (['git.eclipse.org-bot-ssh', 'github-bot-ssh', 'projects-storage.eclipse.org-bot-ssh']) {
                    sh \'\'\'
                        cd ${WORKSPACE}/eclipse.platform.releng.aggregator/eclipse.platform.releng.aggregator/cje-production/mbscripts
                        bash -x ./mb110_tagBuildInputs.sh $CJE_ROOT/buildproperties.shsource 2>&1 | tee $logDir/mb110_tagBuildInputs.sh.log
                        if [[ ${PIPESTATUS[0]} -ne 0 ]]
                        then
                            echo "Failed in Tag Build Inputs stage"
                            exit 1
                        fi
                    \'\'\'
                  }
                }
            }
		}
	  stage('Create Source Bundles'){
          steps {
              container('jnlp') {
                    sh \'\'\'
                        cd ${WORKSPACE}/eclipse.platform.releng.aggregator/eclipse.platform.releng.aggregator/cje-production/mbscripts
                        unset JAVA_TOOL_OPTIONS 
                        unset _JAVA_OPTIONS
                        ./mb200_createSourceBundles.sh $CJE_ROOT/buildproperties.shsource 2>&1 | tee $logDir/mb200_createSourceBundles.sh.log
                        if [[ ${PIPESTATUS[0]} -ne 0 ]]
                        then
                            echo "Failed in Create Source Bundles stage"
                            exit 1
                        fi
                    \'\'\'
                }
            }
		}
	  stage('Aggregator maven build'){
	      environment {
                KEYRING = credentials('secret-subkeys-releng.asc')
                MAVEN_GPG_PASSPHRASE = credentials('secret-subkeys-releng.acs-passphrase')
          }
          steps {
              container('jnlp') {
                    sh \'\'\'
                        cd ${WORKSPACE}/eclipse.platform.releng.aggregator/eclipse.platform.releng.aggregator/cje-production/mbscripts
                        unset JAVA_TOOL_OPTIONS 
                        unset _JAVA_OPTIONS
                        ./mb220_buildSdkPatch.sh $CJE_ROOT/buildproperties.shsource 2>&1 | tee $logDir/mb220_buildSdkPatch.sh.log
                        if [[ ${PIPESTATUS[0]} -ne 0 ]]
                        then
                            echo "Failed in Aggregator maven build stage"
                            exit 1
                        fi
                    \'\'\'
                }
            }
		}
	  stage('Gather Eclipse Parts'){
	      environment {
                KEYRING = credentials('secret-subkeys-releng.asc')
                KEYRING_PASSPHRASE = credentials('secret-subkeys-releng.acs-passphrase')
          }
          steps {
              container('jnlp') {
                      withAnt(installation: 'apache-ant-latest') {
                          sh \'\'\'
                            cd ${WORKSPACE}/eclipse.platform.releng.aggregator/eclipse.platform.releng.aggregator/cje-production/mbscripts
                            bash -x ./mb300_gatherEclipseParts.sh $CJE_ROOT/buildproperties.shsource 2>&1 | tee $logDir/mb300_gatherEclipseParts.sh.log
                            if [[ ${PIPESTATUS[0]} -ne 0 ]]
                            then
                                echo "Failed in Gather Eclipse Parts stage"
                                exit 1
                            fi
                          \'\'\'
                      }
                }
            }
		}
	  stage('Gather Equinox Parts'){
	  environment {
                KEYRING = credentials('secret-subkeys-releng.asc')
                KEYRING_PASSPHRASE = credentials('secret-subkeys-releng.acs-passphrase')
          }
          steps {
              container('jnlp') {
                      withAnt(installation: 'apache-ant-latest') {
                          sh \'\'\'
                            cd ${WORKSPACE}/eclipse.platform.releng.aggregator/eclipse.platform.releng.aggregator/cje-production/mbscripts
                            ./mb310_gatherEquinoxParts.sh $CJE_ROOT/buildproperties.shsource 2>&1 | tee $logDir/mb310_gatherEquinoxParts.sh.log
                            if [[ ${PIPESTATUS[0]} -ne 0 ]]
                            then
                                echo "Failed in Gather Equinox Parts stage"
                                exit 1
                            fi
                          \'\'\'
                      }
                }
            }
		}
	  stage('Generate Repo reports'){
          steps {
              container('jnlp') {
                      sh \'\'\'
                        cd ${WORKSPACE}/eclipse.platform.releng.aggregator/eclipse.platform.releng.aggregator/cje-production/mbscripts
                        unset JAVA_TOOL_OPTIONS 
                        unset _JAVA_OPTIONS
                        ./mb500_createRepoReports.sh $CJE_ROOT/buildproperties.shsource 2>&1 | tee $logDir/mb500_createRepoReports.sh.log
                        if [[ ${PIPESTATUS[0]} -ne 0 ]]
                        then
                            echo "Failed in Generate Repo reports stage"
                            exit 1
                        fi
                      \'\'\'
                }
            }
		}
	  stage('Generate API tools reports'){
          steps {
              container('jnlp') {
                      sh \'\'\'
                        cd ${WORKSPACE}/eclipse.platform.releng.aggregator/eclipse.platform.releng.aggregator/cje-production/mbscripts
                        unset JAVA_TOOL_OPTIONS 
                        unset _JAVA_OPTIONS
                        ./mb510_createApiToolsReports.sh $CJE_ROOT/buildproperties.shsource 2>&1 | tee $logDir/mb510_createApiToolsReports.sh.log
                        if [[ ${PIPESTATUS[0]} -ne 0 ]]
                        then
                            echo "Failed in Generate API tools reports stage"
                            exit 1
                        fi
                      \'\'\'
                }
            }
		}
	  stage('Export environment variables stage 2'){
          steps {
              container('jnlp') {
                script {
                    env.BUILD_IID = sh(script:'echo $(source $CJE_ROOT/buildproperties.shsource;echo $BUILD_TYPE$TIMESTAMP)', returnStdout: true)
                    env.BUILD_VERSION = sh(script:'echo $(source $CJE_ROOT/buildproperties.shsource;echo $RELEASE_VER)', returnStdout: true)
                    env.STREAM = sh(script:'echo $(source $CJE_ROOT/buildproperties.shsource;echo $STREAM)', returnStdout: true)
                    env.COMPARATOR_ERRORS_SUBJECT = sh(script:'echo $(source $CJE_ROOT/buildproperties.shsource;echo $COMPARATOR_ERRORS_SUBJECT)', returnStdout: true)
                    env.COMPARATOR_ERRORS_BODY = sh(script:'echo $(source $CJE_ROOT/buildproperties.shsource;echo $COMPARATOR_ERRORS_BODY)', returnStdout: true)
                    env.POM_UPDATES_SUBJECT = sh(script:'echo $(source $CJE_ROOT/buildproperties.shsource;echo $POM_UPDATES_SUBJECT)', returnStdout: true)
                    env.POM_UPDATES_BODY = sh(script:'echo $(source $CJE_ROOT/buildproperties.shsource;echo $POM_UPDATES_BODY)', returnStdout: true)
                    env.EBUILDER_HASH = sh(script:'echo $(source $CJE_ROOT/buildproperties.shsource;echo $EBUILDER_HASH)', returnStdout: true)
                    env.RELEASE_VER = sh(script:'echo $(source $CJE_ROOT/buildproperties.shsource;echo $RELEASE_VER)', returnStdout: true)
                  }
                }
            }
        }
	  stage('Archive artifacts'){
          steps {
              container('jnlp') {
                sh \'\'\'
                    cd ${WORKSPACE}/eclipse.platform.releng.aggregator/eclipse.platform.releng.aggregator/cje-production
                    source $CJE_ROOT/buildproperties.shsource
                    cp -r $logDir/* $CJE_ROOT/$DROP_DIR/$BUILD_ID/buildlogs
                    rm -rf $logDir
                    rm -rf $CJE_ROOT/$DROP_DIR/$BUILD_ID/apitoolingreference
                    cp $CJE_ROOT/buildproperties.txt $CJE_ROOT/$DROP_DIR/$BUILD_ID
                    cp $CJE_ROOT/buildproperties.php $CJE_ROOT/$DROP_DIR/$BUILD_ID
                    cp $CJE_ROOT/buildproperties.properties $CJE_ROOT/$DROP_DIR/$BUILD_ID
                    cp $CJE_ROOT/buildproperties.shsource $CJE_ROOT/$DROP_DIR/$BUILD_ID
                    cp $CJE_ROOT/$DROP_DIR/$BUILD_ID/buildproperties.* $CJE_ROOT/$EQUINOX_DROP_DIR/$BUILD_ID
                \'\'\'
              }
              archiveArtifacts '**/siteDir/**'
            }
		}
	  stage('Promote Eclipse platform'){
          steps {
              container('jnlp') {
                  sshagent(['projects-storage.eclipse.org-bot-ssh']) {
                      sh \'\'\'
                        cd ${WORKSPACE}/eclipse.platform.releng.aggregator/eclipse.platform.releng.aggregator/cje-production/mbscripts
                        ./mb600_promoteEclipse.sh $CJE_ROOT/buildproperties.shsource
                      \'\'\'
                  }
                }
                build job: 'eclipse.releng.updateIndex', wait: false
            }
		}
	  stage('Promote Equinox'){
          steps {
              container('jnlp') {
                  sshagent(['projects-storage.eclipse.org-bot-ssh']) {
                      sh \'\'\'
                        cd ${WORKSPACE}/eclipse.platform.releng.aggregator/eclipse.platform.releng.aggregator/cje-production/mbscripts
                        ./mb610_promoteEquinox.sh $CJE_ROOT/buildproperties.shsource
                      \'\'\'
                  }
                }
            }
		}
	  stage('Promote Update Site'){
          steps {
              container('jnlp') {
                  sshagent(['projects-storage.eclipse.org-bot-ssh']) {
                      sh \'\'\'
                        cd ${WORKSPACE}/eclipse.platform.releng.aggregator/eclipse.platform.releng.aggregator/cje-production/mbscripts
                        ./mb620_promoteUpdateSite.sh $CJE_ROOT/buildproperties.shsource
                      \'\'\'
                  }
                }
            }
		}
	  stage('Trigger tests'){
          steps {
            container('jnlp') {
              build job: 'AutomatedTests/ep''' + MAJOR + MINOR + '''I-unit-linux-x86_64-java21', parameters: [string(name: 'buildId', value: "${env.BUILD_IID.trim()}")], wait: false
              build job: 'AutomatedTests/ep''' + MAJOR + MINOR + '''I-unit-linux-x86_64-java23', parameters: [string(name: 'buildId', value: "${env.BUILD_IID.trim()}")], wait: false
              build job: 'AutomatedTests/ep''' + MAJOR + MINOR + '''I-unit-macosx-aarch64-java21', parameters: [string(name: 'buildId', value: "${env.BUILD_IID.trim()}")], wait: false
              build job: 'AutomatedTests/ep''' + MAJOR + MINOR + '''I-unit-macosx-x86_64-java21', parameters: [string(name: 'buildId', value: "${env.BUILD_IID.trim()}")], wait: false
              build job: 'AutomatedTests/ep''' + MAJOR + MINOR + '''I-unit-win32-x86_64-java21', parameters: [string(name: 'buildId', value: "${env.BUILD_IID.trim()}")], wait: false
              build job: 'SmokeTests/Start-smoke-tests', parameters: [string(name: 'buildId', value: "${env.BUILD_IID.trim()}")], wait: false
            }
          }
		}
		stage('Trigger publication to Maven snapshots repo') {
			when {
				environment name: 'COMPARATOR_ERRORS_SUBJECT', value: ''
				// On comparator-erros, skip the deployment of snapshot version to the 'eclipse-snapshots' maven repository to prevent that ECJ snapshot
				// from being used in verification builds. Similar to how the p2-repository is not added to the I-build composite in that case.
			}
			steps {
              container('jnlp') {
				build job: 'CBIaggregator', parameters: [string(name: 'snapshotOrRelease', value: '-snapshot')], wait: false
              }
			}
		}
	}
	post {
        failure {
            emailext body: "Please go to <a href='${BUILD_URL}console'>${BUILD_URL}console</a> and check the build failure.<br><br>",
            subject: "${env.BUILD_VERSION} I-Build: ${env.BUILD_IID.trim()} - BUILD FAILED", 
            to: "platform-releng-dev@eclipse.org",
            from:"genie.releng@eclipse.org"
            archive '${CJE_ROOT}/siteDir/eclipse/downloads/drops4/${env.BUILD_IID.trim()}/gitLog.html, $CJE_ROOT/gitCache/eclipse.platform.releng.aggregator'
        }
        success {
            emailext body: "Eclipse downloads:<br>    <a href='https://download.eclipse.org/eclipse/downloads/drops4/${env.BUILD_IID.trim()}'>https://download.eclipse.org/eclipse/downloads/drops4/${env.BUILD_IID.trim()}</a><br><br> Build logs and/or test results (eventually):<br>    <a href='https://download.eclipse.org/eclipse/downloads/drops4/${env.BUILD_IID.trim()}/testResults.php'>https://download.eclipse.org/eclipse/downloads/drops4/${env.BUILD_IID.trim()}/testResults.php</a><br><br>${env.POM_UPDATES_BODY.trim()}${env.COMPARATOR_ERRORS_BODY.trim()}Software site repository:<br>    <a href='https://download.eclipse.org/eclipse/updates/${env.RELEASE_VER.trim()}-I-builds'>https://download.eclipse.org/eclipse/updates/${env.RELEASE_VER.trim()}-I-builds</a><br><br>Specific (simple) site repository:<br>    <a href='https://download.eclipse.org/eclipse/updates/${env.RELEASE_VER.trim()}-I-builds/${env.BUILD_IID.trim()}'>https://download.eclipse.org/eclipse/updates/${env.RELEASE_VER.trim()}-I-builds/${env.BUILD_IID.trim()}</a><br><br>Equinox downloads:<br>     <a href='https://download.eclipse.org/equinox/drops/${env.BUILD_IID.trim()}'>https://download.eclipse.org/equinox/drops/${env.BUILD_IID.trim()}</a><br><br>", 
            subject: "${env.BUILD_VERSION} I-Build: ${env.BUILD_IID.trim()} ${env.POM_UPDATES_SUBJECT.trim()} ${env.COMPARATOR_ERRORS_SUBJECT.trim()}", 
            to: "platform-releng-dev@eclipse.org",
            from:"genie.releng@eclipse.org"
        }
	}
}

        ''')
      }
    }
  }
}
