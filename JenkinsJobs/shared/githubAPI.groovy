
@groovy.transform.Field
def boolean IS_DRY_RUN = false

def setDryRun(boolean isDryRun) {
	IS_DRY_RUN = isDryRun
}

/** Returns a list of all repositories in the specified organization.*/
def listReposOfOrganization(String orga) {
	def response = queryGithubAPI('', "orgs/${orga}/repos", null)
	if (!(response instanceof List) && (response.errors || (response.status && response.status != 201))) {
		error "Response contains errors:\n${response}"
	}
	return response
}

/**
 * Create a new milestone.
 * @param msDueDay the milestone's due-date, format: YYYY-MM-DD
 */
def createMilestone(String orga, String repo, String msTitle, String msDescription, String msDueDay) {
	echo "In ${orga}/${repo} create milestone: ${msTitle} due on ${msDueDay}"
	def params = [title: msTitle, description: msDescription, due_on: "${msDueDay}T23:59:59Z"]
	def response = queryGithubAPI('-X POST', "repos/${orga}/${repo}/milestones", params)
	if (response?.errors || (response?.status && response.status != 201)) {
		if (response.errors && response.errors[0]?.code == 'already_exists') {
			echo 'Milestone already exists and is not modified'
			// TODO: update milestone in this case: https://docs.github.com/en/rest/issues/milestones?apiVersion=2022-11-28#update-a-milestone
			// Usefull if e.g. the dates are wrongly read from the calendar
		} else {
			error "Response contains errors:\n${response}"
		}
	}
}

/**
 * Create a PR in the specified repo, from a branch that is expected to reside in the same repository.
 */
def createPullRequest(String orgaSlashRepo, String prTitle, String prBody, String headBranch, String baseBranch = 'master', boolean skipExistingPR = false) {
	echo "In ${orgaSlashRepo} create PR: '${prTitle}' on branch ${headBranch}"
	def params = [title: prTitle, body: prBody, head: headBranch, base: baseBranch]
	def response = queryGithubAPI('-X POST',"repos/${orgaSlashRepo}/pulls", params)
	if (response?.errors || (response?.status && response.status != 201)) {
		if (skipExistingPR && response.errors[0]?.message?.contains('pull request already exists')) {
			echo "Ignoring failure to create pull request: ${response.errors[0].message}"
			return null; // PR already exists and the caller asked to ignore this error
		}
		error "Response contains errors:\n${response}"
	}
	return response?.html_url
}

def triggerWorkflow(String orgaSlashRepo, String workflowId, Map<String, String> inputs, String referenceBranch = 'master') {
	echo "In ${orgaSlashRepo} trigger workflow '${workflowId}' on branch ${referenceBranch} with inputs ${inputs}"
	def params = ['ref': referenceBranch, 'inputs': inputs]
	def response = queryGithubAPI('-X POST',"repos/${orgaSlashRepo}/actions/workflows/${workflowId}/dispatches", params, true)
	if (response?.errors || (response?.status && response.status != 204)) {
		error "Response contains errors:\n${response}"
	}
}

def queryGithubAPI(String method, String endpoint, Map<String, Object> queryParameters, boolean allowEmptyReponse = false) {
	def query = """\
		curl -L ${method} \
			-H "Accept: application/vnd.github+json" \
			-H "Authorization: Bearer \${GITHUB_BOT_TOKEN}" \
			-H "X-GitHub-Api-Version: 2022-11-28" \
			https://api.github.com/${endpoint} \
		""".stripIndent()
	if (queryParameters != null) {
		def params = writeJSON(json: queryParameters, returnText: true)
		query += "-d '" + params + "'"
	}
	if (IS_DRY_RUN && !method.isEmpty()) {
		if (!env.GITHUB_BOT_TOKEN) {
			error 'Required GITHUB_BOT_TOKEN not set'
		}
		echo "Query (not send): ${query}"
		return null
	}
	def response = sh(script: query, returnStdout: true)
	if (response == null || response.isEmpty()) {
		if (allowEmptyReponse) {
			return null
		}
		error 'Response is null or empty. This commonly indicates: HTTP/1.1 500 Internal Server Error'
	}
	return readJSON(text: response)
}

return this
