<html>
<head>
<?php
$parts = explode("/", getcwd());
$parts2 = explode("-", $parts[count($parts) - 1]);
$buildName = $parts2[0] . "-" . $parts2[1];

// Get build type names

$fileHandle = fopen("./dlconfig2.txt", "r");
while (!feof($fileHandle)) {

    $aLine = fgets($fileHandle, 4096); // Length parameter only optional after 4.2.0
    $parts = explode(",", $aLine);
    $dropNames[trim($parts[0])] = trim($parts[1]);
}
fclose($fileHandle);

$buildType = $dropNames[$parts2[0]];

echo "<title>Build Notes for $buildType $buildName </title>";
?>
<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1">
<link rel="stylesheet" href="../../../default_style.css" type="text/css">
</head>
<body>

<?php
if (file_exists("report.txt")) {

    echo "<h2>Change Reports</h2>";
    echo "<p><a href=\"report.txt\">Report of changes</a> from previous build.</p>";
}


if (file_exists("buildnotes")) {
    $hasNotes = false;
    $aDirectory = dir("buildnotes");
    while ($anEntry = $aDirectory->read()) {
        if (($anEntry != "." && $anEntry != "..") && (! preg_match("/\.css/",$anEntry))) {
            // found something, so we do "have notes"
            if (! $hasNotes) {
                echo "<h2>Build Notes</h2>\n";
                echo "<ul>";
                $hasNotes=true;
            }
            $parts = explode("_", $anEntry);
            $baseName = $parts[1];
            $parts = explode(".", $baseName);
            $component = $parts[0];
            $line = "<li>Component: <a href=\"buildnotes/$anEntry\">$component</a> ";
            echo "$line";
            echo "</li>";
        }
    }
    if ($hasNotes) {
       echo "</ul>\n";
    }
    echo "<p>Build notes (if any) are used to notify the community of notable issues or changes in a particular build.</p>";
    echo "<p>Committers, to include build notes for your component, add a file with the pattern buildnotes_&lt;component-name&gt;.html to the root of one of your bundle's source tree.</p>";

    aDirectory.closedir();
}

?>

</body>
</html>
