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
<h3 name="Performancefingerprint">Performance fingerprint</h3>

<?php

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
?>
  <p>
    <a name="unit"></a>Legend: <br/>*: Missing reference data. Build used for
    comparison specified in ending parenthesis.<br>green: faster,
    less memory <br>red: slower, more memory <br>grey:
    regression with explanation. Click the bar for notes on this
    scenario. <br>x axis: difference between current value and
    baseline value as percentage<br>
  </p>

<h3 name="ScenarioDetail">Detailed performance data grouped by scenario prefix</h3>

  <?php

    if (count($componentFps)==0){
        echo "Results pending.";
    }
    else {
        $type=$_SERVER['QUERY_STRING'];
        if ($type=="") {
            $type="fp_type=0";
        }
        sort($componentFps);

        for ($counter=0;$counter<count($componentFps);$counter++){
            $parts=split(".php",$componentFps[$counter]);
            $prefix=$parts[0];
            $href="<A HREF=\"$performanceDir/$componentFps[$counter]?";
            $href=$href . $type . "\">$prefix*</A><br>";
            echo $href;
        }
    }
?>

<h3 name="UnitTest">Performance Unit Test Results for <?php echo "$BUILD_ID"; ?> </h3>

<p>The table shows the unit test results for this build on the platforms
tested. You may access the test results page specific to each
component on a specific platform by clicking the cell link.
Normally, the number of errors is indicated in the cell.
A "-1" or "DNF" means the test "Did Not Finish" for unknown reasons
and hence no results page is available. In that case,
more information can sometimes be found in
the <a href="../perflogs.php#console">console logs</a>.</p>
<?php
if (file_exists("$performanceDir/testNotes.html")) {
    $my_file = file_get_contents("$performanceDir/testNotes.html");
    echo $my_file;
}
if (file_exists("../baseline.php")) {
   echo "<p>See also the <a href=\"../baseline.php\">baseline unit tests results</a>.</p>";
}
?>


<?php
$rowResultsFile="performanceTables.html";
  if (file_exists($rowResultsFile)) {
    include $rowResultsFile;
} else {
    include "testResultsTablesPending.html";
}
?>
</table>
</div>

</body>
</html>
