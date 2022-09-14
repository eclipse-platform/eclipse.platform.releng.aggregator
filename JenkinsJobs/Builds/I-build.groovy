def config = new groovy.json.JsonSlurper().parseText(readFileFromWorkspace('${WORKSPACE}/JenkinsJobs/JobDSL.json'))
def STREAMS = config.Streams

for (STREAM in STREAMS){
  def BRANCH = config.Branches.STREAM

  pipelineJob('Builds/I-build-' + STREAM){
    description('Daily Eclipse Integration builds.')

    triggers {
      cron('''
        TZ=America/Toronto
        # format: Minute Hour Day Month Day of the week (0-7)

        # - - - Integration Eclipse SDK builds - - - 
        # Normal : 6 PM every day 
        0 18 * * *
        # rebuilds
        #0 12 23 3 3

        # - - - Milestone week/RC weeks - - - 
        # Post M3, no nightlies, I-builds only. (Be sure to "turn off" for tests and sign off days)
        #0 6 * * 6,7,1,2,3
        #0 18 * * 5,6,7,1,2,3
        # rebuilds
        #45 2 07 04 4
      ''')
    }

    logRotator {
      numToKeep(25)
    }

    definition {
      cpsScm {
        lightweight(true)
        scm {
          github('https://github.com/eclipse-platform/eclipse.platform.releng.aggregator/', BRANCH)
        }
      }

      cps {
        sandbox()
        script(readFileFromWorkspace('JenkinsJobs/Builds/I-build.jenkinsfile'))
      }
    }
  }
}
