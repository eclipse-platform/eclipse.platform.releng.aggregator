
def cmd = "ls JenkinsJobs/Releng"
def relengJobs = cmd.execute().text

listView('Releng') {
  description('Jobs related to routine releng tasks. Some are periodic, some are "manual" jobs ran only when needed.')

  columns {
    status()
    weather()
    name()
    lastSuccess()
    lastFailure()
    lastDuration()
    buildButton()
  }

  filterExecutors()

  jobs {
    for (job in relengJobs) {
      def jobName = job.split('.')
      //if not FOLDER or VIEW
      name(jobName[0])
    }
  }

}

listView('Publish to Maven') {
  description('List of jobs used to publish Maven artifacts to repo.eclipse.org (snapshots) or Maven Central (release)')

  columns {
    status()
    weather()
    name()
    lastSuccess()
    lastFailure()
    lastDuration()
    buildButton()
  }

  filterExecutors()

  jobs {
    name('Releng/CBIaggregator')
    name('Releng/PublishJDTtoMaven')
    name('Releng/PublishPDEToMaven')
    name('Releng/PublishPlatformToMaven')
  }
}