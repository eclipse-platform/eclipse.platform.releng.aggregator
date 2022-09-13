pipelineJob('Builds/Y-build'){
  description('Daily Maintenance Builds.')

  triggers {
    cron('''
      TZ=America/Toronto
      # format: Minute Hour Day Month Day of the week (0-7)

      #Daily Y-build
      0 10 * * *
      #milestone week
      #0 6 * * 2
      #0 6 * * 4
      #
      #0 2 21 7 4
    ''')
  }

  logRotator {
    numToKeep(25)
  }

  definition {
    cpsScm {
      lightweight(true)
      scm {
        github('https://github.com/eclipse-platform/eclipse.platform.releng.aggregator/', 'R4_25_maintenance')
      }
    }

    cps {
      sandbox()
      script(readFileFromWorkspace('JenkinsJobs/Builds/Y-build.jenkinsfile'))
    }
  }
}
