/*******************************************************************************
 *  Copyright (c) 2025, 2025 Ed Merks and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     Ed Merks - initial API and implementation
 *     Hannes Wellmann - Apply Eclipse News scripts to reworked Build drop websites
 *******************************************************************************/

const meta = toElements(`
<meta charset="utf-8">
<meta http-equiv="X-UA-Compatible" content="IE=edge">
<meta name="viewport" content="width=device-width, initial-scale=1">
<link rel="shortcut icon" href="https://eclipseide.org/favicon.ico"/>
`);

const defaultHeader = toElements(`
	<a href="https://www.eclipse.org/downloads/packages/">Eclipse IDE</a>
	<a href="https://eclipseide.org/working-group/">Working Group</a>
	<a href="https://eclipseide.org/release/noteworthy/">New &amp; Noteworthy</a>
	<a href="https://marketplace.eclipse.org/">Marketplace</a>
`);

const eclipseBreadcrumbBase = toElements(`
	<a href="https://eclipse.org/">Home</a>
	<a href="https://www.eclipse.org/projects/">Projects</a>
	<a href="https://eclipse.dev/eclipse/">Eclipse</a>
`);

const equinoxBreadcrumbBase = toElements(`
	<a href="https://eclipse.org/">Home</a>
	<a href="https://www.eclipse.org/projects/">Projects</a>
	<a href="https://eclipse.dev/equinox/">Equinox</a>
`);

function getBuildType(buildData) {
    //This might be called for stable/release builds too
    return buildData.identifier.startsWith('Y') ? 'Y' : 'I'
}

function getBuildTypeName(buildData) {
    const identifier = buildData.identifier
    if (identifier.startsWith('R-')) {
        return 'Release'
    } else if (identifier.startsWith('S-')) {
        return identifier.includes('RC') ? 'Release Candidate' : 'Stable'
    } else {
        return buildData.kind
    }
}

function getWS(os) {
    if (os == 'win32') {
        return 'win32'
    } else if (os == 'linux') {
        return 'gtk'
    } else if (os == 'macosx') {
        return 'cocoa'
    } else {
        throw new Error('Unknown OS: ' + os)
    }
}

function getOSLabel(name) {
    if (name.includes('win32')) {
        return 'Windows'
    } else if (name.includes('linux')) {
        return 'Linux'
    } else if (name.includes('macosx')) {
        return 'macOS'
    } else {
        throw new Error('Cannot determine OS from name: ' + name)
    }
}
//TODO: make entire labels non-breaking
function getCPUArchLabel(name) {
    if (name.includes('x86_64')) {
        return 'x86 64-bit'
    } else if (name.includes('aarch64')) {
        return 'ARM 64-bit'
    } else if (name.includes('ppc64le')) {
        return 'PowerPC 64-bit'
    } else if (name.includes('riscv64')) {
        return 'RISC-V 64-bit'
    } else {
        throw new Error('Cannot determine CPU-Arch from name: ' + name)
    }
}

function getJenkinsTestJobsFolderURL(build) {
    const buildType = getBuildType(build)
    const testJobFolderName = buildType == 'I' ? 'AutomatedTests' : 'YBuilds'
    return `https://ci.eclipse.org/releng/job/${testJobFolderName}`
}

function getJenkinsTestJobNamePrefix(build) {
    const buildType = getBuildType(build)
    return `ep${build.releaseShort.replace('.', '')}${buildType}-unit`
}

function parseTestConfiguration(testConfig) {
    const [os, arch, javaPart] = testConfig.split('-')
    if (!javaPart.startsWith('java')) {
        throw new Error('Unexpected java version: ' + testConfig)
    }
    const javaVersion = javaPart.substring(4)
    const ws = getWS(os)
    return [os, ws, arch, javaVersion]
}

function getLongTestConfigurationName(testConfig, buildConfig) {
    const jobNamePrefix = getJenkinsTestJobNamePrefix(buildConfig)
    const [os, ws, arch, javaVersion] = parseTestConfiguration(testConfig)
    return `${jobNamePrefix}-${testConfig}_${os}.${ws}.${arch}_${javaVersion}`
}

function fetchAllTestSummaryFiles(build, testresultsPath) {
    const jobNamePrefix = getJenkinsTestJobNamePrefix(build)
    return fetchAllJSON(build.expectedTests.map(c => `${testresultsPath}${jobNamePrefix}-${c}-summary.json`))
}

function getPreliminaryPageData() {
    const identifier = getIdentifierFromSiteLocation()
    if (identifier) {
        let data = { identifier: identifier }
        if (identifier.startsWith('I') || identifier.startsWith('Y')) {
            data.label = identifier
        } else if (identifier.startsWith('S-') || identifier.startsWith('R-')) {
            data.label = identifier.substring(2, identifier.indexOf('-', 2))
            const versionEndIndex = Math.max(data.label.indexOf('M'), data.label.indexOf('RC'))
            const version = versionEndIndex < 0 ? data.label : data.label.substring(0, versionEndIndex)
            data.release = version.split('.').length === 2 ? (version + '.0') : version
            data.releaseShort = data.release.substring(0, data.release.lastIndexOf('.'))
        } else {
            throw new Error(`Unexpected identifier: ${identifier}`)
        }
        if (!identifier.startsWith('Y')) { // Y-build's adapt their 'kind' to the targeted java version, which is therefore not constant
            data.kind = 'Integration'
        }
        if (_dataGenerator) {
            data = _dataGenerator(data)
        }
        Object.keys(data).forEach(k => data[k] == undefined && delete data[k]);
        return data
    }
    return null
}

function getIdentifierFromSiteLocation() {
    if (window.location.hostname === 'download.eclipse.org') {
        const pathname = window.location.pathname
        for (const prefix of ['/eclipse/downloads/drops4/', '/equinox/drops/']) {
            if (pathname.startsWith(prefix)) {
                const idEndingSlash = pathname.indexOf('/', prefix.length)
                return pathname.substring(prefix.length, idEndingSlash > -1 ? idEndingSlash : pathname.length)
            }
        }
    }
    return null
}

const BUILD_DATE_FORMAT = new Intl.DateTimeFormat('en-GB', {
    timeZone: 'UTC',
    year: 'numeric',
    month: 'short',
    day: '2-digit',
    weekday: 'short',
    hour12: false,
    hour: '2-digit',
    minute: '2-digit',
})

function formatBuildDate(date) {
    return BUILD_DATE_FORMAT.format(new Date(date))
}

// Cache the runtime formatter if the API is available
let runtimeFormat = null;
if (typeof Intl.DurationFormat !== 'undefined') {
    try {
        runtimeFormat = new Intl.DurationFormat('en', { style: 'short' });
    } catch (e) {
        // Intl.DurationFormat not available
    }
}

function formatRuntime(runtime) {
    const totalSeconds = Math.trunc(runtime);

    // Try to use Temporal API if both Temporal and DurationFormat are available (not supported in all browsers yet)
    if (typeof Temporal !== 'undefined' && Temporal.Duration && runtimeFormat) {
        try {
            const duration = Temporal.Duration.from({ seconds: totalSeconds });
            return runtimeFormat.format(duration.round({
                largestUnit: 'hours',
                smallestUnit: totalSeconds >= 3600 ? 'minutes' : 'seconds'
            }));
        } catch (e) {
            // Fall through to fallback implementation
        }
    }

    // Fallback implementation for browsers without Temporal/DurationFormat support
    const hours = Math.floor(totalSeconds / 3600);
    const minutes = Math.floor((totalSeconds % 3600) / 60);
    const seconds = totalSeconds % 60;

    if (hours > 0) {
        // For durations >= 1 hour, show hours and minutes
        return `${hours} hr, ${minutes} min`;
    } else if (minutes > 0) {
        // For durations < 1 hour but >= 1 minute, show minutes and seconds
        return `${minutes} min, ${seconds} sec`;
    } else {
        // For durations < 1 minute, show only seconds
        return `${seconds} sec`;
    }
}

function fetchAllJSON(urls) {
    const promises = urls.map(url => fetch(url).then(res => {
        if (res.status == 404) {
            return {} // File (not yet) available -> Assume empty.
        }
        return res.json()
    }))
    return Promise.all(promises)
}

let _pageData = null
let _dataGenerator = null

function loadPageData(dataPath, dataGenerator = null) {
    _dataGenerator = dataGenerator
    _pageData = fetch(dataPath).then(res => res.json())
    if (dataGenerator) {
        _pageData = _pageData.then(dataGenerator)
    }
}

let contentPostProcessor = (_mainElement, _contentData) => {}

function generate() {
    try {
        const head = document.head;
        var referenceNode = head.querySelector('script');
        for (const element of [...meta]) {
            head.insertBefore(element, referenceNode.nextElementSibling)
            referenceNode = element;
        }

        const generators = document.querySelectorAll('[data-generate]');
        for (const element of generators) {
            const generator = element.getAttribute('data-generate');
            const generate = new Function(generator);
            generate.call(element, element);
        }

        const generatedBody = generateBody();
        // To reduce flickering, early resolve variables that can be derived from the build-drop's folder name
        const preliminaryData = _pageData ? getPreliminaryPageData() : null
        if (preliminaryData) {
            resolveDataReferences(generatedBody, preliminaryData, true)
        }
        document.body.replaceChildren(generatedBody);

        generateTOCItems(document.body) // assume no headers (for the TOC) are generated dynamically

        if (_pageData) {
            _pageData.then(data => {
                if (preliminaryData) {
                    verifyDataConsistency(preliminaryData, data)
                }
                const mainElement = document.body.querySelector('main')
                const contentMain = mainElement.querySelector('main') // This is the main element of the calling html file
                resolveDataReferences(document, data)
                contentPostProcessor(contentMain, data)
            })
        }
    } catch (exception) {
        logException(exception.message, exception)
    }
}

function generateTOCItems(mainElement) {
    const headersOfTOC = mainElement.querySelectorAll('h1[id], h2[id], h3[id], h4[id]');
    if (headersOfTOC.length === 0) {
        document.getElementById('toc-container').remove()
    } else {
        //TODO: Consider to use the more advanced style from the large aside at https://eclipse.dev/eclipse/news/news.html
        const tocList = document.getElementById('table-of-contents')
        for (const header of headersOfTOC) {
            const item = document.createElement('li')
            item.innerHTML = `<a href="#${header.id}">${header.textContent}</a>`
            tocList.appendChild(item)
        }
    }
}

const dataReferencePattern = /\${(?<path>[\w\-\.]+)}/g

function resolveDataReferences(contextElement, contextData, lenient = false) {
    const dataElements = Array.from(contextElement.getElementsByClassName('data-ref'))
    for (const element of dataElements) {
        const resolved = element.outerHTML.replaceAll(dataReferencePattern, (match, pathGroup, _offset, _string) => {
            return getValue(contextData, pathGroup, lenient ? match : undefined)
        })
        element.outerHTML = resolved
        dataReferencePattern.lastIndex = 0 // reset lastIndex as RegExp.prototype.test() is stateful. See https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/RegExp/test
        if (!lenient || !dataReferencePattern.test(resolved)) {
            // Prevent multiple processing in subsequent passes with different context (if no variables are contained anymore)
            element.classList.remove('data-ref')
        }
    }
}

function getValue(data, path, lenienceDefaultValue = undefined) {
    let value = data
    for (const key of path.split('.')) {
        if (!value.hasOwnProperty(key)) {
            if (lenienceDefaultValue) {
                return lenienceDefaultValue // just skip absent variables
            }
            throw new Error(`Key '${key}' not found in ${JSON.stringify(value)}`)
        }
        value = value[key]
    }
    return value;
}

function verifyDataConsistency(preliminaryData, data) {
    for (const key in preliminaryData) {
        const dataValue = data[key]
        if (dataValue !== undefined && dataValue !== preliminaryData[key]) {
            const msg = `Prelininary value of key '${key}' differes from loaded data.
	                             preliminary - ${preliminaryData[key]},
	                             loaded data - ${data[key]}`
            logException(msg, msg)
            throw new Error(msg)
        }
    }
}

function logException(message, loggedObject) {
    document.body.prepend(toElement(`<p>Failed to generate content: <b style="color: FireBrick">${message}</b></p>`));
    console.log(loggedObject);
}

function generateBody() {
    const hasHeadersForTOC = document.querySelector('h1[id], h2[id], h3[id], h4[id]') !== null;
    const col = hasHeadersForTOC ? 'col-md-18' : ' col-md-24';
    return toElement(`
<div>
	${generateHeader()}
	<main id="content">
		<div class="novaContent container" id="novaContent">
			<div class="row">
				<div class="${col} main-col-content">
					<div class="novaContent" id="novaContent">
						<div class="row">
							${generateBreadcrumb()}
						</div>
						<div class=" main-col-content">
							<div id="midcolumn">
							${generateMainContent()}
							</div>
						</div>
					</div>
				</div>
				<div id="toc-container" class="col-md-6">
					<aside>
						<ul class="ul-left-nav">
							<div class="sideitem">
								<h2>Table of Contents</h2>
								<div id="toc-target">
									<ul id="table-of-contents" style="list-style-type:square;">
									</ul>
								</div>
							</div>
						</ul>
					</aside>
				</div>
			</div>
		</div>
	</main>
	<footer id="footer">
		<div class="container">
			<div class="footer-sections row equal-height-md font-bold">
				<div id="footer-eclipse-foundation" class="footer-section col-md-5 col-sm-8">
					<div class="menu-heading">Eclipse Foundation</div>
					<ul class="nav">
						<ul class="nav">
							<li><a href="http://www.eclipse.org/org/">About</a></li>
							<li><a href="https://projects.eclipse.org/">Projects</a></li>
							<li><a href="http://www.eclipse.org/collaborations/">Collaborations</a></li>
							<li><a href="http://www.eclipse.org/membership/">Membership</a></li>
							<li><a href="http://www.eclipse.org/sponsor/">Sponsor</a></li>
						</ul>
					</ul>
				</div>
				<div id="footer-legal" class="footer-section col-md-5 col-sm-8">
					<div class="menu-heading">Legal</div>
					<ul class="nav">
						<ul class="nav">
							<li><a href="http://www.eclipse.org/legal/privacy.php">Privacy Policy</a></li>
							<li><a href="http://www.eclipse.org/legal/termsofuse.php">Terms of Use</a></li>
							<li><a href="http://www.eclipse.org/legal/compliance/">Compliance</a></li>
							<li><a href="http://www.eclipse.org/org/documents/Community_Code_of_Conduct.php">Code of
									Conduct</a></li>
							<li><a href="http://www.eclipse.org/legal/">Legal Resources</a></li>
						</ul>
					</ul>
				</div>
				<div id="footer-more" class="footer-section col-md-5 col-sm-8">
					<div class="menu-heading">More</div>
					<ul class="nav">
						<ul class="nav">
							<li><a href="http://www.eclipse.org/security/">Report a Vulnerability</a></li>
							<li><a href="https://www.eclipsestatus.io/">Service Status</a></li>
							<li><a href="http://www.eclipse.org/org/foundation/contact.php">Contact</a></li>
							<li><a href="http://www.eclipse.org//projects/support/">Support</a></li>
						</ul>
					</ul>
				</div>
			</div>
			<div class="col-sm-24">
				<div class="row">
					<div id="copyright" class="col-md-16">
						<p id="copyright-text">Copyright © Eclipse Foundation AISBL. All Rights Reserved.</p>
					</div>
				</div>
			</div>
			<a href="#" class="scrollup" onclick="scrollToTop()">Back to the top</a>
		</div>
	</footer>
</div>
`);
}

function generateMainContent() {
    const main = document.body.querySelector('main')
    if (main != null) {
        return main.outerHTML
    }
    return '<main>The body specifies no content.</main>';
}

function generateHeader() {
    let elements = document.querySelectorAll('#header>a');
    if (elements.length == 0) {
        elements = defaultHeader
    }
    const items = Array.from(elements).map(link => {
        link.classList.add('link-unstyled');
        return `
<li class="navbar-nav-links-item">
	${link.outerHTML}
</li>
`;
    });
    const mobileItems = Array.from(elements).map(link => {
        link.className = 'mobile-menu-item mobile-menu-dropdown-toggle';
        return `
<li class="mobile-menu-dropdown">
	${link.outerHTML}
</li>
`;
    });

    return `
<header class="header-wrapper" id="header">
	<div class="header-navbar-wrapper">
		<div class="container">
			<div class="header-navbar">
				<a class="header-navbar-brand" href="https://eclipseide.org/">
					<div class="logo-wrapper">
						<img src="https://eclipse.dev/eclipse.org-common/themes/solstice/public/images/logo/eclipse-ide/eclipse_logo.svg" alt="Eclipse Project" width="150"/>
					</div>
				</a>
				<nav class="header-navbar-nav">
					<ul class="header-navbar-nav-links">
						${items.join('\n')}
					</ul>
				</nav>
				<div class="header-navbar-end">
					<div class="float-right hidden-xs" id="btn-call-for-action">
						<a href="https://www.eclipse.org/sponsor/ide/" class="btn btn-huge btn-warning">
							<i class="fa fa-star"></i> Sponsor
						</a>
					</div>
					<button class="mobile-menu-btn" onclick="toggleMenu()">
						<i class="fa fa-bars fa-xl"/></i>
					</button>
				</div>
			</div>
		</div>
	</div>
	<nav id="mobile-menu" class="mobile-menu hidden" aria-expanded="false">
		<ul>
			${mobileItems.join('\n')}
		</ul>
	</nav>
</header>
`;
}

function generateDefaultBreadcrumb(element, items) {
    return prependChildren(element, 'breadcrumb', ...items);
}

function generateBreadcrumb() {
    const breadcumbs = document.getElementById('breadcrumb')
    if (breadcumbs == null) {
        return '';
    }
    const elements = Array.from(breadcumbs.children);
    const items = elements.map(e => `<li>${e.outerHTML}</li>`);
    return `
<section class="default-breadcrumbs hidden-print breadcrumbs-default-margin"
	id="breadcrumb">
	<div class="container">
		<h3 class="sr-only">Breadcrumbs</h3>
		<div class="row">
			<div class="col-sm-24">
				<ol class="breadcrumb">
					${items.join('\n')}
				</ol>
			</div>
		</div>
	</div>
</section>
`;
}

function toElements(text) {
    const wrapper = document.createElement('div');
    wrapper.innerHTML = text;
    return wrapper.children
}

function toElement(text) {
    const elements = toElements(text)
    if (elements.length != 1) {
        throw new Error(`Not exactly one element: ${elements.length}`)
    }
    return elements[0]
}

function prependChildren(element, id, ...children) {
    element.id = id;
    element.prepend(...children);
    return element;
}
