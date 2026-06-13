/*******************************************************************************
 * Copyright (c) 2025, 2026 Hannes Wellmann and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Hannes Wellmann - initial API and implementation
 *******************************************************************************/

@groovy.transform.Field
def boolean _GH_API_IS_DRY_RUN = true

def setDryRun(boolean isDryRun) {
	_GH_API_IS_DRY_RUN = isDryRun
}

def listEclipseOrganizations() {
	return [ 'eclipse-platform', 'eclipse-equinox', 'eclipse-jdt', 'eclipse-pde' ]
}

/** Returns a list of all repositories in the specified organization.*/
def listReposOfOrganization(String orga) {
	def response = queryGithubAPI('', "orgs/${orga}/repos")
	if (!(response instanceof List) && isFailed(response, 200)) {
		error "Response contains errors:\n${response}"
	}
	return response.findAll{ repository ->
		if (repository.archived) {
			echo "Skipping archived repository: ${repository.name}"
			return false
		} else if ('.eclipsefdn'.equals(repository.name)) {
			echo "Skipping .eclipsefdn repository of : ${orga}"
			return false
		}
		return true
	}
}

/**
 * Create a new milestone.
 * @param dueDay the milestone's due-date, format: YYYY-MM-DD
 */
def createMilestone(String orgaRepo, String title, String description, String dueDay) {
	echo "In ${orgaRepo}, create milestone: ${title} due on ${dueDay}"
	def params = [title: title, description: description, due_on: "${dueDay}T23:59:59Z"]
	def response = queryGithubAPI('-X POST', "repos/${orgaRepo}/milestones", params)
	if (isFailed(response, 201)) {
		if (response.errors && response.errors[0]?.code == 'already_exists') {
			echo "Milestone '${title}' already exists and is updated"
			updateMilestone(orgaRepo, title, params)
		} else {
			error "Response contains errors:\n${response}"
		}
	}
}

def closeMilestone(String orgaRepo, String title) {
	updateMilestone(orgaRepo, title, [state: 'closed'])
}

def updateMilestone(String orgaRepo, String title, Map<String, Object> updatedParameters) {
	def milestoneNumber = findMilestoneNumber(orgaRepo, title)
	echo "In ${orgaRepo}, update milestone: ${title}"
	def response = queryGithubAPI('-X PATCH', "repos/${orgaRepo}/milestones/${milestoneNumber}", updatedParameters)
	if (isFailed(response, 200)) {
		error "Response contains errors:\n${response}"
	}
}

def findMilestoneNumber(String orgaRepo, String title) {
	// Iterate over all (open and closed) milestones, sorted by decending due-date.
	// See https://docs.github.com/en/rest/issues/milestones?apiVersion=2026-03-10#list-milestones
	// Consider even closed milestones to make the pipelines more robust in case some milestones are closed manually
	def milestone = findElement("repos/${orgaRepo}/milestones?state=all&sort=due_on&direction=desc", {ms -> ms.title == title})
	if (milestone == null) {
		error "Milestone '${title}' not found among the most recent ~1000 milestones"
	}
	return milestone
}

private Object findElement(String query, Closure predicate) {
	// See https://docs.github.com/en/rest/using-the-rest-api/using-pagination-in-the-rest-api?apiVersion=2026-03-10
	def perPage = 30 // Should usually be sufficient
	def maxPage = 33
	for (int page in 1..maxPage) {
		def response = queryGithubAPI('', "${query}&per_page=${perPage}&page=${page}")
		if (!(response instanceof List) && isFailed(response, 200)) {
			error "Response contains errors:\n${response}"
		}
		for (element in response) {
			if (predicate.call(element)) {
				return element
			}
		}
		if (response.size() < perPage) {
			break; // last page reached
		}
	}
	return null
}

/**
 * Create a PR in the specified repo, from a branch that is expected to reside in the same repository.
 */
def createPullRequest(String orgaSlashRepo, String title, String body, String headBranch, String baseBranch = 'master', boolean skipExistingPR = false) {
	echo "In ${orgaSlashRepo} create PR: '${title}' on branch ${headBranch}"
	def params = [title: title, body: body, head: headBranch, base: baseBranch]
	def response = queryGithubAPI('-X POST',"repos/${orgaSlashRepo}/pulls", params)
	if (isFailed(response, 201)) {
		if (skipExistingPR && response.errors[0]?.message?.contains('pull request already exists')) {
			echo "Ignoring failure to create pull request: ${response.errors[0].message}"
			return null; // PR already exists and the caller asked to ignore this error
		}
		error "Response contains errors:\n${response}"
	}
	return response?.html_url
}

/**
 * Create an issue in the specified repo.
 */
def createIssue(String orgaRepo, String title, String body) {
	echo "In ${orgaRepo}, create Issue: '${title}'"
	def params = [title: title, body: body]
	def response = queryGithubAPI('-X POST',"repos/${orgaRepo}/issues", params)
	if (isFailed(response, 201)) {
		error "Response contains errors:\n${response}"
	}
	return response?.html_url
}

def findIssue(String orgaRepo, String title) {
	// Iterate over all issues, sorted by decending date of last update.
	// See https://docs.github.com/en/rest/issues/issues?apiVersion=2026-03-10#list-repository-issues
	def issue = findElement("repos/${orgaRepo}/issues?state=all&sort=updated&direction=desc", {i -> i.title == title})
	return issue?.html_url // Do not fail if issue was not found
}

def triggerWorkflow(String orgaSlashRepo, String workflowId, Map<String, String> inputs, String referenceBranch = 'master') {
	echo "In ${orgaSlashRepo} trigger workflow '${workflowId}' on branch ${referenceBranch} with inputs ${inputs}"
	def params = ['ref': referenceBranch, 'inputs': inputs]
	def response = queryGithubAPI('-X POST',"repos/${orgaSlashRepo}/actions/workflows/${workflowId}/dispatches", params, true)
	if (isFailed(response, 200)) {
		error "Response contains errors:\n${response}"
	}
}

def queryGithubAPI(String method, String endpoint, Map<String, Object> queryParameters = null, boolean allowEmptyReponse = false) {
	def query = """\
		curl -L ${method} \
			-H "Accept: application/vnd.github+json" \
			-H "Authorization: Bearer \${GITHUB_BOT_TOKEN}" \
			-H "X-GitHub-Api-Version: 2026-03-10" \
			'https://api.github.com/${endpoint}' \
		""".replace('\t','').trim()
	if (queryParameters != null) {
		def params = writeJSON(json: queryParameters, returnText: true)
		params = params.replace("'", "'\\''") // Escape single quotes by '\''
		query += " -d '" + params + "'"
	}
	if (_GH_API_IS_DRY_RUN && !method.isEmpty()) {
		echo "Query (not send): ${query}"
		return null
	}
	// Re-use Github API token, if already set up in the caller
	def Closure executeQuery = { sh(script: query, returnStdout: true) }
	def response = env.GITHUB_BOT_TOKEN ? executeQuery.call()
		: withCredentials([string(credentialsId: 'github-bot-token', variable: 'GITHUB_BOT_TOKEN')], executeQuery)
	if (response == null || response.isEmpty()) {
		if (allowEmptyReponse) {
			return null
		}
		error 'Response is null or empty. This commonly indicates: HTTP/1.1 500 Internal Server Error'
	}
	return readJSON(text: response)
}

private boolean isFailed(Map response, int successCode) {
	return response?.errors || (response?.status && response.status?.toInteger() != successCode)
}

def getCurrentRepositoriesGithubName() {
	def submoduleURL = sh(script: "git config remote.origin.url", returnStdout: true).trim()
	// Extract repository path from e.g.: https://github.com/eclipse-platform/eclipse.platform.git
	def expectedPrefix = 'https://github.com/'
	def expectedSuffix = '.git'
	if (!submoduleURL.startsWith(expectedPrefix) || !submoduleURL.endsWith(expectedSuffix)) {
		error "Unexpected of submodule URL: ${submoduleURL}"
	}
	return submoduleURL.substring(expectedPrefix.length(), submoduleURL.length() - expectedSuffix.length())
}

return this
