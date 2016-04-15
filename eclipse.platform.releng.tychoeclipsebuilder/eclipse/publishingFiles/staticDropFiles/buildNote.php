<?php
include_once("buildproperties.php");
include_once("utilityFunctions.php");

// var_dump($_GET);
$anEntry=$_GET['anEntry'];
$component=$_GET['component'];

//echo "DEBUG: anEntry: ".$anEntry ;
//echo "<br />";
//echo "DEBUG: component:".$component ;

# Begin: page-specific settings.
$pageTitle    = "Build Notes for " . $BUILD_ID . " for " . $component ;
$pageKeywords = "eclipse,project,plug-ins,plugins,java,ide,swt,refactoring,free java ide,tools,platform,open source,development environment,development,ide";
$pageAuthor   = "David Williams";

//ini_set("display_errors", "true");
//error_reporting (E_ALL);

ob_start();
$endingBreadCrumbs="<li><a href=\"../". $BUILD_ID. "/\">" . $BUILD_ID . "</a></li><li><a href=\"buildNotes.php\">Build Notes</a></li><li class=\"active\">$component</li>";

require("DL.thin.header.php.html");
?>
<div class="row">
 <div class="col-md-12 col-md-offset-2">

<?php

# buildnotes.php is not included or linked to, unless the directory 'buildnotes' already exists
# at top of drop directory. See
# https://bugs.eclipse.org/bugs/show_bug.cgi?id=436219
echo "<h1>Build Notes for " . $BUILD_ID . " for " . $component . "</h1>";
//echo "<h2>Component: " . $component . "</h2>";
$notesfile="buildnotes/".$anEntry;
//echo "DEBUG: notesfile: $notesfile";

if (file_exists($notesfile)) {
      $my_file = file_get_contents($notesfile);
      echo $my_file;
} else {
  echo "file note found: $notesfile";
}

?>
</div>
</div>
</main>
</body>
</html>


