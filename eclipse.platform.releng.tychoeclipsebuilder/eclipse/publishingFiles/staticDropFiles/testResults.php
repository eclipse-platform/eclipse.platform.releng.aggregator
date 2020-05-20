<?php

include_once("buildproperties.php");
include_once("utilityFunctions.php");

# Begin: page-specific settings.
$pageTitle    = "Test Results for $BUILD_ID";
$pageKeywords = "eclipse,project,plug-ins,plugins,java,ide,swt,refactoring,free java ide,tools,platform,open source,development environment,development,ide";
$pageAuthor   = "David Williams and Christopher Guindon";

//ini_set("display_errors", "true");
//error_reporting (E_ALL);


ob_start();

/*
DL.thin.header.php.html was original obtained from

wget https://eclipse.org/eclipse.org-common/themes/solstice/html_template/thin/header.php

and then that file modified to suit our needs.
Occasionally, our version should be compared to the "standard" to see if anything has
changed, in the interest of staying consistent.

See https://eclipse.org/eclipse.org-common/themes/solstice/docs/

 */
$endingBreadCrumbs="<li><a href=\"../$BUILD_DIR_SEG/\">$BUILD_ID</a></li><li class=\"active\">Test Results</li>";

require("DL.thin.header.php.html");

?>



  <?php if (! isset ($BUILD_FAILED) ) { ?>

<aside class="col-md-6" id="leftcol" style="margin-top:20px;" >
<ul class="ul-left-nav fa-ul hidden-print" style="text-color:black; background-color:#EFEBFF; background-size:contain; background-clip:border-box; border-color: black; font-size:12px; font-weight:bold; padding:2px; line-height:1; border-radius: 1;  margin:20px 3px 20px 3px">
      <li><a href="#Logs">Logs</a></li>
      <li><a href="#UnitTest">Unit Test Results</a></li>
      <li><a href="#PluginsErrors">Plugins Containing Compile Errors</a></li>

</ul>
</aside>
  <!-- end 'not build failed' -->

<?php }

echo "<div id=\"midcolumn\">".PHP_EOL;

echo "<h1>Test Results for <a href=\"../".$BUILD_DIR_SEG."\">".$BUILD_ID;
if (file_exists("buildUnstable")) {
        echo "&nbsp<a href=\"https://wiki.eclipse.org/Platform-releng/Unstable_build\" title=\"Unstable Build\" style='color:red;'>Unstable!</a>\n";
}
echo "</a></h1>".PHP_EOL;
if (file_exists("buildUnstable")) {
  $bu_file = file_get_contents("buildUnstable");
  echo "$bu_file";
}
echo "<h3 id=\"Logs\"> Logs for <a href=\"../".$BUILD_DIR_SEG."\">".$BUILD_ID."</a></h3>".PHP_EOL;
echo "<ul>";


if (file_exists("buildlogs/reporeports/index.html")) {

?>
        <li>
        <a href="buildlogs/reporeports/index.html"><b> Repository Reports </b></a>
        </li>
        <?php } ?>

        <li>
        <a href="logs.php#javadoc"><b> Javadoc Logs </b></a>
        </li>
        <li> <a href="logs.php#console"><b> Console Output Logs </b></a>
        </li>
        <li> <a href="buildlogs.php"><b>Release engineering build logs</b></a>
        </li>
<?php
  echo " <li><a href=\"apitools/analysis/html/index.html\"><b>API Tools Version Verification Report</b></a>";
  echo "  This tool verifies the versions of the plugins against Eclipse ${API_PREV_REF_LABEL}.&nbsp;&nbsp;
  Exclusions are listed in <a href=\"https://git.eclipse.org/c/platform/eclipse.platform.releng.aggregator.git/tree/eclipse.platform.releng.tychoeclipsebuilder/eclipse/apiexclude/exclude_list_external.txt?h=$BRANCH\">.../apiexclude/exclude_list_external.txt</a>.</li> ";
?>

<?php
  $deprecationFilename="apitools/deprecation/apideprecation.html";
  if (file_exists($deprecationFilename)) {
    echo " <li><a href=\"$deprecationFilename\"><b>API Tools Deprecation Report</b></a>";
    echo "  This tool generates a report for API deprecated since ${API_PREV_REF_LABEL}.</li> ";
  }
  else {
    echo "  <li>No deprecation report. Nothing deprecated since ${API_PREV_REF_LABEL}.</li>";
  }
?>

<?php
  // have removed coverage measurements for now
  // echo " <li><a href=\"coverage.php\"><b>JaCoCo code coverage report</b></a></li>";
?>

<?php
  $freezeFilename="apitools/freeze_report.html";
  if (file_exists($freezeFilename)) {
    echo "<li><a href=\"$freezeFilename\"><b>API Tools Post-API Freeze Report</b></a>&nbsp;&nbsp;";
    echo "This report describes API changes since ${API_FREEZE_REF_LABEL}.  Exclusions are listed in <a href=\"https://git.eclipse.org/c/platform/eclipse.platform.releng.aggregator.git/tree/eclipse.platform.releng.tychoeclipsebuilder/eclipse/apiexclude/exclude_list.txt?h=$BRANCH\">.../apiexclude/exclude_list.txt</a>.</li>";
  }
  else {
    echo "  <li>No freeze report. Only generated in main stream after RC1.</li>";
  }
?>

<?php
  echo " <li><a href=\"apitools/apifilters-$BUILD_ID.zip\"><b>Zip of .api_filters files used in the build</b></a></li>";
?>
<?php

  echo"<li>eclipse.platform.releng.aggregator: $BRANCH (branch or hash: $EBUILDER_HASH)</li> ";

?>
<?php
  echo "<li>\n";
  $generated=file_exists("performance/global_fp.php");
  if (file_exists("performance/performance.php") && $generated) {
    echo "View the <a href=\"performance/performance.php\">performance test results</a> for the current build.\n";
  } else {
    echo "Performance tests are pending.\n";
  }
  echo "</li>\n";
  echo "</ul>\n";
?>
</div> <!-- end mid column (logs) section -->
<div class="resultsSection">
<?php

// all the following tables are styled based on being in the "resultsSection".
// See resultsSection.css.

  // testResultsTables.html is generated by a custom ant task in
  // build tools (see TestResultsGenerator.java). It consist of
  // one to three tables: test results, missing files, files missing from
  // testManifest.xml. The later two are are rarely produced, since usually nothing
  // is missing.
  $rowResultsFile="testResultsTables.html";
  if (file_exists($rowResultsFile)) {
    include $rowResultsFile;
} else {
    include "testResultsTablesPending.html";
}

// compilerSummary.html is generated by the same custom ant task.
// (Though, if it already exists, from previous run, it is not re-generated, normally.
// It consists of two tables, 1. errors and warnings from compiler. 2) access errors and warnings.
if (file_exists("compilerSummary.html")) {
    include "compilerSummary.html";
} else {
    include "compilerSummaryPending.html";
}
?>

</div> <!-- close resultSection -->
</main>
    </body>
  </html>
<?php
  $html = ob_get_clean();

  #echo the computed content
  echo $html;
?>
