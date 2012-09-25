#!/bin/bash

if [[ ! -e "setup.sh" ]]; then
   echo "Copy setup.tpl to setup.sh and modify it for your local environment"
   exit 1
fi

BASE=$( cd $( dirname "$0" ) > /dev/null ; pwd )

. "$BASE/setup.sh"

if [[ ! -e "${m2settings}" ]]; then
    echo "Copy $HOME/.m2/settings.xml or settings.tpl to ${m2settings}"
    exit 1
fi

mvnWrapper() {
    mvn -Dmaven.repo.local="${m2repo}" --settings "${m2settings}" "$@" || exit 1
}

cmd="$1"

case "$cmd" in
    build ) #CMD Build all the sources
        mvnWrapper -f eclipse-parent/pom.xml clean install
        mvnWrapper -f maven-cbi-plugin/pom.xml clean install
        mvnWrapper clean install -Dmaven.test.skip=true
        echo "Build successful"
        echo "You can find the installation files in $BASE/TMP/org.eclipse.sdk.epp/target/products/"
        ls -l "$BASE/TMP/org.eclipse.sdk.epp/target/products"
        ;;

    test ) #CMD Run the tests. You must build at least once before you can run this
        # TODO
        ;;

    * )
        echo "Missing command. Available are:"
        grep CMD "$0" | grep -v grep | sed -e "s:^[ \t]+::g" -e 's:[)] #CMD:-:'
        ;;
esac

exit 0
