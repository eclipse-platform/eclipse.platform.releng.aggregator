#!/usr/bin/env bash
#

convertOptions () {
  CONVERT='
  ;../org.eclipse.ant.core/src
  ;../org.eclipse.compare.core/src
  ;../org.eclipse.compare/compare
  ;../org.eclipse.core.commands/src
  ;../org.eclipse.core.contenttype/src
  ;../org.eclipse.core.databinding.beans/src
  ;../org.eclipse.core.databinding.observable/src
  ;../org.eclipse.core.databinding.property/src
  ;../org.eclipse.core.databinding/src
  ;../org.eclipse.core.expressions/src
  ;../org.eclipse.core.filebuffers/src
  ;../org.eclipse.core.filesystem/src
  ;../org.eclipse.core.jobs/src
  ;../org.eclipse.core.net/src
  ;../org.eclipse.core.resources/src
  ;../org.eclipse.core.runtime.compatibility/src-model
  ;../org.eclipse.core.runtime.compatibility/src-runtime
  ;../org.eclipse.core.runtime/src
  ;../org.eclipse.core.variables/src
  ;../org.eclipse.debug.core/core
  ;../org.eclipse.debug.ui/ui
  ;../org.eclipse.equinox.app/src
  ;../org.eclipse.e4.core.commands/src
  ;../org.eclipse.e4.core.contexts/src
  ;../org.eclipse.e4.core.di/src
  ;../org.eclipse.e4.core.services/src
  ;../org.eclipse.e4.ui.bindings/src
  ;../org.eclipse.e4.ui.css.core/src
  ;../org.eclipse.e4.ui.css.swt/src
  ;../org.eclipse.e4.ui.css.swt.theme/src
  ;../org.eclipse.e4.ui.di/src
  ;../org.eclipse.e4.ui.model.workbench/src
  ;../org.eclipse.e4.ui.services/src
  ;../org.eclipse.e4.ui.widgets/src
  ;../org.eclipse.e4.ui.workbench.renderers.swt/src
  ;../org.eclipse.e4.ui.workbench.swt/src
  ;../org.eclipse.e4.ui.workbench/src
  ;../org.eclipse.e4.ui.workbench3/src
  ;../org.eclipse.equinox.bidi/src
  ;../org.eclipse.equinox.ds/src
  ;../org.eclipse.equinox.common/src
  ;../org.eclipse.equinox.frameworkadmin/src
  ;../org.eclipse.equinox.http.jetty_*/src
  ;../org.eclipse.equinox.http.registry/src
  ;../org.eclipse.equinox.http.servlet/src
  ;../org.eclipse.equinox.jsp.jasper.registry/src
  ;../org.eclipse.equinox.jsp.jasper/src
  ;../org.eclipse.equinox.preferences/src
  ;../org.eclipse.equinox.p2.core/src
  ;../org.eclipse.equinox.p2.director/src
  ;../org.eclipse.equinox.p2.engine/src
  ;../org.eclipse.equinox.p2.metadata/src
  ;../org.eclipse.equinox.p2.metadata.repository/src
  ;../org.eclipse.equinox.p2.operations/src
  ;../org.eclipse.equinox.p2.ql/src
  ;../org.eclipse.equinox.p2.repository/src
  ;../org.eclipse.equinox.p2.touchpoint.eclipse/src
  ;../org.eclipse.equinox.p2.ui/src
  ;../org.eclipse.equinox.registry/src
  ;../org.eclipse.equinox.security/src
  ;../org.eclipse.help.base/src
  ;../org.eclipse.help.base/src_demo
  ;../org.eclipse.help.ui/src
  ;../org.eclipse.help.webapp/src
  ;../org.eclipse.help/src
  ;../org.eclipse.jface.databinding/src
  ;../org.eclipse.jface.text/projection
  ;../org.eclipse.jface.text/src
  ;../org.eclipse.jface/src
  ;../org.eclipse.jsch.core/src
  ;../org.eclipse.jsch.ui/src
  ;../org.eclipse.ltk.core.refactoring/src
  ;../org.eclipse.ltk.ui.refactoring/src
  ;../org.eclipse.osgi/core/adaptor
  ;../org.eclipse.osgi/core/framework
  ;../org.eclipse.osgi/eclipseAdaptor/src
  ;../org.eclipse.osgi/security/src
  ;../org.eclipse.osgi/supplement/src
  ;../org.eclipse.pde/src
  ;../org.eclipse.platform/src
  ;../org.eclipse.search/new search
  ;../org.eclipse.search/search
  ;../org.eclipse.swt/Eclipse SWT Accessibility/common/
  ;../org.eclipse.swt/Eclipse SWT Accessibility/win32/
  ;../org.eclipse.swt/Eclipse SWT AWT/common/
  ;../org.eclipse.swt/Eclipse SWT AWT/win32/
  ;../org.eclipse.swt/Eclipse SWT Browser/common/
  ;../org.eclipse.swt/Eclipse SWT Browser/win32/
  ;../org.eclipse.swt/Eclipse SWT Custom Widgets/common/
  ;../org.eclipse.swt/Eclipse SWT Drag and Drop/common/
  ;../org.eclipse.swt/Eclipse SWT Drag and Drop/win32/
  ;../org.eclipse.swt/Eclipse SWT OLE Win32/win32/
  ;../org.eclipse.swt/Eclipse SWT OpenGL/common
  ;../org.eclipse.swt/Eclipse SWT OpenGL/emulated
  ;../org.eclipse.swt/Eclipse SWT OpenGL/glx
  ;../org.eclipse.swt/Eclipse SWT OpenGL/win32
  ;../org.eclipse.swt/Eclipse SWT PI/common/
  ;../org.eclipse.swt/Eclipse SWT PI/common_j2se/
  ;../org.eclipse.swt/Eclipse SWT PI/win32/
  ;../org.eclipse.swt/Eclipse SWT Printing/common/
  ;../org.eclipse.swt/Eclipse SWT Printing/win32/
  ;../org.eclipse.swt/Eclipse SWT Program/common/
  ;../org.eclipse.swt/Eclipse SWT Program/win32/
  ;../org.eclipse.swt/Eclipse SWT/common/
  ;../org.eclipse.swt/Eclipse SWT/common_j2se/
  ;../org.eclipse.swt/Eclipse SWT/win32/
  ;../org.eclipse.team.core/src
  ;../org.eclipse.team.ui/src
  ;../org.eclipse.text/projection
  ;../org.eclipse.text/src
  ;../org.eclipse.ui.cheatsheets/src
  ;../org.eclipse.ui.console/src
  ;../org.eclipse.ui.editors/src
  ;../org.eclipse.ui.forms/src
  ;../org.eclipse.ui.ide/extensions
  ;../org.eclipse.ui.ide/src
  ;../org.eclipse.ui.intro.universal/src
  ;../org.eclipse.ui.intro/src
  ;../org.eclipse.ui.navigator.resources/src
  ;../org.eclipse.ui.navigator/src
  ;../org.eclipse.ui.views.properties.tabbed/src
  ;../org.eclipse.ui.views/src
  ;../org.eclipse.ui.workbench.texteditor/src
  ;../org.eclipse.ui.workbench/Eclipse UI
  ;../org.eclipse.ui.workbench/Eclipse UI Editor Support
  ;../org.eclipse.ui/src
  ;../org.eclipse.update.configurator/src
  '


  for DPATH in $CONVERT; do
    NPATH=${DPATH:1}
    PLUGIN=$( echo $NPATH | cut -f2 -d/ )
    PLUGIN_DIR=$( grep "${PLUGIN}\$" dirs.txt | head -1 )
    PLUGIN_BASE=$( echo $PLUGIN_DIR | sed "s/\/${PLUGIN}\$//g" | sed 's!/!.!g' )
    echo ${DPATH} | sed "s/\.\./@${PLUGIN_BASE}@/g"
  done

}


convertSchema () {

  while read line; do
    NPATH="$line"
    PLUGIN=$( echo $NPATH | cut -f2 -d/ )
    PLUGIN_DIR=$( grep "${PLUGIN}\$" dirs.txt | head -1 )
    PLUGIN_BASE=$( echo $PLUGIN_DIR | sed "s/\/${PLUGIN}\$//g" | sed 's!/!.!g' )
    echo ${NPATH} | sed "s/\.\./\${${PLUGIN_BASE}\}/g"
  done <<EOF
  <pde.convertSchemaToHTML manifest="../org.eclipse.ant.core/plugin.xml" destination="${dest}" />
  <pde.convertSchemaToHTML manifest="../org.eclipse.ant.ui/plugin.xml" destination="${dest}" />
  <pde.convertSchemaToHTML manifest="../org.eclipse.compare/plugin.xml" destination="${dest}" />
  <pde.convertSchemaToHTML manifest="../org.eclipse.core.contenttype/plugin.xml" destination="${dest}" />
  <pde.convertSchemaToHTML manifest="../org.eclipse.core.expressions/plugin.xml" destination="${dest}" />
  <pde.convertSchemaToHTML manifest="../org.eclipse.core.filebuffers/plugin.xml" destination="${dest}" />
  <pde.convertSchemaToHTML manifest="../org.eclipse.core.filesystem/plugin.xml" destination="${dest}" />
  <pde.convertSchemaToHTML manifest="../org.eclipse.core.resources/plugin.xml" destination="${dest}" />
  <pde.convertSchemaToHTML manifest="../org.eclipse.core.runtime/plugin.xml" destination="${dest}" />
  <pde.convertSchemaToHTML manifest="../org.eclipse.core.variables/plugin.xml" destination="${dest}" />
  <pde.convertSchemaToHTML manifest="../org.eclipse.debug.core/plugin.xml" destination="${dest}" />
  <pde.convertSchemaToHTML manifest="../org.eclipse.debug.ui/plugin.xml" destination="${dest}" />
  <pde.convertSchemaToHTML manifest="../org.eclipse.e4.ui.workbench/plugin.xml" destination="${dest}" />
  <pde.convertSchemaToHTML manifest="../org.eclipse.equinox.preferences/plugin.xml" destination="${dest}" />
  <pde.convertSchemaToHTML manifest="../org.eclipse.equinox.app/plugin.xml" destination="${dest}" />
  <pde.convertSchemaToHTML manifest="../org.eclipse.equinox.bidi/plugin.xml" destination="${dest}" />
  <pde.convertSchemaToHTML manifest="../org.eclipse.equinox.http.registry/plugin.xml" destination="${dest}" />
  <pde.convertSchemaToHTML manifest="../org.eclipse.equinox.registry/plugin.xml" destination="${dest}" />
  <pde.convertSchemaToHTML manifest="../org.eclipse.equinox.security/plugin.xml" destination="${dest}" />
  <pde.convertSchemaToHTML manifest="../org.eclipse.help/plugin.xml" destination="${dest}" />
  <pde.convertSchemaToHTML manifest="../org.eclipse.help.base/plugin.xml" destination="${dest}" />
  <pde.convertSchemaToHTML manifest="../org.eclipse.help.ui/plugin.xml" destination="${dest}" />
  <pde.convertSchemaToHTML manifest="../org.eclipse.help.webapp/plugin.xml" destination="${dest}" />
  <pde.convertSchemaToHTML manifest="../org.eclipse.ltk.core.refactoring/plugin.xml" destination="${dest}" />
  <pde.convertSchemaToHTML manifest="../org.eclipse.ltk.ui.refactoring/plugin.xml" destination="${dest}" />
  <pde.convertSchemaToHTML manifest="../org.eclipse.search/plugin.xml" destination="${dest}" />
  <pde.convertSchemaToHTML manifest="../org.eclipse.team.core/plugin.xml" destination="${dest}" />
  <pde.convertSchemaToHTML manifest="../org.eclipse.team.ui/plugin.xml" destination="${dest}" />
  <pde.convertSchemaToHTML manifest="../org.eclipse.ui/plugin.xml" destination="${dest}" />
  <pde.convertSchemaToHTML manifest="../org.eclipse.ui.browser/plugin.xml" destination="${dest}" />
  <pde.convertSchemaToHTML manifest="../org.eclipse.ui.cheatsheets/plugin.xml" destination="${dest}" />
  <!--copy extra files linked to by generated extension point doc-->
  <copy file="../org.eclipse.ui.cheatsheets/schema/cheatSheetContentFileSpec.html" todir="${dest}"/>
  <copy file="../org.eclipse.ui.cheatsheets/schema/cheatSheetContentFileSpec.exsd" todir="${dest}"/>
  <copy file="../org.eclipse.ui.cheatsheets/schema/compositeContentFile.html" todir="${dest}"/>
  <copy file="../org.eclipse.ui.cheatsheets/schema/compositeContentFile.exsd" todir="${dest}"/>

  <pde.convertSchemaToHTML manifest="../org.eclipse.ui.console/plugin.xml" destination="${dest}" />
  <pde.convertSchemaToHTML manifest="../org.eclipse.ui.editors/plugin.xml" destination="${dest}" />
  <pde.convertSchemaToHTML manifest="../org.eclipse.ui.externaltools/plugin.xml" destination="${dest}" />
  <pde.convertSchemaToHTML manifest="../org.eclipse.ui.ide/plugin.xml" destination="${dest}" />
  <pde.convertSchemaToHTML manifest="../org.eclipse.ui.intro/plugin.xml" destination="${dest}" />
  <!--copy extra HTML file linked to by generated extension point doc-->
  <copy file="../org.eclipse.ui.intro/schema/introContentFileSpec.html" todir="${dest}"/>
  <pde.convertSchemaToHTML manifest="../org.eclipse.ui.navigator/plugin.xml" destination="${dest}" />
  <pde.convertSchemaToHTML manifest="../org.eclipse.ui.views.properties.tabbed/plugin.xml" destination="${dest}" />
  <pde.convertSchemaToHTML manifest="../org.eclipse.ui.workbench.texteditor/plugin.xml" destination="${dest}" />
EOF


}

convertDot () {
  while read line; do
    NPATH="$line"
    PLUGIN=$( echo $NPATH | cut -f2 -d/ )
    PLUGIN_DIR=$( grep "${PLUGIN}\$" dirs.txt | head -1 )
    PLUGIN_BASE=$( echo $PLUGIN_DIR | sed "s/\/${PLUGIN}\$//g" | sed 's!/!.!g' )
    echo ${NPATH} |  sed "s/\.\./@${PLUGIN_BASE}@/g"
  done <<EOF
  ;../org.eclipse.equinox.p2.director/@dot
  ;../org.eclipse.equinox.p2.garbagecollector/@dot
  ;../org.eclipse.equinox.p2.metadata.repository/@dot
  ;../org.eclipse.equinox.p2.publisher/@dot
  ;../org.eclipse.equinox.simpleconfigurator/@dot
  ;../org.eclipse.equinox.supplement/@dot
  ;../org.eclipse.help.appserver/@dot
  ;../org.eclipse.osgi.services/@dot
  ;../org.eclipse.osgi/@dot

EOF
}

convertJdtOptions () {
  while read line; do
    NPATH="$line"
    PLUGIN=$( echo $NPATH | cut -f2 -d/ )
    PLUGIN_DIR=$( grep "${PLUGIN}\$" dirs.txt | head -1 )
    PLUGIN_BASE=$( echo $PLUGIN_DIR | sed "s/\/${PLUGIN}\$//g" | sed 's!/!.!g' )
    echo ${NPATH} |  sed "s/\.\./@${PLUGIN_BASE}@/g"
  done <<EOF
  ../org.eclipse.jdt.annotation/src
  ;../org.eclipse.ant.launching/src
  ;../org.eclipse.ant.ui/Ant Editor
  ;../org.eclipse.ant.ui/Ant Tools Support
  ;../org.eclipse.jdt.apt.core/src
  ;../org.eclipse.jdt.core/antadapter
  ;../org.eclipse.jdt.core/batch
  ;../org.eclipse.jdt.core/codeassist
  ;../org.eclipse.jdt.core/compiler
  ;../org.eclipse.jdt.core/dom
  ;../org.eclipse.jdt.core/eval
  ;../org.eclipse.jdt.core/formatter
  ;../org.eclipse.jdt.core/model
  ;../org.eclipse.jdt.core/search
  ;../org.eclipse.jdt.core.manipulation/common
  ;../org.eclipse.jdt.core.manipulation/refactoring
  ;../org.eclipse.jdt.debug/eval
  ;../org.eclipse.jdt.debug/jdi
  ;../org.eclipse.jdt.debug/model
  ;../org.eclipse.jdt.debug.ui/ui
  ;../org.eclipse.jdt.junit/src
  ;../org.eclipse.jdt.junit.core/src
  ;../org.eclipse.jdt.junit.runtime/src
  ;../org.eclipse.jdt.launching/launching
  ;../org.eclipse.jdt.ui/core extension
  ;../org.eclipse.jdt.ui/core refactoring
  ;../org.eclipse.jdt.ui/internal compatibility
  ;../org.eclipse.jdt.ui/ui
  ;../org.eclipse.jdt.ui/ui refactoring
EOF
}

convertJdtSchema () {
  while read line; do
    NPATH="$line"
    PLUGIN=$( echo $NPATH | cut -f2 -d/ )
    PLUGIN_DIR=$( grep "${PLUGIN}\$" dirs.txt | head -1 )
    PLUGIN_BASE=$( echo $PLUGIN_DIR | sed "s/\/${PLUGIN}\$//g" | sed 's!/!.!g' )
    echo ${NPATH} | sed "s/\.\./\${${PLUGIN_BASE}\}/g"
  done <<EOF
  <pde.convertSchemaToHTML manifest="../org.eclipse.jdt.apt.core/plugin.xml" destination="${dest}" />
  <pde.convertSchemaToHTML manifest="../org.eclipse.jdt.core/plugin.xml" destination="${dest}" />
  <pde.convertSchemaToHTML manifest="../org.eclipse.jdt.core.manipulation/plugin.xml" destination="${dest}" />
  <pde.convertSchemaToHTML manifest="../org.eclipse.jdt.debug/plugin.xml" destination="${dest}" />
  <pde.convertSchemaToHTML manifest="../org.eclipse.jdt.debug.ui/plugin.xml" destination="${dest}" />
  <pde.convertSchemaToHTML manifest="../org.eclipse.jdt.junit/plugin.xml" destination="${dest}" />
  <pde.convertSchemaToHTML manifest="../org.eclipse.jdt.junit.core/plugin.xml" destination="${dest}" />
  <pde.convertSchemaToHTML manifest="../org.eclipse.jdt.launching/plugin.xml" destination="${dest}" />
  <pde.convertSchemaToHTML manifest="../org.eclipse.jdt.ui/plugin.xml" destination="${dest}" />

EOF
}

convertPDESchema () {
  while read line; do
    NPATH="$line"
    PLUGIN=$( echo $NPATH | cut -f2 -d/ )
    PLUGIN_DIR=$( grep "${PLUGIN}\$" dirs.txt | head -1 )
    PLUGIN_BASE=$( echo $PLUGIN_DIR | sed "s/\/${PLUGIN}\$//g" | sed 's!/!.!g' )
    echo ${NPATH} | sed "s/\.\./\${${PLUGIN_BASE}\}/g"
  done <<EOF
  <pde.convertSchemaToHTML manifest="../org.eclipse.pde.core/plugin.xml" destination="${dest}" />
  <pde.convertSchemaToHTML manifest="../org.eclipse.pde.build/plugin.xml" destination="${dest}" />
  <pde.convertSchemaToHTML manifest="../org.eclipse.pde.ui/plugin.xml" destination="${dest}" />
  <pde.convertSchemaToHTML manifest="../org.eclipse.pde.launching/plugin.xml" destination="${dest}" />
EOF

}

convertPDEOptions () {
  while read line; do
    NPATH="$line"
    PLUGIN=$( echo $NPATH | cut -f2 -d/ )
    PLUGIN_DIR=$( grep "${PLUGIN}\$" dirs.txt | head -1 )
    PLUGIN_BASE=$( echo $PLUGIN_DIR | sed "s/\/${PLUGIN}\$//g" | sed 's!/!.!g' )
    echo ${NPATH} |  sed "s/\.\./@${PLUGIN_BASE}@/g"
  done <<EOF
  ../org.eclipse.pde.core/src
  ;../org.eclipse.pde.ui/src
  ;../org.eclipse.pde.build/src
  ;../org.eclipse.pde.launching/src
EOF
}

convertPDEOptions

