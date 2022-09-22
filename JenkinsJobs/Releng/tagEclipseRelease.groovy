job('Releng/tagEclipseRelease'){
  displayName('Tag Eclipse Release')
  description('Tag promoted builds.')

  logRotator {
    daysToKeep(5)
    numToKeep(5)
  }

  parameters {
    stringParam('tag', null, '''
R is used for release builds. For example: R4_25
S is used for milestones and includes the milestone version. For example: S4_25_0_RC2
    ''')
    stringParam('buildId', null, 'I-build ID of the build that was promoted, for example: I20220831-1800')
    stringParam('annotation', null, '''
GitHub issue (or legacy bugzilla bug ID) to track tagging the release, for example:
#557 - Tag Eclipse 4.25 release
    ''')
  }

  jdk('oracle-jdk8-latest')

  label('migration')

  wrappers { //adds pre/post actions
    timestamps()
    sshAgent('ssh://genie.releng@git.eclipse.org', 'GitHub bot (SSH)')
    preBuildCleanup()
  }
  
  steps {
    shell('''
#!/bin/bash -x

# Strip spaces from the buildId and tag
buildId=$(echo $buildId|tr -d ' ')
tag=$(echo $tag|tr -d ' ')

#If build id or tag is empty we need to exit.
if [ -z "$buildId" ]
then
  exit 1
fi

if [ -z "$tag" ]
then
  exit 1
fi

buildType=$(echo ${buildId}|cut -c 1)

cd ${WORKSPACE}

git config --global user.email "releng-bot@eclipse.org"
git config --global user.name "Eclipse Releng Bot"
git clone -b master --recursive git@github.com:eclipse-platform/eclipse.platform.releng.aggregator.git

cd eclipse.platform.releng.aggregator

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

function toPushRepo() {
	from="$1"
	if ! [[ "$from" == http* ]]; then
		echo $from
	else
		echo $(sed -e 's,http://git.eclipse.org/gitroot,ssh://genie.releng@git.eclipse.org:29418,' -e 's,https://git.eclipse.org/r,ssh://genie.releng@git.eclipse.org:29418,' -e 's/https:\/\/github.com/ssh:\/\/git@github.com/g' <<< $from)
	fi
}

git tag -a -m "${annotation}" ${tag} ${buildId}
git push --verbose $(toPushRepo $(git config --get remote.origin.url)) tag ${tag}

for i in $(ls)
do
	pushd $i
    git tag -a -m "${annotation}" ${tag} ${buildId}
    git push --verbose $(toPushRepo $(git config --get remote.origin.url)) tag ${tag}
    popd
done
    ''')
  }
}
