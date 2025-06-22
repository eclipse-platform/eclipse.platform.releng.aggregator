import groovy.json.JsonOutput

@groovy.transform.Field
def boolean IS_DRY_RUN = true

def setDryRun(boolean isDryRun) {
	IS_DRY_RUN = isDryRun
}

def replaceAllInFile(String filePath, Map<String,String> replacements) {
	def content = readFile(filePath)
	for (entry in replacements) {
		def newContent = content.replaceAll(entry.key, entry.value)
		if (newContent == content && !(content =~ entry.key)) { // pattern matches, but the replacement is equal to the current content
			error("Pattern not found in file '${filePath}': ${entry.key}")
		}
		content = newContent
	}
	writeFile(file:filePath, text: content)
}

def modifyJSON(String jsonFilePath, Closure transformation) {
	def json = readJSON(file: jsonFilePath)
	transformation.call(json)
	// This leads to prettier results than using the writeJSON() step, even with the pretty parameter set.
	writeFile(file: jsonFilePath, text: JsonOutput.prettyPrint(JsonOutput.toJson(json)), encoding :'UTF-8')
}

def runHereAndForEachGitSubmodule(Closure task) {
	task.call()
	forEachGitSubmodule(task)
}

def forEachGitSubmodule(Closure task) {
	def submodulePaths = sh(script: "git submodule --quiet foreach 'echo \$sm_path'", returnStdout: true).trim().split('\\s')
	for (submodulePath in submodulePaths) {
		dir("${submodulePath}") {
			task.call(submodulePath)
		}
	}
}

def gitCommitAllExcludingSubmodules(String commitMessage) {
	withEnv(["COMMIT_MESSAGE=${commitMessage}"]) {
		sh '''
			#Commit all changes, except for the updated sub-modules here
			git add --all
			git restore --staged $(git submodule foreach --quiet 'echo $sm_path')
			git commit --message "${COMMIT_MESSAGE}"
		'''
	}
}

def gitPushBranch(String localBranch, String remoteTargetBranch, boolean force = false) {
	gitPush("${localBranch}:refs/heads/${remoteTargetBranch}", force)
}

def gitPushTag(String tagName, boolean force = false) {
	gitPush("tag ${tagName}", force)
}

private void gitPush(String refSpec, boolean force) {
	def dryRunOption = IS_DRY_RUN ? '--dry-run' : ''
	def forceOption = force ? '--force' : ''
	sshagent(['github-bot-ssh']) {
		sh """#!/bin/bash -xe
			# Switch to SSH, if the configured URL uses HTTPS (we can only push with SSH)
			pushURL=\$(git config remote.origin.url | sed --expression 's|https://github.com/|git@github.com:|')
			git push ${dryRunOption} ${forceOption} "\${pushURL}" ${refSpec}
		"""
	}
}

def List<String> listBuildDropDirectoriesOnRemote(String remoteDirectory, String dropNamePattern = "*", int major = 0, int minor = 0, int service = 0) {
	def versionFilter = major == 0 ? '' :"""\
		| xargs grep -l 'STREAMMinor=\\"${minor}\\"' \
		| xargs grep -l 'STREAMMajor=\\"${major}\\"' \
		| xargs grep -l 'STREAMService=\\"${service}\\"' \
	""".trim()
	def result = sh(script: """ssh genie.releng@projects-storage.eclipse.org "cd ${remoteDirectory} && \
		find -maxdepth 2 -type f -path './${dropNamePattern}/buildproperties.txt' \
		${versionFilter} | xargs dirname | sort -u"
	""", returnStdout: true).trim()
	return result.isEmpty() ? [] : result.split('\\s+').collect{ d -> d.startsWith('./') ? d.substring(2) : d }
}

private void removeDropsOnRemote(String remoteDirectory, List<String> drops) {
	def paths = drops.collect{ r -> "${remoteDirectory}/${r}"}.join(' ')
	// The main portion of the script is executed on the storage server
	// References to locally defined variables required double escaping,
	// they are escaped for Groovy and the string in the script as they are to be interpreted at the target server.
	// In general this section is very fragile! Only change with great caution and extensive testing!
	sh """ssh genie.releng@projects-storage.eclipse.org "
		for dropDir in ${paths}; do
			if [ ! -d \\"\\\${dropDir}\\" ]; then
				echo \\"Skip not existing directory: \\\${dropDir}\\"
			elif [ ! -f \\"\\\${dropDir}/buildKeep\\" ]; then
				echo \\"Remove directory \\\${dropDir}\\"
				${ IS_DRY_RUN ? 'echo __' : ''}rm -rf \\"\\\${dropDir}\\"
			else
				echo \\"Keep drop marked to be kept: \\\${dropDir}\\"
			fi
		done "
	"""
}

return this
