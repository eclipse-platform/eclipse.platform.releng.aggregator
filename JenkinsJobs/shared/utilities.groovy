
@groovy.transform.Field
def boolean IS_DRY_RUN = true

def setDryRun(boolean isDryRun) {
	IS_DRY_RUN = isDryRun
}

def replaceInFile(String filePath, Map<String,String> replacements) {
	replaceAllInFile(filePath, replacements.collectEntries{ k, v -> [java.util.regex.Pattern.quote(k), v] });
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

return this
