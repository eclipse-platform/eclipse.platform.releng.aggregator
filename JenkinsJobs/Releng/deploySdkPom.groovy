job('Releng/deploySdkPom'){
  displayName('Deploy Eclipse Platform SDK Pom')
  description('''Deploys the Eclipse Platform releng sdk pom to repo.eclipse.org on an hourly. Or, if immediate update needed, contact releng team at platform-releng-dev@eclipse.org to deploy at other times.

For this to be used by consuming projects, they must have their local maven repo in their workspace and "clean it" to get the latest SNAPSHOTs, or, alternatively, they need to specify --update-snapshots in their maven parameters. (Hudson drop down about SNAPSHOTS of "FORCE" corresponds to --update-snapshots).
''')

  logRotator {
    numToKeep(25)
  }

  properties {
    githubProjectUrl('https://github.com/eclipse-platform/eclipse.platform.releng.aggregator/')
  }

  concurrentBuild(true)

  jdk('openjdk-jdk17-latest')

  scm {
    git {
      remote{
        url('https://github.com/eclipse-platform/eclipse.platform.releng.aggregator.git')
      }
      branch('master')
      branch('R4_32_maintenance')
    }
  }

  triggers {
    gitHubPushTrigger()
    pollSCM {
      scmpoll_spec('@hourly')
    }
  }

  wrappers { //adds pre/post actions
    preBuildCleanup()
    timestamps()
        buildTimeoutWrapper{
      strategy {
        absoluteTimeOutStrategy {
          timeoutMinutes('60')
        }
        timeoutEnvVar('')
      }
    }
  }
  
  steps {
    maven {
      mavenInstallation('apache-maven-latest')
      goals('deploy')
      rootPOM('eclipse.platform.releng.prereqs.sdk/pom.xml')
    }
  }

  publishers {
    archiveArtifacts {
      pattern('eclipse.platform.releng.prereqs.sdk/pom.xml')
    }
  }
}
