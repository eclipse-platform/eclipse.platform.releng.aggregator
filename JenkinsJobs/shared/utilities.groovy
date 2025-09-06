
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

return this
