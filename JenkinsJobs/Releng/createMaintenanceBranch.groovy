job('Releng/createMaintenanceBranch'){
  displayName('Create Maintenance Branch')
  description('Create a new maintenance branch for a release.')

  logRotator {
    daysToKeep(5)
    numToKeep(5)
  }

  parameters {
    stringParam('branchName', null, 'Name of the branch to be created. For example: R4_26_maintenance')
    stringParam('tag', null, 'Release tag to be used when making the branch. For example: S4_26_0_RC2')
  }

  label('centos-latest')

  wrappers { //adds pre/post actions
    timestamps()
    sshAgent('git.eclipse.org-bot-ssh', 'github-bot-ssh')
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
	git checkout -b ${branchName} ${tag}
    git branch --set-upstream-to origin ${branchName}
    PUSH_URL="$(fn_toPushRepo $(git config --get remote.origin.url))"
	git push -u $PUSH_URL ${branchName}
}

git config --global user.email "genie.releng@eclipse.org"
git config --global user.name "Eclipse Releng Bot"

git clone --recurse-submodules git@github.com:eclipse-platform/eclipse.platform.releng.aggregator.git

cd eclipse.platform.releng.aggregator/

#create maintenance branch in aggregator if it does not exist
existingBranches=$(git branch -r)

if [[ ! $(echo $existingBranches | grep $branchName) ]]; then {
	git checkout -b ${branchName} ${tag}
    git branch --set-upstream-to origin ${branchName}
    PUSH_URL="$(fn_toPushRepo $(git config --get remote.origin.url))"
	git push -u $PUSH_URL ${branchName}
} else {
	echo "Already created branch ${branchName} in eclipse.platform.releng.aggregator"
}
fi

for i in $(ls)
do
	if [[ -d $i ]]; then {
	pushd $i
    existingBranches=$(git branch -r)
		if [[ ! $(echo $existingBranches | grep $branchName) ]]; then {
			fn_branch_create $i
		} else {
			echo "Already created branch ${branchName} in $i)"
		}
		fi
    popd
	}
	fi
done
    ''')
  }
}
