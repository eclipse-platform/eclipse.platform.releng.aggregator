#!/bin/bash

###
### THIS SCRIPT IS DEPRECATED, USE build.sh INSTEAD.
###

echo "WARNING: This script is deprecated and will be removed in the future, please use build.sh instead."



BASE=$( cd $( dirname "$0" ) > /dev/null ; pwd )

if [[ ! -e "$BASE/setup.sh" ]]; then
   echo "Copy cbi_setup.tpl to $BASE/setup.sh and modify it for your local environment"
   exit 1
fi


source "$BASE/setup.sh"

if [[ ! -e "${m2settings}" ]]; then
    echo "Copy $HOME/.m2/settings.xml or cbi_settings.tpl to ${m2settings}"
    exit 1
fi

mvnWrapper() {
    mvn -Dmaven.repo.local="${m2repo}" --settings "${m2settings}" "$@" || exit 1
}

cmd="$1"

case "$cmd" in
    build ) #CMD Build all the sources
        mvnWrapper -f eclipse-platform-parent/pom.xml clean install
        mvnWrapper clean install -Dmaven.test.skip=true
        echo "Build successful"
        echo "You can find the installation files in $BASE/../eclipse.platform.releng.tychoeclipsebuilder/sdk/target/products/"
        ls -l "$BASE/../eclipse.platform.releng.tychoeclipsebuilder/sdk/target/products"
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

