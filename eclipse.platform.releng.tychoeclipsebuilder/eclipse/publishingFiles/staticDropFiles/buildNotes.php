<html>
<head>
<?php

include("buildproperties.php");

echo "<title>Build Notes for $BUILD_ID </title>";
?>
<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1">
<link rel="stylesheet" href="../../../default_style.css" type="text/css">
</head>
<body>

<?php

# buildnotes.php is not included or linked to, unless the directory 'buildnotes' already exists
# at top of drop directory. See
# https://bugs.eclipse.org/bugs/show_bug.cgi?id=436219
echo "<h1>Build Notes for $BUILD_ID </h1>";
echo "<p>Build notes are used to notify the community of notable issues or changes in a particular build.</p>";
echo "<p>Committers, see <a href=\"https://wiki.eclipse.org/Platform-releng/Platform_Build_Automated#Build_Notes\">Build notes</a> on Eclipse releng wiki for instructions.</p>";
echo "<ul>";
$aDirectory = dir("buildnotes");
while ($anEntry = $aDirectory->read()) {
  if (($anEntry != "." && $anEntry != "..") && (! preg_match("/\.css/",$anEntry))) {
    $parts = explode("_", $anEntry);
    $baseName = $parts[1];
    $parts = explode(".", $baseName);
    $component = $parts[0];
    $line = "<li>Component: <a href=\"buildnotes/$anEntry\">$component</a> ";
    echo "$line";
    echo "</li>";
  }
}
echo "</ul>";
$aDirectory->close();


?>

</body>
</html>
