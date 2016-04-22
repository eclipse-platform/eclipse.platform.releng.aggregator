<?
include_once("buildproperties.php");
include_once("utilityFunctions.php");

# Begin: page-specific settings.
$pageTitle    = "Build Notes for $BUILD_ID";
$pageKeywords = "eclipse,project,plug-ins,plugins,java,ide,swt,refactoring,free java ide,tools,platform,open source,development environment,development,ide";
$pageAuthor   = "David Williams";

ini_set("display_errors", "true");
error_reporting (E_ALL);

ob_start();
$endingBreadCrumbs="<li><a href=\"../$BUILD_ID/\">$BUILD_ID</a></li><li class=\"active\">Build Notes</li>";

require("DL.thin.header.php.html");
?>
<div class="row">
 <div class="col-md-12 col-md-offset-2">

<?php

# buildnotes.php is not included or linked to, unless the directory 'buildnotes' already exists
# at top of drop directory. See
# https://bugs.eclipse.org/bugs/show_bug.cgi?id=436219
echo "<h1>Build Notes for $BUILD_ID </h1>";
echo "<p>Build notes are used to notify the community of notable issues or changes in a particular build.</p>";
echo "<p>(Committers, see <a href=\"https://wiki.eclipse.org/Platform-releng/Platform_Build_Automated#Build_Notes\">Build notes</a> on Eclipse releng wiki for instructions.)</p>";
echo "<p>Component: </p>";
echo "<ul>";
$aDirectory = dir("buildnotes");
while ($anEntry = $aDirectory->read()) {
  if (($anEntry != "." && $anEntry != "..") && (! preg_match("/\.css/",$anEntry))) {
    $parts = explode("_", $anEntry);
    $baseName = $parts[1];
    $parts = explode(".", $baseName);
    $component = $parts[0];
    $pair1="anEntry=".urlencode($anEntry);
    $pair2="component=".urlencode($component);
    // echo "<br />DEBUG: ".$pair1;
    // echo "<br />DEBUG: ".$pair2;
    $argline=$pair1."&".$pair2;
    // echo "<br />DEBUG: " . $argline;
    $argline=htmlspecialchars($argline);
    // echo "<br />DEBUG: " . $argline;
    echo "<li>"; 
    echo "<a href='buildNote.php?".$argline."'>";
    echo "$component";
    echo "</a>";
    echo "</li>";
  }
}
echo "</ul>";
$aDirectory->close();


?>
</div>
</div>
</main>
</body>
</html>
