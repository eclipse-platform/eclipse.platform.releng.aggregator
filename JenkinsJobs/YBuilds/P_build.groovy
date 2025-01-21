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
      inheritFrom 'ubuntu-2404'
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
		jdk 'temurin-jdk21-latest'
		maven 'apache-maven-latest'
		ant 'apache-ant-latest'
	}
  environment {
      BUILD_TYPE = 'P'
      BUILD_TYPE_NAME = 'Beta Java 24'
      PATCH_OR_BRANCH_LABEL = 'java24patch'
      PATCH_BUILD="${PATCH_OR_BRANCH_LABEL}"
      
      MAVEN_OPTS = '-Xmx4G'
      CJE_ROOT = "${WORKSPACE}/eclipse.platform.releng.aggregator/eclipse.platform.releng.aggregator/cje-production"
      PATH = "$PATH:/opt/tools/apache-maven/latest/bin"
      logDir = "$CJE_ROOT/buildlogs"
    }
  
  stages {
	  stage('Setup intial configuration'){
          steps {
                  sshagent(['github-bot-ssh']) {
                      dir ('eclipse.platform.releng.aggregator') {
                        sh \'\'\'
                            git clone -b master git@github.com:eclipse-platform/eclipse.platform.releng.aggregator.git
                        \'\'\'
                      }
                    }
                    dir("${CJE_ROOT}") {
                    sh \'\'\'
                        chmod +x mbscripts/*
                        mkdir -p $logDir
                    \'\'\'
                }
            }
		}
	  stage('Generate environment variables'){
          steps {
              dir("${CJE_ROOT}/mbscripts") {
                sh \'\'\'
                    set -eo pipefail
                    ./mb010_createEnvfiles.sh $CJE_ROOT/buildproperties.shsource 2>&1 | tee $logDir/mb010_createEnvfiles.sh.log
                \'\'\'
                }
            }
		}
	  stage('Export environment variables stage 1'){
          steps {
                script {
                    env.BUILD_IID = sh(script:'echo $(source $CJE_ROOT/buildproperties.shsource;echo $BUILD_TYPE$TIMESTAMP)', returnStdout: true).trim()
                    env.RELEASE_VER = sh(script:'echo $(source $CJE_ROOT/buildproperties.shsource;echo $RELEASE_VER)', returnStdout: true).trim()
                  }
            }
        }
	  stage('Clone Repositories'){
          steps {
              dir("${CJE_ROOT}/mbscripts") {
                  sshagent(['github-bot-ssh']) {
                    sh \'\'\'
                        set -eo pipefail
                        git config --global user.email "eclipse-releng-bot@eclipse.org"
                        git config --global user.name "Eclipse Releng Bot"
                        ./mb100_cloneRepos.sh $CJE_ROOT/buildproperties.shsource 2>&1 | tee $logDir/mb100_cloneRepos.sh.log
                    \'\'\'
                  }
                }
            }
		}
	  stage('Tag Build Inputs'){
          steps {
              dir("${CJE_ROOT}/mbscripts") {
                  sshagent (['github-bot-ssh', 'projects-storage.eclipse.org-bot-ssh']) {
                    sh \'\'\'
                        set -xeo pipefail
                        ./mb110_tagBuildInputs.sh $CJE_ROOT/buildproperties.shsource 2>&1 | tee $logDir/mb110_tagBuildInputs.sh.log
                    \'\'\'
                  }
                }
            }
		}
		stage('Aggregator maven build'){
          steps {
              dir("${CJE_ROOT}/mbscripts") {
                    sh \'\'\'
                        set -eo pipefail
                        ./mb220_buildSdkPatch.sh $CJE_ROOT/buildproperties.shsource 2>&1 | tee $logDir/mb220_buildSdkPatch.sh.log
                    \'\'\'
                }
            }
		}
	  stage('Gather Eclipse Parts'){
          steps {
              dir("${CJE_ROOT}/mbscripts") {
                          sh \'\'\'
                            set -xeo pipefail
                            ./mb300_gatherEclipseParts.sh $CJE_ROOT/buildproperties.shsource 2>&1 | tee $logDir/mb300_gatherEclipseParts.sh.log
                          \'\'\'
                }
            }
		}
	  stage('Promote Update Site'){
          steps {
              dir("${CJE_ROOT}/mbscripts") {
                  sshagent(['projects-storage.eclipse.org-bot-ssh']) {
                      sh \'\'\'
                        ./mb620_promoteUpdateSite.sh $CJE_ROOT/buildproperties.shsource
                      \'\'\'
                  }
                }
            }
		}
	}
	post {
		always {
			archiveArtifacts 'cje-production/siteDir/**'
		}
        failure {
            subject: subject: "${RELEASE_VER} ${BUILD_TYPE}-Build: ${BUILD_IID} - BUILD FAILED",
            body: "Please go to ${BUILD_URL}console and check the build failure.", mimeType: 'text/plain',
            to: 'jdt-dev@eclipse.org', from: 'genie.releng@eclipse.org'
        }
        success {
            subject: "${RELEASE_VER} ${BUILD_TYPE}-Build: ${BUILD_IID}",
            emailext body: """
            Software site repository:
            https://download.eclipse.org/eclipse/updates/${RELEASE_VER}-P-builds
            
            Specific (simple) site repository
            https://download.eclipse.org/eclipse/updates/${RELEASE_VER}-P-builds/${BUILD_IID}
            "",
            to: 'jdt-dev@eclipse.org', from: 'genie.releng@eclipse.org'
        }
	}
}

      ''')
    }
  }
}
