listView('Builds') {
  description('Eclipse periodic build jobs.')

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
    regex('^I-build(.*)')
    regex('^Y-build(.*)')
    regex('^AddTo(.*)')
    name('P-build')
    name('markStable')
    name('markUnstable')
  }

}