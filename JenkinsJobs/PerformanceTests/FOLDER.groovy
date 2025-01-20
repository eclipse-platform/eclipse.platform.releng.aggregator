def config = new groovy.json.JsonSlurper().parseText(readFileFromWorkspace('JenkinsJobs/JobDSL.json'))
def STREAMS = config.Streams

folder('PerformanceTests') {
  displayName('Performance Tests')
  description('Folder for Performance Tests')
}

for (STREAM in STREAMS){
	def MAJOR = STREAM.split('\\.')[0]
	def MINOR = STREAM.split('\\.')[1]
	pipelineJob('PerformanceTests/ep' + MAJOR + MINOR + '-perf-tests'){ // The result collecting scripts rely on the '-perf-' part in the name
		displayName('ep' + MAJOR + MINOR + '-Performance-Tests')
		description('Run performance tests')
		// Define parameters in job configuration to make them already available in the very first build
		parameters {
			stringParam('buildId', null, 'Build ID to test, such as I20140821-0800 or M20140822-0800')
			stringParam('testToRun', 'selectPerformance', """
Name of test suite (or test suite collection) to run. 
Collections:
selectPerformance (a group of tests that complete in about 3 hours)
otherPerformance  (a small group of tests that either are not working, or take greater than one hour each). 

Individual Tests Suites, per collection: 
selectPerformance:

antui
compare
coreresources
coreruntime
jdtdebug
jdtui
osgi
pdeui
swt
teamcvs
ua
uiforms
uiperformance
uircp

otherPerformance:
equinoxp2ui
pdeapitooling
jdtcoreperf
jdttext
jdtuirefactoring
""")
		}
		definition {
			cpsScm {
				lightweight(true)
				scm {
					github('eclipse-platform/eclipse.platform.releng.aggregator', 'master')
				}
				scriptPath('JenkinsJobs/PerformanceTests/PerformanceTests.jenkinsfile')
			}
		}
	}
}
