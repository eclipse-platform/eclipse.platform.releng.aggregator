<?php require_once($_SERVER['DOCUMENT_ROOT'] . "/eclipse.org-common/system/app.class.php");	require_once($_SERVER['DOCUMENT_ROOT'] . "/eclipse.org-common/system/nav.class.php"); 	require_once($_SERVER['DOCUMENT_ROOT'] . "/eclipse.org-common/system/menu.class.php"); 	$App 	= new App();	$Nav	= new Nav();	$Menu 	= new Menu();		include($App->getProjectCommon());    # All on the same line to unclutter the user's desktop'
# Begin: page-specific settings.  Change these. 
$pageTitle = "Equinox Downloads";
$pageKeywords = "equinox, osgi, framework, runtime, download";
$pageAuthor = "Equinox committers";

$contents = substr(file_get_contents('dlconfig.txt'),0,-1);
$contents = str_replace("\n", "", $contents);

#split the content file by & and fill the arrays
$elements = explode("&",$contents);
$t = 0; 
$p = 0;
for ($c = 0; $c < count($elements)-1; $c++) {
    $tString = "dropType";
    $pString = "dropPrefix";
    if (strstr($elements[$c],$tString)) {
        $temp = preg_split("/=/",$elements[$c]);
        $dropType[$t] = $temp[1];
        $t++;
    }
    if (strstr($elements[$c],$pString)) {
        $temp = preg_split("/=/",$elements[$c]);
        $dropPrefix[$p] = $temp[1];
        $p++;
    }
}

for ($i = 0; $i < count($dropType); $i++) {
    $typeToPrefix[$dropType[$i]] = $dropPrefix[$i];
}

$aDirectory = dir("drops");
while ($anEntry = $aDirectory->read()) {

    // Short cut because we know aDirectory only contains other directories.

    if ($anEntry != "." && $anEntry!=".." && $anEntry!="TIME") {
        $parts = explode("-", $anEntry);
        if (count($parts) == 3) {

            //$buckets[$parts[0]][] = $anEntry;

            $timePart = $parts[2];
            $year = substr($timePart, 0, 4);
            $month = substr($timePart, 4, 2);
            $day = substr($timePart, 6, 2);
            $hour = substr($timePart,8,2);
            $minute = substr($timePart,10,2);
            // special logic adds 1 second if build id contains "RC" ... this was 
            // added for the M build case, where there is an M build and and RC version that 
            // have same time stamp. One second should not effect desplayed values.
            $isRC = strpos($anEntry, "RC");
            if ($isRC === false) {
               $timeStamp = mktime($hour, $minute, 0, $month, $day, $year);
            } else { 
               $timeStamp = mktime($hour, $minute, 1, $month, $day, $year);
            }
            $buckets[$parts[0]][$timeStamp] = $anEntry; 
            $timeStamps[$anEntry] = date("D, j M Y -- H:i (O)", $timeStamp);
            if ($timeStamp > $latestTimeStamp[$parts[0]]) {
                $latestTimeStamp[$parts[0]] = $timeStamp;
                $latestFile[$parts[0]] = $anEntry;
            }
        }

        if (count($parts) == 2) {
            $buildType=substr($parts[0],0,1);
            //$buckets[$buildType][] = $anEntry;
            $datePart = substr($parts[0],1);
            $timePart = $parts[1];
            $year = substr($datePart, 0, 4);
            $month = substr($datePart, 4, 2);
            $day = substr($datePart, 6, 2);
            $hour = substr($timePart,0,2);
            $minute = substr($timePart,2,2);
            $isRC = strpos($anEntry, "RC");
            if ($isRC === false) {
                $timeStamp = mktime($hour, $minute, 0, $month, $day, $year);
            } else { 
                $timeStamp = mktime($hour, $minute, 1, $month, $day, $year);
            }
            $buckets[$buildType[0]][$timeStamp] = $anEntry;   

            $timeStamps[$anEntry] = date("D, j M Y -- H:i (O)", $timeStamp);
            if ($timeStamp > $latestTimeStamp[$buildType]) {
                $latestTimeStamp[$buildType] = $timeStamp;
                $latestFile[$buildType] = $anEntry;
            }
        }
    }
}

$html = <<<EOHTML
<div id="midcolumn">
        <h3>$pageTitle</h3>
        <p>For access to archived builds, look <a href="http://archive.eclipse.org/equinox/">here</a>.</p>

        <div class="homeitem3col">
                <h3>Latest Builds</h3>
                <table  width="100%" CELLSPACING=0 CELLPADDING=3> 

EOHTML;

foreach($dropType as $value) {
    $prefix=$typeToPrefix[$value];
    $fileName = $latestFile[$prefix];
    $parts = explode("-", $fileName);

    // Uncomment the line below if we need click through licenses.
    // echo "<td><a href=license.php?license=drops/$fileName>$parts[1]</a></td>";

    // Comment the line below if we need click through licenses.
    if (count($parts)==3)
        $html .= <<<EOHTML
                        <tr>
                                <td width="30%"><a href="drops/$fileName/index.php">$parts[1]</a></td>

EOHTML;
    if (count($parts)==2) 
        $html .= <<<EOHTML
                        <tr>
                                <td width="30%"><a href="drops/$fileName/index.php">$fileName</a></td>

EOHTML;

    $html .= <<<EOHTML
                                <td>$value</td>
                                <td>$timeStamps[$fileName]</td>
                        </tr>

EOHTML;
}

$html .= <<<EOHTML
                </table>

EOHTML;

foreach($dropType as $value) {
    $prefix=$typeToPrefix[$value];

    $html .= <<<EOHTML

                <h3>$value Builds</h3>
                <table  width="100%" CELLSPACING=0 CELLPADDING=3>

EOHTML;

    $aBucket = $buckets[$prefix];
    if (isset($aBucket)) {
        rsort($aBucket);
        foreach($aBucket as $innerValue) {
            $parts = explode("-", $innerValue);
            $html .= <<<EOHTML
                        <tr>

EOHTML;
            // Uncomment the line below if we need click through licenses.
            // echo "<td><a href=\"license.php?license=drops/$innerValue\">$parts[1]</a></td>";

            // Comment the line below if we need click through licenses.
            if (count ($parts)==3)
                $html .= <<<EOHTML
                                <td width="30%"><a href="drops/$innerValue/index.php">$parts[1]</a></td>

EOHTML;
            if (count ($parts)==2)
                $html .= <<<EOHTML
                                <td width="30%"><a href="drops/$innerValue/index.php">$innerValue</a></td>

EOHTML;

            $html .= <<<EOHTML
                                <td>$timeStamps[$innerValue]</td>
                        </tr>

EOHTML;
        }
    }
    $html .= <<<EOHTML
                </table>

EOHTML;
}
$html .= <<<EOHTML
        </div>
</div>

EOHTML;

generateRapPage( $App, $Menu, $Nav, $pageAuthor, $pageKeywords, $pageTitle, $html );
?>
