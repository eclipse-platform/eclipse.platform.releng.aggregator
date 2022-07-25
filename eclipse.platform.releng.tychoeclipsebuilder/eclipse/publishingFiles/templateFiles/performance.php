<?php


include("../utilityFunctions.php");
include("../buildproperties.php");
include ("../perfTestConfigs.php");

# Begin: page-specific settings.
$pageTitle    = "Performance Test Results for $BUILD_ID";
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

require("../DL.thin.header.php.html");

?>

<!-- Commenting out Fingerprint until I get it working - sdawley -->
<!-- <h3 name="Performancefingerprint">Performance fingerprint</h3>
<?php
/*

    $performanceDir=".";
    $performance = dir($performanceDir);
    $index=0;
    $fpcount=0;

    $fp_file="$performanceDir/global_fp.php";
    if (file_exists($fp_file)) {
        include($fp_file);
    }
    while ($file = $performance->read()) {
        if (strstr($file,".php")){
            $parts=split(".php",$file);
            $component=$parts[0];
            $start=substr($component, 0, 11);
            if ($start == "org.eclipse") {
                $componentFps[$fpcount]=$file;
                $fpcount++;
            }
        }
    }
    */
?>
  <p>
    <a name="unit"></a>Legend: <br/>*: Missing reference data. Build used for
    comparison specified in ending parenthesis.<br>green: faster,
    less memory <br>red: slower, more memory <br>grey:
    regression with explanation. Click the bar for notes on this
    scenario. <br>x axis: difference between current value and
    baseline value as percentage<br>
  </p>
-->

<h3 name="ScenarioDetail">Detailed performance data grouped by scenario prefix</h3>

  <?php

    if (file_exists("../BasicResultsIndex.html")) {
        $my_file = file_get_contents("../BasicResultsIndex.html");
        echo $my_file;
    }
    else {
        echo "Results pending.";
    }
?>

<?php
if (file_exists("../perftestNotes.html")) {
    $my_file = file_get_contents("../perftestNotes.html");
    echo $my_file;
}
?>


<?php
$rowResultsFile="../performanceResultsTable.html";
  if (file_exists($rowResultsFile)) {
    include $rowResultsFile;
} else {
    include "../testResultsTablesPending.html";
}
$LRrowResultsFile="../performanceLRResultsTables.html";
  if (file_exists($LRrowResultsFile)) {
    include $LRrowResultsFile;
}
?>
</table>
</div>

</body>
</html>
