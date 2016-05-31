<?php

include_once("buildproperties.php");
include_once("utilityFunctions.php");
include ("perfTestConfigs.php");
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
<li><a href="#UnitTest">Performance Unit Test Results</a></li>

</ul>
</aside>
  <!-- end 'not build failed' -->

<?php }

echo "<div id=\"midcolumn\">".PHP_EOL;

echo "<h1>Test Results for <a href=\"../".$BUILD_DIR_SEG."\">".$BUILD_ID."</a></h1>".PHP_EOL;

echo "<h3 id=\"Logs\"> Logs for <a href=\"../".$BUILD_DIR_SEG."\">".$BUILD_ID."</a></h3>".PHP_EOL;
echo "<ul>";


<li> <a href="perfBaselinelogs.php#console"><b> Console Output Logs </b></a>
</li>

</div>

<h3><a name="UnitTest">Performance Unit Test Results for <?php echo "$BUILD_ID"; ?> </a></h3>

<p>The table shows the unit test results for this build on the platforms
tested. You may access the test results page specific to each
component on a specific platform by clicking the cell link.
Normally, the number of errors is indicated in the cell.
A "-1" or "DNF" means the test "Did Not Finish" for unknown reasosns
and hence no results page is available. In that case,
more information can sometimes be found in
the <a href="perfBaselinelogs.php#console">console logs</a>.</p>
<?php
if (file_exists("testNotes.html")) {
    $my_file = file_get_contents("testNotes.html");
    echo $my_file;
}
?>


<?php
$rowResultsFile="perfBaselineResultsTables.html";
  if (file_exists($rowResultsFile)) {
    include $rowResultsFile;
} else {
    include "testResultsTablesPending.html";
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