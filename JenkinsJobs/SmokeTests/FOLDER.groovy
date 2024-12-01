folder('SmokeTests') {
  displayName('Smoke Tests')
  description('Folder for Smoke Tests')
}

pipelineJob('SmokeTests/Start-smoke-tests'){
	description('Start all smoke tests for the Eclipse SDK')
	parameters {  // Define parameters in job configuration to make them already available in the very first build
		stringParam {
			name('buildId')
			description('Build Id to test (such as I20240611-1800, N20120716-0800).')
			trim(true)
		}
		stringParam {
			name('java17x64')
			defaultValue('https://download.java.net/java/GA/jdk17.0.2/dfd4a8d0985749f896bed50d7138ee7f/8/GPL/openjdk-17.0.2_linux-x64_bin.tar.gz')
			description('Fully qualified path for java 17 for x84_64 linux')
			trim(true)
		}
		stringParam {
			name('java17ppcle')
			defaultValue('https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.4%2B8/OpenJDK17U-jdk_ppc64le_linux_hotspot_17.0.4_8.tar.gz')
			description('Fully qualified path for java 17 for ppc64le linux')
			trim(true)
		}
		stringParam {
			name('java17arm64')
			defaultValue('https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.4%2B8/OpenJDK17U-jdk_aarch64_linux_hotspot_17.0.4_8.tar.gz')
			description('Fully qualified path for java 17 for arm64 linux')
			trim(true)
		}
		stringParam {
			name('java21x64')
			defaultValue('https://download.java.net/java/GA/jdk21/fd2272bbf8e04c3dbaee13770090416c/35/GPL/openjdk-21_linux-x64_bin.tar.gz')
			description('Fully qualified path for java 21 for x84_64 linux')
			trim(true)
		}
		stringParam {
			name('java21arm64')
			defaultValue('https://download.java.net/java/GA/jdk21/fd2272bbf8e04c3dbaee13770090416c/35/GPL/openjdk-21_linux-aarch64_bin.tar.gz')
			description('Fully qualified path for java 21 for arm64 linux')
			trim(true)
		}
		stringParam {
			name('java23x64')
			defaultValue('https://download.java.net/java/GA/jdk23/3c5b90190c68498b986a97f276efd28a/37/GPL/openjdk-23_linux-x64_bin.tar.gz')
			description('Fully qualified path for java 23 for x84_64 linux')
			trim(true)
		}
		stringParam {
			name('java23arm64')
			defaultValue('https://download.java.net/java/GA/jdk23/3c5b90190c68498b986a97f276efd28a/37/GPL/openjdk-23_linux-aarch64_bin.tar.gz')
			description('Fully qualified path for java 23 for arm64 linux')
			trim(true)
		}
	}
	definition {
		cpsScm {
			lightweight(true)
			scm {
				github('eclipse-platform/eclipse.platform.releng.aggregator', 'master')
			}
			scriptPath('JenkinsJobs/SmokeTests/StartSmokeTests.jenkinsfile')
		}
	}
}
