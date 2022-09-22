job('Releng/createMaintenanceBranch'){
  displayName('Create Maintenance Branch')
  description('Create a new maintenance branch for a release.')

  logRotator {
    daysToKeep(5)
    numToKeep(5)
  }

  parameters {
    stringParam('branchName', null, 'Name of the branch to be created. For example: R4_25_maintenance')
    stringParam('tag', null, 'Release tag to be used when making the branch. For example: S4_25_0_RC2')
  }

  jdk('openjdk-jdk11-latest')

  label('migration')

  wrappers { //adds pre/post actions
    timestamps()
    sshAgent('ssh://genie.releng@git.eclipse.org', 'GitHub bot (SSH)')
  }
  
  steps {
    shell('''
#!/bin/bash -x

function fn_toPushRepo() {
	from="$1"
	if ! [[ "$from" == http* ]]; then
		echo $from
	else
		echo $(sed -e 's,http://git.eclipse.org/gitroot,ssh://genie.releng@git.eclipse.org:29418,' -e 's,https://git.eclipse.org/r,ssh://genie.releng@git.eclipse.org:29418,' -e 's,https://github.com/,git@github.com:,' <<< $from)
	fi
}

fn_branch_create() 
{
	cd ${WORKSPACE}/eclipse.platform.releng.aggregator/$1
	git checkout -b ${branchName} ${tag}
    git branch --set-upstream ${branchName} origin/${branchName}
    PUSH_URL="$(fn_toPushRepo $(git config --get remote.origin.url))"
	git push -u $PUSH_URL ${branchName}
}



git config --global user.email "genie.releng@eclipse.org"
git config --global user.name "Eclipse Releng Bot"

git clone --recurse-submodules git@github.com:eclipse-platform/eclipse.platform.releng.aggregator.git

cd eclipse.platform.releng.aggregator/
git clean -f -d -x
git submodule foreach git clean -f -d -x
git reset --hard
git submodule foreach git reset --hard
git checkout master
git submodule foreach git checkout master
git clean -f -d -x
git submodule foreach git clean -f -d -x
git reset --hard
git submodule foreach git reset --hard
git pull
git submodule foreach git pull

fn_branch_create eclipse.jdt
fn_branch_create eclipse.jdt.core
fn_branch_create eclipse.jdt.core.binaries
fn_branch_create eclipse.jdt.debug
fn_branch_create eclipse.jdt.ui
#fn_branch_create eclipse.pde.build
#fn_branch_create eclipse.pde.ui
fn_branch_create eclipse.pde
fn_branch_create eclipse.platform
fn_branch_create eclipse.platform.common
fn_branch_create eclipse.platform.debug
fn_branch_create eclipse.platform.releng
fn_branch_create eclipse.platform.resources
fn_branch_create eclipse.platform.runtime
fn_branch_create eclipse.platform.swt
fn_branch_create eclipse.platform.swt.binaries
fn_branch_create eclipse.platform.team
fn_branch_create eclipse.platform.text
fn_branch_create eclipse.platform.ua
fn_branch_create eclipse.platform.ui
fn_branch_create eclipse.platform.ui.tools
fn_branch_create rt.equinox.binaries
fn_branch_create rt.equinox.bundles
fn_branch_create equinox
fn_branch_create rt.equinox.p2
fn_branch_create 
    ''')
  }
}
