<?php
# Begin: page-specific settings.
$pageTitle    = "Description of Deliverable";
$pageKeywords = "eclipse,project,plug-ins,plugins,java,ide,swt,refactoring,free java ide,tools,platform,open source,development environment,development,ide";
$pageAuthor   = "David Williams and Christopher Guindon";
require("DL.thin.header.php.html");
?>

  <div id="leftcol">
    <ul id="leftnav">
      <li><a href="#Repository">Eclipse p2 Repository</a></li>
      <li><a href="#EclipseSDK">Eclipse SDK</a></li>
      <li><a href="#JUnitPlugin">JUnit Plugin Tests and Automated Testing Framework</a></li>
      <li><a href="#ExamplePlugins">Example Plug-ins</a></li>
      <li><a href="#RCPRuntime">RCP Runtime Binary</a></li>
      <li><a href="#RCPSDK">RCP SDK</a></li>
      <!-- <li><a href="#DeltaPack">Delta Pack</a></li> -->
      <li><a href="#PlatformRuntime">Platform Runtime Binary</a></li>
      <li><a href="#JDTRuntime">JDT Runtime Binary</a></li>
      <li><a href="#JDTSDK">JDT SDK</a></li>
      <li><a href="#JDTCORE">JDT Core Batch Compiler</a></li>
      <li><a href="#PDERuntime">PDE Runtime Binary</a></li>
      <li><a href="#PDESDK">PDE SDK</a></li>
      <li><a href="#CVSRuntime">CVS Client Runtime Binary</a></li>
      <li><a href="#CVSSDK">CVS Client SDK</a></li>
      <li><a href="#SWT">SWT binary and Source</a></li>
      <li><a href="#orgeclipsereleng">org.eclipse.releng.tools plug-in</a></li>
    </ul>

  </div>

  <div id="midcolumn">
    <h2>Download Details</h2>

    <div class="homeitem3col">
      <div class="container">
      <dl class="midlist" >
        <dt id="Repository" data-toggle="collapse" data-target="#RepositoryDD">Eclipse Repository</dt>
        <dd id="RepositoryDD" class="collapse">The Eclipse Repository includes all that is produced by the Eclipse Project, including the Eclipse Platform,
        Java development tools, and Plug-in Development Environment, Unit Tests, and even some extra items from other projects
        required by Eclipse (such as Equinox and a few bundles from EMF and Orbit). Please be aware that repositories have
        different retention policies, and restrictions on what types can be updated with what other types. See the wiki's <a
          href="http://wiki.eclipse.org/Eclipse_Project_Update_Sites">Update Sites</a> document for details.
        </dd>
        <dt id="EclipseSDK"  data-toggle="collapse" data-target="#EclipseSDKDD">Eclipse SDK</dt>
        <dd id="EclipseSDKDD" class="collapse">The Eclipse SDK includes the Eclipse Platform, Java development tools, and Plug-in Development Environment,
        including source and both user and programmer documentation. If you aren't sure which download you want... then you
        probably want this one. You will need a <a href="http://wiki.eclipse.org/Eclipse/Installation#Install_a_JVM">Java
          runtime environment (JRE)</a> to use Eclipse (Java SE 6 or greater is recommended).
        </dd>
        <dt id="JUnitPlugin" data-toggle="collapse" data-target="#JUnitPluginDD">JUnit Plugin Tests and Automated Testing Framework</dt>

        <dd id="JUnitPluginDD" class="collapse">These packages contain the Test Framework and JUnit test plugins used to run JUnit plug-in tests from the
        command line. See the Platform&apos;s <a href="http://wiki.eclipse.org/Platform-releng/Automated_Testing">Automated
          Testing</a> wiki page for more information and setup instructions. Includes both source code and binary.
        </dd>
        <dt id="ExamplePlugins" data-toggle="collapse" data-target="#ExamplePluginsDD">Example Plug-ins</dt>
        <dd id="ExamplePluginsDD" class="collapse">To install the examples, download the p2 repository zip containing the examples into a directory on disk.
        Select Help -&gt; Install New Software. Select Add to add a new software site. Select Archive and
        specify the location of the examples p2 repository zip and Okay. You will be prompted to restart Eclipse to
        enable the new bundles. For information on what the examples do and how to run them, look in the &quot;Examples
        Guide&quot; section of the &quot;Platform Plug-in Developer Guide&quot;, by selecting Help Contents from the Help
        menu, and choosing &quot;Platform Plug-in Developer Guide&quot; book from the combo box.
        </dd>
        <dt id="RCPRuntime"  data-toggle="collapse" data-target="#RCPRuntimeDD">RCP Runtime Binary</dt> 
         <dd id="RCPRuntimeDD" class="collapse">This p2 repository contains the Eclipse Rich Client Platform base bundles and do not contain source or
        programmer documentation. These downloads are meant to be used as target platforms when developing RCP applications,
        and are not executable, stand-alone applications.</dd>
        <dt id="RCPSDK" data-toggle="collapse" data-target="#RCPSDKDD">RCP SDK</dt> 
        <dd id="RCPSDKDD" class="collapse">This p2 repository consists of the Eclipse Rich Client Platform base bundles and their source and the RCP delta
        pack.</dd>
        <dt id="DeltaPack" data-toggle="collapse" data-target="#DeltaPackDD">Delta Pack</dt> 
        <dd id="DeltaPackDD" class="collapse">The delta pack contains all the platform specific resources from the SDK and is used for cross-platform exports
        of RCP applications.</dd>
        <dt id="PlatformRuntime" data-toggle="collapse" data-target="#PlatformRuntimeDD">Platform Runtime Binary</dt>
        <dd id="PlatformRuntimeDD" class="collapse">These drops contain only the Eclipse Platform with user documentation and no source and no programmer
        documentation. The Java development tools and Plug-in Development Environment are NOT included. You can use these
        drops to help you package your tool plug-ins for redistribution when you don&apos;t want to ship the entire SDK.</dd>
        <dt id="PlatformSDK" data-toggle="collapse" data-target="#PlatformSDKDD">Platform SDK</dt> 
        <dd id="PlatformSDKDD" class="collapse">These drops contain the Eclipse Platform Runtime binary with associated source and programmer documentation.</dd>
        <dt id="JDTRuntime" data-toggle="collapse" data-target="#JDTRuntimeDD">JDT Runtime Binary</dt>
        <dd id="JDTRuntimeDD" class="collapse">This p2 repository contains the Java development tools bundles only, with user documentation and no source and
        no programmer documentation. The Eclipse platform and Plug-in development environment are NOT included. You can
        combine this with the Platform Runtime Binary if your tools rely on the JDT being present.</dd>
        <dt id="JDTSDK" data-toggle="collapse" data-target="#JDTSDKDD">JDT SDK</dt> 
        <dd id="JDTSDKDD" class="collapse">This p2 repository contains the JDT Runtime binary with associated source and programmer documentation.</dd>
        <dt id="JDTCORE" data-toggle="collapse" data-target="#JDTCOREDD">JDT Core Batch Compileri</dt> 
        <dd id="JDTCOREDD" class="collapse">These drops contain the standalone batch java compiler, Ant compiler adapter and associated source. The batch
        compiler and Ant adapter (ecj.jar) are extracted from the org.eclipse.jdt.core plug-in as a 1.2MB download. For
        examples of usage, please refer to this help section: JDT Plug-in Developer Guide&gt;Programmer&apos;s Guide&gt;JDT
        Core&gt;Compiling Java code.</dd>
        <dt id="PDERuntime" data-toggle="collapse" data-target="#PDERuntimeDD">PDE Runtime Binary</dt> 
        <dd id="PDERuntimeDD" class="collapse">This p2 repository contains the Plug-in Development Enviroment bundles only, with user documentation. The
        Eclipse platform and Java development tools are NOT included. You can combine this with the Platform and JDT Runtime
        Binary or SDK if your tools rely on the PDE being present.</dd>
        <dt id="PDEProducts" data-toggle="collapse" data-target="#PDEProductsDD">PDE Build Products</dt> 
        <dd id="PDEProductsDD" class="collapse">The PDE Builders are self-contained, executable PDE Build configurations that can be used to build OSGi and
        Eclipse-based systems. They can also be used as the basis for more sophisticated build systems that run tests, do API
        scans, publish builds etc.</dd>
        <dt id="PDESDK" data-toggle="collapse" data-target="#PDESDKDD">PDE SDK</dt> 
        <dd id="PDESDKDD" class="collapse">These drops contain the PDE Runtime Binary with associated source.</dd>
        <dt id="CVSRuntime" data-toggle="collapse" data-target="#CVSRuntimeDD">CVS Client Runtime Binary</dt>
        <dd id="CVSRuntimeDD" class="collapse">This p2 repository contains the CVS Client plug-ins only. The Eclipse platform, Java development, and Plug-in
        Development Environment tools are NOT included. You can combine this with the Platform and JDT Runtime Binary or SDK
        if your tools rely on the CVS client being present.</dd>
        <dt id="CVSSDK" data-toggle="collapse" data-target="#CVSSDKDD">CVS Client SDK</dt> 
        <dd id="CVSSDKDD" class="collapse">This p2 repository contains the CVS Runtime Binary with associated source.</dd>
        <dt id="SWT" data-toggle="collapse" data-target="#SWTDD">SWT Binary and Source</dt>
        <dd id="SWTDD" class="collapse">
        These drops contain the SWT libraries and source for standalone SWT application development. For examples of
        standalone SWT applications refer to the <a href="http://www.eclipse.org/swt/snippets/">snippets</a> section of the
        SWT Component page.

        <p>To run a standalone SWT application, add the swt jar(s) to the classpath and add the directory/folder for the
        SWT JNI library to the java.library.path. For example, if you extract the download below to C:\SWT you would launch
        the HelloWorld application with the following command:</p>
        <code>java -classpath C:\SWT\swt.jar;C:\MyApp\helloworld.jar -Djava.library.path=C:\SWT HelloWorld</code>
        <p>
        Note that if you are running on Eclipse 3.3 or later, you do not need to specify the library path, so you
        would launch the HelloWorld application with the following command:
        </p>
        <code>java -classpath C:\SWT\swt.jar;C:\MyApp\helloworld.jar HelloWorld</code>
        <p>
        To run the standalone SWT examples that are shipped with Eclipse, download them from <a
          href="index.php#ExamplePlugins">here</a>. Then copy the file
        eclipse\plugins\org.eclipse.swt.examples_xxx\swtexamples.jar to C:\SWT. Now you can run the examples that are
        described <a href="http://www.eclipse.org/swt/examples.php">here</a>. For example:
        </p>
        <code>cd C:\SWT<br /> java -classpath swt.jar;swtexamples.jar org.eclipse.swt.examples.controlexample.ControlExample</code>
        <p>On Linux systems, note that the classpath separator character is a colon, so the equivalent command becomes:</p>
        <code>java -classpath swt.jar:swtexamples.jar org.eclipse.swt.examples.controlexample.ControlExample</code>
        </dd>

        <dt id="orgeclipsereleng" data-toggle="collapse" data-target="#orgeclipserelengDD">Releng Tools (org.eclipse.releng.tools)</dt>
        <dd id="orgeclipserelengDD" class="collapse">This plug-in provides features that can help committers with the Eclipse development process. It is not intended as
        a "tool to extend", or provide API, etc. It is a simple utility. You can install the tool from the usual Eclipse Project
        repositories, or the zipped repo provided on the download page. Since it uses several &quot;internal&quot; non-API methods, you
        may have to have one that "matches" the version of your development environment.
        <p>Currently, the tool provides two important functions.</p>
        <ul>
          <li>Fix Copyrights. In the Resource Perspective Projects context menu. Select one or more projects in the
          Resource Perspective. This action will sanity check the copyright notices in all the *.java and *.properties files. It
          compares the "end dates" with the last time the file was changed. It works with either Git repositories, or CVS.
          Copyrights will be updated automatically where the tool deems appropriate. A copyright.log file will be written to the
          workspace directory noting odd conflicts that need to be looked at and manually confirmed or modified. You will need
          to commit any changes yourself.</li>
          <li>Validate Versions Match. Once turned on, in preferences, the bundle version in the MANIFEST.MF file will
          be compared with the artifact version in the pom.xml file. If they "mismatch", then a marker is left in problems view,
          so the incorrect version can be fixed before being committed for a build. Mismatched versions can cause Tycho/Maven
          builds to fail and it is easy to change the version on one spot and forget the other, so all committers are encourage
          to use and turn on this tool.</li>
        </ul>
        <p>Older tools for use with CVS: The following tools are for using with CVS map files, and while we have every
        expectation they still work fine, they are not actively used by many committers now that most have moved to use to Git,
        so in theory they might be less stable. If you still have a use for them, that's great, and if you find bugs, we'll
        accept them as valid, but will likely require a high quality patch before much effort is spent on it, since they are a
        low priority for the Eclipse Platform team.</p>
        <ul>
          <li>Release to the Team menu. This action will Tag selected projects with the specified version and
          update the appropriate loaded *.map files with the version. The user must have the *.map files loaded in their
          workspace and the use must commit the map file changes to the repository when done.</li>
          <li>Load Map Projects to the Team menu. Select one or more *.map file and this action will load the projects
          listed in the *.map file into your workspace. Naturally the versions specified in the *.map file will be loaded.</li>
          <li>Tag Map Projects to the Team menu. Select one or more *Map files and this action will tag the projects
          listed in the *Map files with a tag you specify.</li>
          <li>Compare with Released to the Compare menu. Compare the selected projects with the versions referenced in
          the currently loaded map files.</li>
          <li>Replace with Released to the Replace menu. Replace the selected projects with the versions referenced in
          the currently loaded map files.</li>
        </ul>

        </dd>
      </dl>
    </div>
  </div>
</div> <!-- close div classs=container -->
</main> <!-- close main role="main" element -->
    <script src="https://ajax.googleapis.com/ajax/libs/jquery/1.11.1/jquery.min.js"></script>
    <script src="http://maxcdn.bootstrapcdn.com/bootstrap/3.2.0/js/bootstrap.min.js"></script>
</body>
</html>
<?php
  $html = ob_get_clean();

  #echo the computed content
  echo $html;
?>

