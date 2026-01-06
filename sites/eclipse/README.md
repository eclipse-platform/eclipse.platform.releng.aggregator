# Eclipse Build websites

The websites for all Eclipse and Equinox builds and their overview pages are based on plain HTML pages, that read their individual information from data files (in JSON format) and apply them via _JavaScript_.
All data files of each website are contained within the root folder of each site and all sites are generally self-contained.
The data files are generated during a build running the [RelEng Java scripts programs](../../scripts/releng)
or by the [Update Download Index](../../JenkinsJobs/Releng/updateIndex.jenkinsfile) Jenkins job.

# Local testing

To locally create the full set of data (JSON) files for all websites, e.g. for local testing, one can run
`./testDataFetch.sh <buildID>`

That scripts fetches the data files of the build with specified identifier and places it at the expected location and allows to replicate the Eclipse and Equinox websites of that build and the current overview pages locally.
Any I, Y, milestone, RC or release available at https://download.eclipse.org/eclipse/downloads/ may be specified.

Launch `jwebserver` from this repository (requires a JDK-18 or later on `PATH`) and open the localhost URL displayed on the console (by default `http://127.0.0.1:8000/`).
Run `jwebserver --help` for help and further options.
