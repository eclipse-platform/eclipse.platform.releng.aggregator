job('Releng/deployPlatformParentPom'){
  displayName('Deploy Eclipse Platform Parent Pom')
  description('''Deploys the Eclipse Platform parent pom to repo.eclipse.org on an hourly. Or, if immediate update needed, contact releng team at platform-releng-dev@eclipse.org to deploy at other times.

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
      branch('R4_7_maintenance')
      branch('R4_8_maintenance')
      branch('R4_19_maintenance')
      branch('R4_23_maintenance')
      branch('R4_25_maintenance')
      branch('R4_26_maintenance')
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
      rootPOM('eclipse-platform-parent/pom.xml')
    }
  }

  publishers {
    archiveArtifacts {
      pattern('eclipse-platform-parent/pom.xml')
    }
  }
}
