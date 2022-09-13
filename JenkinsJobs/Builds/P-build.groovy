pipelineJob('Builds/P-build'){
  description('Java Update Builds CHECK NOTES.')

  triggers {
    cron('''
      TZ=America/Toronto
      # format: Minute Hour Day Month Day of the week (0-7)

      #Daily P-build
      #0 5 * * *
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
      script(readFileFromWorkspace('JenkinsJobs/Builds/P-build.jenkinsfile'))
    }
  }
}