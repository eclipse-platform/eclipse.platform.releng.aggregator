pipelineJob('AutomatedTests/ep426I-unit-cen64-gtk3-java11'){

  logRotator {
    numToKeep(5)
  }

  parameters {
    stringParam('buildId', null, null)
    stringParam('javaDownload', 'https://download.java.net/java/GA/jdk11/9/GPL/openjdk-11.0.2_linux-x64_bin.tar.gz', null)
  }

  definition {
    cps {
      sandbox()
      script('''
pipeline {
	options {
		timeout(time: 600, unit: 'MINUTES')
		timestamps()
		buildDiscarder(logRotator(numToKeepStr:'5'))
	}
  agent {
    kubernetes {
      label 'centos-unitpodJava11'
      defaultContainer 'custom'
      yaml """
apiVersion: v1
kind: Pod
spec:
  containers:
  - name: "jnlp"
    resources:
      requests:
        cpu: "100m"
        memory: "1024Mi"
      limits:
        cpu: "100m"
        memory: "1024Mi"
  - name: "custom"
    image: "eclipse/platformreleng-centos-gtk3-metacity:8"
    imagePullPolicy: "Always"
    resources:
      limits:
        memory: "6144Mi"
        cpu: "2000m"
      requests:
        memory: "512Mi"
        cpu: "1000m"
    securityContext:
      privileged: false
    tty: true
    command:
    - cat
    volumeMounts:
    - mountPath: "/opt/tools"
      name: "volume-0"
      readOnly: false
    workingDir: "/home/jenkins/agent"
  nodeSelector: {}
  restartPolicy: "Never"
  volumes:
  - name: "volume-0"
    persistentVolumeClaim:
      claimName: "tools-claim-jiro-releng"
      readOnly: true
  - configMap:
      name: "known-hosts"
    name: "volume-1"
  - emptyDir:
      medium: ""
    name: "workspace-volume"
  - emptyDir:
      medium: ""
    name: "volume-3"
"""
    }
  }

  stages {
      stage('Run tests'){
          steps {
              container ('custom'){
                  wrap([$class: 'Xvnc', takeScreenshot: false, useXauthority: true]) {
                      withEnv(["JAVA_HOME_NEW=${ tool 'adoptopenjdk-hotspot-latest-lts' }"]) {
                          withAnt(installation: 'apache-ant-latest') {
                              sh \'\'\'#!/bin/bash -x
                                
                                buildId=$(echo $buildId|tr -d ' ')
                                RAW_DATE_START="$(date +%s )"
                                
                                export LANG=en_US.UTF-8
                                cat /etc/*release
                                echo -e "\\n\\tRAW Date Start: ${RAW_DATE_START} \\n"
                                echo -e "\\n\\t whoami:  $( whoami )\\n"
                                echo -e "\\n\\t uname -a: $(uname -a)\\n"
                                
                                # 0002 is often the default for shell users, but it is not when ran from
                                # a cron job, so we set it explicitly, to be sure of value, so releng group has write access to anything
                                # we create on shared area.
                                oldumask=$(umask)
                                umask 0002
                                
                                echo "umask explicitly set to 0002, old value was $oldumask"
                                
                                # we want java.io.tmpdir to be in $WORKSPACE, but must already exist, for Java to use it.
                                mkdir -p ${WORKSPACE}/tmp
                                
                                wget -O ${WORKSPACE}/getEBuilder.xml --no-verbose --no-check-certificate https://download.eclipse.org/eclipse/relengScripts/production/testScripts/hudsonBootstrap/getEBuilder.xml 2>&1
                                cat ${WORKSPACE}/getEBuilder.xml
                                wget -O ${WORKSPACE}/buildproperties.shsource --no-check-certificate https://download.eclipse.org/eclipse/downloads/drops4/${buildId}/buildproperties.shsource
                                cat ${WORKSPACE}/buildproperties.shsource
                                source ${WORKSPACE}/buildproperties.shsource
                                
                                set -x
                                mkdir -p ${WORKSPACE}/java
                                pushd ${WORKSPACE}/java
                                wget -O jdk.tar.gz --no-verbose ${javaDownload}
                                tar xzf jdk.tar.gz
                                rm jdk.tar.gz
                                export JAVA_HOME_NEW=$(pwd)/$(ls)
                                popd
                                set +x
                                
                                export PATH=${JAVA_HOME_NEW}/bin:${ANT_HOME}/bin:${PATH} 
                                
                                echo JAVA_HOME: $JAVA_HOME
                                export JAVA_HOME=$JAVA_HOME_NEW
                                echo ANT_HOME: $ANT_HOME
                                echo PATH: $PATH
                                export ANT_OPTS="${ANT_OPTS} -Djava.io.tmpdir=${WORKSPACE}/tmp"
                                
                                env 1>envVars.txt 2>&1
                                ant -diagnostics 1>antDiagnostics.txt 2>&1
                                java -XshowSettings -version 1>javaSettings.txt 2>&1
                                
                                ant -f getEBuilder.xml -Djava.io.tmpdir=${WORKSPACE}/tmp -DbuildId=$buildId  -DeclipseStream=$STREAM -DEBUILDER_HASH=${EBUILDER_HASH}  -DdownloadURL=http://download.eclipse.org/eclipse/downloads/drops4/${buildId}  -Dosgi.os=linux -Dosgi.ws=gtk -Dosgi.arch=x86_64 -DtestSuite=all -Djvm=${JAVA_HOME}/bin/java
                                
                                RAW_DATE_END="$(date +%s )"
                                
                                echo -e "\\n\\tRAW Date End: ${RAW_DATE_END} \\n"
                                
                                TOTAL_TIME=$((${RAW_DATE_END} - ${RAW_DATE_START}))
                                
                                echo -e "\\n\\tTotal elapsed time: ${TOTAL_TIME} \\n"
                              \'\'\'
                          }
                      }
                  }
                  junit keepLongStdio: true, testResults: '**/eclipse-testing/results/xml/*.xml'
              }
              archiveArtifacts '**/eclipse-testing/results/**, **/eclipse-testing/directorLogs/**, *.properties, *.txt'
              build job: 'Releng/ep-collectResults', parameters: [string(name: 'triggeringJob', value: "${JOB_BASE_NAME}"), string(name: 'triggeringBuildNumber', value: "${BUILD_NUMBER}"), string(name: 'buildId', value: "${params.buildId}")], wait: false
          }
      }
  }
}
      ''')
    }
  }
}
