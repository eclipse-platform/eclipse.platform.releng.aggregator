pipelineJob('YPBuilds/P-build'){
  description('Java Update Builds.')

  properties {
    pipelineTriggers {
      triggers {
        cron {
          spec("""TZ=America/Toronto
# format: Minute Hour Day Month Day of the week (0-7)

#Daily P-build
#0 5 * * *
          """)
        }
      }
    }
  }

  logRotator {
    numToKeep(25)
  }

  definition {
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
      inheritFrom 'centos-8'
      yaml """
apiVersion: v1
kind: Pod
spec:
  containers:
  - name: "jnlp"
    resources:
      limits:
        memory: "8192Mi"
        cpu: "4000m"
      requests:
        memory: "6144Mi"
        cpu: "2000m"
"""
    }
  }
  tools {
      jdk 'openjdk-jdk17-latest'
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
                            git clone -b R4_33_maintenance git@github.com:eclipse-platform/eclipse.platform.releng.aggregator.git
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
                    cp ../P-build/buildproperties.txt ../buildproperties.txt
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
                    env.RELEASE_VER = sh(script:'echo $(source $CJE_ROOT/buildproperties.shsource;echo $RELEASE_VER)', returnStdout: true)
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
	  stage('Copy build scripts for P-build'){
          steps {
              container('jnlp') {
                    sh \'\'\'
                        cd ${WORKSPACE}/eclipse.platform.releng.aggregator/eclipse.platform.releng.aggregator/cje-production/P-build
                        cp mb220_buildSdkPatch.sh ${WORKSPACE}/eclipse.platform.releng.aggregator/eclipse.platform.releng.aggregator/cje-production/gitCache/eclipse.platform.releng.aggregator/cje-production/mbscripts/.
                        cp mb220_buildSdkPatch.sh ${WORKSPACE}/eclipse.platform.releng.aggregator/eclipse.platform.releng.aggregator/cje-production/mbscripts/.
                        cp mb300_gatherEclipseParts.sh ${WORKSPACE}/eclipse.platform.releng.aggregator/eclipse.platform.releng.aggregator/cje-production/gitCache/eclipse.platform.releng.aggregator/cje-production/mbscripts/.
                        cp mb300_gatherEclipseParts.sh ${WORKSPACE}/eclipse.platform.releng.aggregator/eclipse.platform.releng.aggregator/cje-production/mbscripts/.
                        cp mb620_promoteUpdateSite.sh ${WORKSPACE}/eclipse.platform.releng.aggregator/eclipse.platform.releng.aggregator/cje-production/gitCache/eclipse.platform.releng.aggregator/cje-production/mbscripts/.
                        cp mb620_promoteUpdateSite.sh ${WORKSPACE}/eclipse.platform.releng.aggregator/eclipse.platform.releng.aggregator/cje-production/mbscripts/.
                    \'\'\'
                }
            }
		}
		stage('Aggregator maven build'){
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
	  stage('Export environment variables stage 2'){
          steps {
              container('jnlp') {
                script {
                    env.BUILD_IID = sh(script:'echo $(source $CJE_ROOT/buildproperties.shsource;echo $BUILD_TYPE$TIMESTAMP)', returnStdout: true)
                    env.RELEASE_VER = sh(script:'echo $(source $CJE_ROOT/buildproperties.shsource;echo $RELEASE_VER)', returnStdout: true)
                  }
                }
            }
        }
	  stage('Archive artifacts'){
          steps {
              archiveArtifacts '**/siteDir/**'
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
	}
	post {
        failure {
            emailext body: "Please go to <a href='${BUILD_URL}console'>${BUILD_URL}console</a> and check the build failure.<br><br>",
            subject: "${env.RELEASE_VER.trim()} P-Build: ${env.BUILD_IID.trim()} - BUILD FAILED", 
            to: "rahul.mohanan@ibm.com jarthana@in.ibm.com Sheena.Sheena@ibm.com alshama.m.s@ibm.com elsa.zacharia@ibm.com suby.surendran@ibm.com gpunathi@in.ibm.com sravankumarl@in.ibm.com kalyan_prasad@in.ibm.com lshanmug@in.ibm.com manoj.palat@in.ibm.com niraj.modi@in.ibm.com noopur_gupta@in.ibm.com sarika.sinha@in.ibm.com vikas.chandra@in.ibm.com",
            from:"genie.releng@eclipse.org"
        }
        success {
            emailext body: "Software site repository:<br>    <a href='https://download.eclipse.org/eclipse/updates/${env.RELEASE_VER.trim()}-P-builds'>https://download.eclipse.org/eclipse/updates/${env.RELEASE_VER.trim()}-P-builds</a><br><br>Specific (simple) site repository:<br>    <a href='https://download.eclipse.org/eclipse/updates/${env.RELEASE_VER.trim()}-P-builds/${env.BUILD_IID.trim()}'>https://download.eclipse.org/eclipse/updates/${env.RELEASE_VER.trim()}-P-builds/${env.BUILD_IID.trim()}</a><br><br>", 
            subject: "${env.RELEASE_VER.trim()} P-Build: ${env.BUILD_IID.trim()}", 
            to: "rahul.mohanan@ibm.com jarthana@in.ibm.com Sheena.Sheena@ibm.com alshama.m.s@ibm.com elsa.zacharia@ibm.com suby.surendran@ibm.com gpunathi@in.ibm.com sravankumarl@in.ibm.com kalyan_prasad@in.ibm.com lshanmug@in.ibm.com manoj.palat@in.ibm.com niraj.modi@in.ibm.com noopur_gupta@in.ibm.com sarika.sinha@in.ibm.com vikas.chandra@in.ibm.com",
            from:"genie.releng@eclipse.org"
        }
	}
}

      ''')
    }
  }
}
