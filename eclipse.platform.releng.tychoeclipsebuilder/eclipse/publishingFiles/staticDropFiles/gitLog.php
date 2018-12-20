<?php

include_once("buildproperties.php");
include_once("utilityFunctions.php");

# Begin: page-specific settings.
$pageTitle    = "Git Log for $BUILD_ID";
$pageKeywords = "eclipse,project,plug-ins,plugins,java,ide,swt,refactoring,free java ide,tools,platform,open source,development environment,development,ide";
$pageAuthor   = "Sravan Kumar Lakkimsetti";

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
$endingBreadCrumbs="<li><a href=\"../$BUILD_DIR_SEG/\">$BUILD_ID</a></li><li class=\"active\">Git Log</li>";

require("DL.thin.header.php.html");

?>




<?php

echo "<h1>Git Log for <a href=\"../".$BUILD_DIR_SEG."\">".$BUILD_ID;
echo "</a></h1>".PHP_EOL;
echo "<div class=\"gitLogSection\">";
if (file_exists("gitLog.html")) {
  $log_file = file_get_contents("gitLog.html");
  echo "$log_file";
}
?>

</div>
</main>
    </body>
  </html>
<?php
  $html = ob_get_clean();

  #echo the computed content
  echo $html;
?>
