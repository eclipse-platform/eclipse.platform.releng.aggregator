<?php
# Begin: page-specific settings.  Change these.
$pageTitle    = "Eclipse Project Downloads";
$pageKeywords = "eclipse platform sdk pde jdt downloads";
$pageAuthor   = "David Williams and Christopher Guindon";

//ini_set("display_errors", "true");
//error_reporting (E_ALL);
$eclipseStream="4";
include('../../dlconfig4.php');
$subdirDrops="drops4";
$expectedtestConfigs=0;
$testConfigs = array();

ob_start();

/*
DL.thin.header.php.html was original obtained from

wget https://eclipse.org/eclipse.org-common/themes/solstice/html_template/thin/header.php

and then that file modified to suit our needs.
Occasionally, our version should be compared to the "standard" to see if anything has
changed, in the interest of staying consistent.

See https://eclipse.org/eclipse.org-common/themes/solstice/docs/

 */

require("../../DL.thin.header.php.html");


include_once("buildproperties.php");
include_once("utilityFunctions.php");

// global variables
$expectedTestConfigs=array();
$testResults = array();
$testResultsSummaryFiles=array();

$streamArr = explode(".", $STREAM);
$STREAM_MAJOR = $streamArr[0];
$STREAM_MINOR = $streamArr[1];
$STREAM_SERVICE = $streamArr[2];
?>


<?php if (! isset ($BUILD_FAILED) ) { ?>

<aside class="col-md-4" id="leftcol">
<ul id="leftnav" class="ul-left-nav fa-ul hidden-print">
    <li><a href="#Repository">Eclipse p2 Repository</a></li>
    <li><a href="#EclipseSDK">Eclipse SDK</a></li>
    <li><a href="#JUnitPlugin">JUnit Plugin Tests and Automated Testing Framework</a></li>
    <li><a href="#ExamplePlugins">Example Plug-ins</a></li>
    <li><a href="#RCPRuntime">RCP Runtime Binary</a></li>
    <li><a href="#RCPSDK">RCP SDK</a></li>
    <li><a href="#DeltaPack">Delta Pack</a></li>
    <li><a href="#PlatformRuntime">Platform Runtime Binary</a></li>
    <li><a href="#JDTRuntime">JDT Runtime Binary</a></li>
    <li><a href="#JDTSDK">JDT SDK</a></li>
    <li><a href="#JDTCORE">JDT Core Batch Compiler</a></li>
    <li><a href="#PDERuntime">PDE Runtime Binary</a></li>
    <li><a href="#PDESDK">PDE SDK</a></li>
    <li><a href="#CVSRuntime">CVS Runtim</a></li>
    <li><a href="#CVSSDK">CVS SDK</a></li>
    <li><a href="#SWT">SWT binary and Source</a></li>
    <li><a href="#org.eclipse.releng">Releng Tools</a></li>
</ul>
</aside>

<!-- end 'not build failed' -->
<?php } ?>

<div>
<h1>Eclipse <?php echo $STREAM; ?> <?php echo $BUILD_TYPE_NAME; ?> Build: <?php echo $BUILD_ID; ?> </h1>

<?php
if (file_exists("pom_updates/index.html")) {
  echo "<tr><td width=\"75%\"><font size=\"+1\">";
  echo "<a href=\"pom_updates/\"><b>POM updates made</b></a></font></td></tr>";
}
// check if test build only, just to give warning of oversite.
// see bug 404545
if (isset($testbuildonly) && ($testbuildonly)) {
  echo "<tr><td width=\"75%\">\n";
  echo "<font size=\"+1\">\n";
  echo "Test-Build-Only flag found set. Input was not tagged.\n";
  echo "</font>\n";
  echo "</td></tr>\n";
}

// Use of "BUILD_ID" should work for milestones, and releases, but would require a "copy"
// being made, say for "4.5RC4" during that period we have prepared final bits, but have not
// made visible yet (if we want "N&N" visible from RC build).

if (file_exists("news/")) {
  echo "<tr><td width=\"75%\"><font size=\"+1\">";
  echo "<a href=\"http://www.eclipse.org/eclipse/news/${BUILD_ID}/eclipse_news_${BUILD_ID}.php\"><b>New and Noteworthy</b></a></font></td></tr>";
}
// linkToAcknowledgements is a pure "marker file"
if (file_exists("linkToAcknowledgements")) {
  echo "<tr><td width=\"75%\"><font size=\"+1\">";
  echo "<a href=\"http://www.eclipse.org/eclipse/development/acknowledgements_${BUILD_ID}.php\"><b>Acknowledgments</b></a>";
  echo "</font></td></tr>";
}
// linkToReadme is a pure marker file
if (file_exists("linkToReadme")) {
  echo "<tr><td width=\"75%\"><font size=\"+1\">";
  echo "<a href=\"http://www.eclipse.org/eclipse/development/readme_eclipse_${BUILD_ID}.php\"><b>Eclipse Project ${BUILD_ID} Readme</b></a>";
  echo "</font></td></tr>";
}

if (isset ($BUILD_FAILED) ) {
  echo "<tr><td width=\"75%\"><font size=\"+1\">";
  echo "Build Failed. See <a href=\"buildlogs.php\">logs</a>. <br />\n";
  $PATTERN='!(.*)(/buildlogs/)(.*)!';
  $result = preg_match($PATTERN, $BUILD_FAILED, $MATCHES);
  // cheap short cut, since we expect only 1 such file
  $summaryFile=glob("buildFailed-*");
  if ($result !== FALSE) {
    $SPECIFIC_LOG=$MATCHES[3];
    echo "Specifically, see <a href=\"buildlogs/$SPECIFIC_LOG\">the log with errors</a>, \n";
    echo "or a <a href=\"$summaryFile[0]\">summary</a>. <br /> \n";
    echo "Or see traditional <a href=\"testResults.php\">Compile Logs</a> (if any). \n";
  }


}
else {
?>
 <p style="padding-bottom: 1em">This page provides access to the various deliverables of Eclipse Platform build along with
is logs and tests.</p>
</div>

<div id="midcolumn">

<h3>Logs and Test Links</h3>

<?php
  // for current (modern) builds, test results are always in
  // 'testresults'. That directory only exists after first results
  // have finished and been "published".
  if (file_exists("testresults")) {
    $testResultsDirName="testresults";
  } elseif (file_exists("results")) {
    $testResultsDirName="results";
  } else {
    $testResultsDirName="";
  }


  $boxes=calcTestConfigsRan($testResultsDirName);
  if ($boxes < 0 ) {
    $boxesDisplay = 0;
  } else {
    $boxesDisplay = $boxes;
  }

  //  echo "<ul class='midlist'>";
  echo "<ul>";
  //  We will always display link to logs (as normal link, not using color:inherit;)
  echo "<li>View the <a  style=\"text-decoration:none\" title=\"Link to logs.\" href=\"testResults.php\">logs for the current build</a>.</li>\n";

  // This section if for overall status if anything failed, overall is failed
  // -3 is special code meaning no testResults directory exists yet.
  if ($boxes == -3)   {
    $testResultsStatus = "pending";
  } else {
    /* since boxes is not -3, there must be at least one */
    $totalFailed = 0;
    $expectedBoxes = count($expectedTestConfigs);
    foreach ($expectedTestConfigs as $config) {

      if (isset($testResults[$config])) {
        $testRes = $testResults[$config];
        $failed = $testRes['failCount'];
        $totalFailed = $totalFailed + $failed;
      }
    }
    if ($totalFailed == 0 && $boxes == $expectedBoxes) {
      $testResultsStatus = "success";
    } elseif ($totalFailed == 0 && $boxes < $expectedBoxes) {
      $testResultsStatus = "inProgress";
    } elseif ($totalFailed > 0 && $boxes > 0) {
      $testResultsStatus = "failed";
    } else {
      // This is some sort of programming error?
      // Don't think we should get to here?
      // Will flag as "unknown" but not sure how to convey that ....
      // would only be useful if debugging.
      $testResultsStatus = "unknown";
    }
  }

  if (file_exists("overrideTestColor")) {
    $linkColor='green';
  }
  else {
    if ($testResultsStatus === "failed") {
      /* note we don't override  'inherit' cases, just 'failed'. */
      if (file_exists("overrideTestColor")) {
        $linkColor='green';
      } else {
        $linkColor = 'red';
      }
    } elseif ($testResultsStatus === "success") {
      $linkColor='green';
    } elseif ($testResultsStatus === "pending") {
      $linkColor='inherit';
    } elseif ($testResultsStatus === "inProgress") {
      $linkColor='yellow';
    }
  }


  if ($testResultsStatus == "pending")   {
    echo "<li>Integration and unit tests are pending.</li>\n";
  } else {
    echo "<li>View the <a  style=\"color:${linkColor};text-decoration:none\" title=\"Link test results.\" href=\"testResults.php\">integration and unit test results for the current build.</a></li>\n";
  }


  /* performance tests line item */
  $generated=file_exists("performance/global_fp.php");
  if (file_exists("performance.php") && $generated) {
    echo "<li>View the <a href=\"performance.php\">performance test results</a> for the current build.</li>\n";
  } else {
    echo "<li>Performance tests are pending.</li>\n";
  }

  echo "</ul>\n";

  echo "<h3>Summary of Unit Tests Results</h3>";
  echo "<table class=\"testTable\">\n";
  echo "<caption>".$boxesDisplay." of ".count($expectedTestConfigs)." integration and unit test configurations are complete.</caption> \n";
  echo "<tr><th style=\"width:40%\">Tested Platform</th><th>Failed</th><th>Passed</th><th>Total</th><th>Test&nbsp;Time&nbsp;(s)</th></tr>\n";

  foreach ($expectedTestConfigs as $config) {

    if (isset($testResults[$config])) {
      $testRes = $testResults[$config];
      $failed = $testRes['failCount'];
      $passed = $testRes['passCount'];
      $total = $failed + $passed;
      $duration = $testRes['duration'];
      if (file_exists("overrideTestColor")) {
        $linkColor='green';
      }
      else {
        if ($failed > 0) {
          /* note we don't override  'inherit' cases, just 'failed'. */
          if (file_exists("overrideTestColor")) {
            $linkColor='green';
          } else {
            $linkColor = 'red';
          }
        } else {
          $linkColor='green';
        }
      }
      echo "<tr>\n";
      echo "<td style=\"text-align:left\">\n";
      echo "<a style=\"color:${linkColor};text-decoration:none\" href=\"testResults.php\">".$config."</a>";
      echo "</td>\n";
      echo "<td>$failed</td><td>$passed</td><td>$total</td><td>$duration</td>\n";
      echo "</tr>\n";
    }
    else {
      /* Yes, all configs intentionally links, since all go to the same place, but if no results yet, would not look like one. */
      $linkColor = 'inherit';
      echo "<tr>\n";
      echo "<td style=\"text-align:left\">\n";
      echo "<a style=\"color:${linkColor};text-decoration:none\" href=\"testResults.php\">".$config."</a>";
      echo "</td>\n";
      echo "<td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td>\n";
      echo "</tr>\n";
    }
  }
  echo "</table>\n";


?>

  <h3>Related Links</h3>
  <ul class="midlist">
    <li><a href="https://www.eclipse.org/eclipse/development/plans/eclipse_project_plan_4_4.xml#target_environments">Target Platforms and Environments</a></li>
    <li><a href="directory.txt">View the Git repositories used for the current build.</a></li>
    <li><a href="http://wiki.eclipse.org/Platform-releng/How_to_check_integrity_of_downloads">How to verify a download.</a></li>

<?php
  # place holder: we don't currently produce these reports, and
  # when we do, will need some work here.
  # FWIW, we may want to construct elaborate query into CGit for this,
  # even though that'd be elaborate, would get user to an area where
  # they coudl tweak query, if desired?
  if (file_exists("report.txt")) {
    echo "<p><a href=\"report.txt\">Report of changes</a> from previous build.</p>";
  }
?>

<?php
  if (file_exists("buildnotes/")) {
    echo "<li><a href=\"buildNotes.php\">View build notes for the current build.</a></li>";
  }
?>
</ul>
</div> <!-- end midcolumn -->

<?php
  include("dropSectionUtils.php");
  include("computeRepoURLs.php");
?>
 <!-- main download section -->
<div class="dropSection">
<h3 id="Repository">Eclipse p2 Repository&nbsp;<a href="details.html#Repository"><img src="images/more.gif" title="More..." alt="[More]" /></a></h3>

<?php startTable(); ?>

<?php
  if ((file_exists("$relativePath3/updates/".$STREAM_REPO_NAME)) || (file_exists("$relativePath4/updates/".$STREAM_REPO_NAME))) {
    echo "<tr><td> \n";
    echo "To update your Eclipse installation to this development stream, you can use the software repository at<br />\n";
    echo "&nbsp;&nbsp;<a href=\"$STREAM_REPO_URL\">$STREAM_REPO_URL</a><br />\n";
    echo "</td></tr> \n";
  }
  if ((file_exists("$relativePath3/updates/"."$BUILD_REPO_NAME")) || (file_exists("$relativePath4/updates/"."$BUILD_REPO_NAME")) ) {
    echo "<tr><td> \n";
    echo "To update your build to use this specific build, you can use the software repository at<br />\n";
    echo "&nbsp;&nbsp;<a href=\"$BUILD_REPO_URL\">$BUILD_REPO_URL</a><br />\n";
    echo "</td></tr> \n";
  }
?>
</table>



<h3 id="EclipseSDK"> Eclipse SDK&nbsp;<a href="details.html#EclipseSDK"><img
     src="images/more.gif" title="More..." alt="[More]" /></a>
</h3>

<?php startTable(); ?>
<tr>
   <?php columnHeads(); ?>
</tr>

<tr>
<td>Windows</td>
<?php genLinks("eclipse-SDK-${BUILD_ID}-win32.zip"); ?>
</tr>
<tr>
<td>Windows (x86_64)</td>
<?php genLinks("eclipse-SDK-${BUILD_ID}-win32-x86_64.zip"); ?>
</tr>
<tr>
<td>Linux (x86/GTK+)</td>
<?php genLinks("eclipse-SDK-${BUILD_ID}-linux-gtk.tar.gz"); ?>
</tr>
<tr>
<td>Linux (x86_64/GTK+)</td>
<?php genLinks("eclipse-SDK-${BUILD_ID}-linux-gtk-x86_64.tar.gz"); ?>
</tr>
<tr>
<td>Linux (PPC/GTK+)</td>
<?php genLinks("eclipse-SDK-${BUILD_ID}-linux-gtk-ppc.tar.gz"); ?>
</tr>
<tr>
<td>Linux (PPC64/GTK+)</td>
<?php genLinks("eclipse-SDK-${BUILD_ID}-linux-gtk-ppc64.tar.gz"); ?>
</tr>
<tr>
<td>Linux (s390x/GTK+)</td>
<?php genLinks("eclipse-SDK-${BUILD_ID}-linux-gtk-s390x.tar.gz"); ?>
</tr>
<tr>
<td>Linux (s390/GTK+)</td>
<?php genLinks("eclipse-SDK-${BUILD_ID}-linux-gtk-s390.tar.gz"); ?>
</tr>
<tr>
<td>Linux (PPC64LE/GTK+)</td>
<?php genLinks("eclipse-SDK-${BUILD_ID}-linux-gtk-ppc64le.tar.gz"); ?>
</tr>
<tr>
<td>Solaris 10 (SPARC/GTK+)</td>
<?php genLinks("eclipse-SDK-${BUILD_ID}-solaris-gtk.zip"); ?>
</tr>
<tr>
<td>Solaris 10 (x86/GTK+)</td>
<?php genLinks("eclipse-SDK-${BUILD_ID}-solaris-gtk-x86.zip"); ?>
</tr>
<tr>
<td>HP-UX (ia64/GTK+)</td>
<?php genLinks("eclipse-SDK-${BUILD_ID}-hpux-gtk-ia64.zip"); ?>
</tr>
<tr>
<td>AIX (PPC/GTK+)</td>
<?php genLinks("eclipse-SDK-${BUILD_ID}-aix-gtk-ppc.zip"); ?>
</tr>
<tr>
<td>AIX (PPC64/GTK+)</td>
<?php genLinks("eclipse-SDK-${BUILD_ID}-aix-gtk-ppc64.zip"); ?>
</tr>
<tr>
<td>Mac OSX (Mac/Cocoa/x86_64)</td>
<?php genLinks("eclipse-SDK-${BUILD_ID}-macosx-cocoa-x86_64.tar.gz"); ?>
</tr>

      </table>



      <h3 id="JUnitPlugin"> JUnit Plugin Tests and Automated Testing Framework&nbsp;<a href="details.html#JUnitPlugin"><img
            src="images/more.gif" title="More..." alt="[More]" /></a>
      </h3>
      <?php startTable(); ?> 
        <tr>
          <?php columnHeads(); ?>
        </tr>
        <tr>
<td><img src = "repo.gif" alt="Test Framework" /> All</td>
<?php genLinks("eclipse-test-framework-${BUILD_ID}.zip"); ?>
</tr>
<tr>
<td>All</td>
<?php genLinks("eclipse-Automated-Tests-${BUILD_ID}.zip"); ?>
</tr>

      </table>



      <h3 id="ExamplePlugins"> Example Plug-ins&nbsp;<a href="details.html#ExamplePlugins"><img
            src="images/more.gif" title="More..." alt="[More]" /></a>
      </h3>
      <?php startTable(); ?>
        <tr>

          <?php columnHeads(); ?>
        </tr>
        <tr>
<td><img src = "repo.gif" alt="Examples" /> Eclipse Examples Repo</td>
<?php genLinks("org.eclipse.sdk.examples.source-${BUILD_ID}.zip"); ?>
</tr>

      </table>

 

      <h3 id="RCPRuntime"> RCP Runtime Binary&nbsp;<a href="details.html#RCPRuntime"><img
            src="images/more.gif" title="More..." alt="[More]" /></a>
      </h3>
      <?php startTable(); ?>
        <tr>

          <?php columnHeads(); ?>
        </tr>
        <tr>
<td><img src = "repo.gif" alt="RCP Repo" /> RCP Runtime Repo </td>
<?php genLinks("org.eclipse.rcp-${BUILD_ID}.zip"); ?>
</tr>

      </table>

 

      <h3 id="RCPSDK"> RCP SDK&nbsp;<a href="details.html#RCPSDK"><img
            src="images/more.gif" title="More..." alt="[More]" /></a>
      </h3>
      <?php startTable(); ?>
        <tr>

          <?php columnHeads(); ?>
        </tr>
        <tr>
<td><img src = "repo.gif" alt="RCP Source Repo" /> RCP Source Repo </td>
<?php genLinks("org.eclipse.rcp.source-${BUILD_ID}.zip"); ?>
</tr>

      </table>

 

      <h3 id="DeltaPack"> DeltaPack&nbsp;<a href="details.html#DeltaPack"><img
            src="images/more.gif" title="More..." alt="[More]" /></a>
      </h3>
      <?php startTable(); ?>
        <tr>

          <?php columnHeads(); ?>
        </tr>
        <tr>
<td>All</td>
<?php genLinks("eclipse-${BUILD_ID}-delta-pack.zip"); ?>
</tr>

      </table>

 

      <h3 id="PlatformRuntime"> Platform Runtime Binary&nbsp;<a href="details.html#PlatformRuntime"><img
            src="images/more.gif" title="More..." alt="[More]" /></a>
      </h3>
      <?php startTable(); ?>
        <tr>

          <?php columnHeads(); ?>
        </tr>
        <tr>
<td>Windows</td>
<?php genLinks("eclipse-platform-${BUILD_ID}-win32.zip"); ?>
</tr>
<tr>
<td>Windows (x86_64)</td>
<?php genLinks("eclipse-platform-${BUILD_ID}-win32-x86_64.zip"); ?>
</tr>
<tr>
<td>Linux (x86/GTK+)</td>
<?php genLinks("eclipse-platform-${BUILD_ID}-linux-gtk.tar.gz"); ?>
</tr>
<tr>
<td>Linux (x86_64/GTK+)</td>
<?php genLinks("eclipse-platform-${BUILD_ID}-linux-gtk-x86_64.tar.gz"); ?>
</tr>
<tr>
<td>Linux (PPC/GTK+)</td>
<?php genLinks("eclipse-platform-${BUILD_ID}-linux-gtk-ppc.tar.gz"); ?>
</tr>
<tr>
<td>Linux (PPC64/GTK+)</td>
<?php genLinks("eclipse-platform-${BUILD_ID}-linux-gtk-ppc64.tar.gz"); ?>
</tr>
<tr>
<td>Linux (s390x/GTK+)</td>
<?php genLinks("eclipse-platform-${BUILD_ID}-linux-gtk-s390x.tar.gz"); ?>
</tr>
<tr>
<td>Linux (s390/GTK+)</td>
<?php genLinks("eclipse-platform-${BUILD_ID}-linux-gtk-s390.tar.gz"); ?>
</tr>
<tr>
<td>Linux (PPC64LE/GTK+)</td>
<?php genLinks("eclipse-platform-${BUILD_ID}-linux-gtk-ppc64le.tar.gz"); ?>
</tr>
<tr>
<td>Solaris 10 (SPARC/GTK+)</td>
<?php genLinks("eclipse-platform-${BUILD_ID}-solaris-gtk.zip"); ?>
</tr>
<tr>
<td>Solaris 10 (x86/GTK+)</td>
<?php genLinks("eclipse-platform-${BUILD_ID}-solaris-gtk-x86.zip"); ?>
</tr>
<tr>
<td>HPUX (ia64/GTK+)</td>
<?php genLinks("eclipse-platform-${BUILD_ID}-hpux-gtk-ia64.zip"); ?>
</tr>
<tr>
<td>AIX (PPC/GTK)</td>
<?php genLinks("eclipse-platform-${BUILD_ID}-aix-gtk-ppc.zip"); ?>
</tr>
<tr>
<td>AIX (PPC64/GTK)</td>
<?php genLinks("eclipse-platform-${BUILD_ID}-aix-gtk-ppc64.zip"); ?>
</tr>
<tr>
<td>Mac OSX (Mac/Cocoa/x86_64)</td>
<?php genLinks("eclipse-platform-${BUILD_ID}-macosx-cocoa-x86_64.tar.gz"); ?>
</tr>
<tr>
<td><img src = "repo.gif"  alt="Runtime Repo" /> Platform Runtime Repo </td>
<?php genLinks("org.eclipse.platform-${BUILD_ID}.zip"); ?>
</tr>

      </table>
 

      <h3 id="JDTRuntime"> JDT Runtime Binary&nbsp;<a href="details.html#JDTRuntime"><img
            src="images/more.gif" title="More..." alt="[More]" /></a>
      </h3>
      <?php startTable(); ?>
        <tr>

          <?php columnHeads(); ?>
        </tr>
        <tr>
<td><img src = "repo.gif" alt="JDT Repo" /> JDT Runtime Repo </td>
<?php genLinks("org.eclipse.jdt-${BUILD_ID}.zip"); ?>
</tr>

      </table>

 

      <h3 id="JDTSDK"> JDT SDK &nbsp;<a href="details.html#JDTSDK"><img
            src="images/more.gif" title="More..." alt="[More]" /></a>
      </h3>
      <?php startTable(); ?>
        <tr>

          <?php columnHeads(); ?>
        </tr>
        <tr>
<td><img src = "repo.gif" alt=" JDT Source Repo" /> JDT Source Repo </td>
<?php genLinks("org.eclipse.jdt.source-${BUILD_ID}.zip"); ?>
</tr>

      </table>
 

      <h3 id="JDTCORE"> JDT Core Batch Compiler &nbsp;<a href="details.html#JDTCORE"><img
            src="images/more.gif" title="More..." alt="[More]" /></a>
      </h3>
      <?php startTable(); ?>
        <tr>

          <?php columnHeads(); ?>
        </tr>
        <tr>
<td>All</td>
<?php genLinks("ecj-${BUILD_ID}.jar"); ?>
</tr>
<tr>
<td>All</td>
<?php genLinks("ecjsrc-${BUILD_ID}.jar"); ?>
</tr>

      </table>
 

      <h3 id="PDERuntime"> PDE Runtime Binary&nbsp;<a href="details.html#PDERuntime"><img
            src="images/more.gif" title="More..." alt="[More]" /></a>
      </h3>
      <?php startTable(); ?>
        <tr>

          <?php columnHeads(); ?>
        </tr>
        <tr>
<td><img src = "repo.gif" alt="PDE Repo" /> PDE Repo </td>
<?php genLinks("org.eclipse.pde-${BUILD_ID}.zip"); ?>
</tr>
<tr>
<td><img src = "repo.gif" alt="PDE API Tools" /> PDE API Tools execution environment fragments repo</td>
<?php genLinks("org.eclipse.pde.api.tools.ee.feature-${BUILD_ID}.zip"); ?>
</tr>

      </table>
 

      <h3 id="PDESDK"> PDE SDK&nbsp;<a href="details.html#PDESDK"><img
            src="images/more.gif" title="More..." alt="[More]" /></a>
      </h3>
      <?php startTable(); ?>
        <tr>

          <?php columnHeads(); ?>
          </tr>
          <tr>
<td><img src = "repo.gif" alt="PDE Source Repo" /> PDE Source Repo </td>
<?php genLinks("org.eclipse.pde.source-${BUILD_ID}.zip"); ?>
</tr>
      </table>

 

<h3 id="CVSRuntime"> CVS Client Runtime Binary&nbsp;<a href="details.html#CVSRuntime"><img
            src="images/more.gif" title="More..." alt="[More]" /></a>
      </h3>
      <?php startTable(); ?>
        <tr>

          <?php columnHeads(); ?>
        </tr>
        <tr>
<td><img src = "repo.gif" alt="CVS Repo" /> CVS Runtime Repo </td>
<?php genLinks("org.eclipse.cvs-${BUILD_ID}.zip"); ?>
</tr>

      </table>

 


<h3 id="CVSSDK"> CVS Client SDK&nbsp;<a href="details.html#CVSSDK"><img
            src="images/more.gif" title="More..." alt="[More]" /></a>
      </h3>
      <?php startTable(); ?>
        <tr>

          <?php columnHeads(); ?>
        </tr>
          <tr>
<td><img src = "repo.gif" alt="CVS Source Repo" /> CVS Source Repo </td>
<?php genLinks("org.eclipse.cvs.source-${BUILD_ID}.zip"); ?>
</tr>

      </table>

      <h3 id="SWT"> SWT Binary and Source&nbsp;<a href="details.html#SWT"><img
            src="images/more.gif" title="More..." alt="[More]" /></a>
      </h3>
      <?php startTable(); ?>
        <tr>
          <?php columnHeads(); ?>
        </tr>
        <tr>
<td>Windows</td>
<?php genLinks("swt-${BUILD_ID}-win32-win32-x86.zip"); ?>
</tr>
<tr>
<td>Windows (x86_64)</td>
<?php genLinks("swt-${BUILD_ID}-win32-win32-x86_64.zip"); ?>
</tr>
<tr>
<td>Linux (x86/GTK+)</td>
<?php genLinks("swt-${BUILD_ID}-gtk-linux-x86.zip"); ?>
</tr>
<tr>
<td>Linux (x86_64/GTK+)</td>
<?php genLinks("swt-${BUILD_ID}-gtk-linux-x86_64.zip"); ?>
</tr>
<tr>
<td>Linux (PPC/GTK+)</td>
<?php genLinks("swt-${BUILD_ID}-gtk-linux-ppc.zip"); ?>
</tr>
<tr>
<td>Linux (PPC64/GTK+)</td>
<?php genLinks("swt-${BUILD_ID}-gtk-linux-ppc64.zip"); ?>
</tr>
<tr>
<td>Linux (PPC64LE/GTK+)</td>
<?php genLinks("swt-${BUILD_ID}-gtk-linux-ppc64le.zip"); ?>
</tr>
<tr>
<td>Solaris 10 (SPARC/GTK+)</td>
<?php genLinks("swt-${BUILD_ID}-gtk-solaris-sparc.zip"); ?>
</tr>
<tr>
<td>Solaris 10 (x86/GTK+)</td>
<?php genLinks("swt-${BUILD_ID}-gtk-solaris-x86.zip"); ?>
</tr>
<tr>
<td>HPUX (ia64/GTK+)</td>
<?php genLinks("swt-${BUILD_ID}-gtk-hpux-ia64.zip"); ?>
</tr>
<tr>
<td>AIX (PPC/GTK+)</td>
<?php genLinks("swt-${BUILD_ID}-gtk-aix-ppc.zip"); ?>
</tr>
<tr>
<td>AIX (PPC64/GTK+)</td>
<?php genLinks("swt-${BUILD_ID}-gtk-aix-ppc64.zip"); ?>
</tr>
<tr>
   <td>Mac OSX (Mac/Cocoa/x86_64)</td>
   <?php genLinks("swt-${BUILD_ID}-cocoa-macosx-x86_64.zip"); ?>
</tr>
</table>

 

<h3 id="org.eclipse.releng"> org.eclipse.releng.tools plug-in&nbsp;<a href="details.html#org.eclipse.releng"><img
            src="images/more.gif" title="More..." alt="[More]" /></a>
</h3>
<?php startTable(); ?>
<tr>
  <?php columnHeads(); ?>
</tr>
<tr>
  <td><img src = "repo.gif" alt="Releng Tools Repo" /> Releng Tools Repo</td>
  <?php genLinks("org.eclipse.releng.tools-${BUILD_ID}.zip"); ?>
</tr>
</table>

<?php } ?>
</div> <!-- end dropsection -->
</div> <!-- close div classs=container -->
</main> <!-- close main role="main" element -->
</body>
</html>
<?php
  $html = ob_get_clean();

  #echo the computed content
  echo $html;
?>

