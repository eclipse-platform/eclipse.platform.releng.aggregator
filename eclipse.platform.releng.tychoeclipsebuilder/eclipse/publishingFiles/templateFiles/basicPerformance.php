<?php
//include("../utilityFunctions.php");
//include("../buildproperties.php");
//include ("../perfTestConfigs.php");

# Get variables
if (isset($_GET["name"])){
  $COMPONENT_ID = $_GET["name"];
}
if (isset($_GET["build"])){
  $BUILD_ID = $_GET["build"];
}
if (isset($_GET["baseline"])){
  $BASELINE_ID = $_GET["baseline"];
}

# Page Settings
$pageTitle  = "Performance of $COMPONENT_ID $BUILD_ID";
$pageKeywords = "eclipse,project,plug-ins,plugins,java,ide,swt,refactoring,free java ide,tools,platform,open source,development environment,development,ide";
$pageAuthor   = "Samantha Dawley";

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
$endingBreadCrumbs="<li><a href=\"../$BUILD_DIR_SEG/\">$BUILD_ID</a></li><li><a href=\"./performance.php\">TEST RESULTS</a></li><li class=\"active\">$COMPONENT_ID RESULTS</li>";

require("../DL.thin.header.php.html");

echo "<h2>Performance of $COMPONENT_ID: $BUILD_ID relative to $BASELINE_ID</h2>";

if (file_exists($COMPONENT_ID.'_BasicTable.html')) {
  $my_file = file_get_contents($COMPONENT_ID.'_BasicTable.html');
  echo $my_file;
} 
else {
  echo "Results Pending...";
}

?>