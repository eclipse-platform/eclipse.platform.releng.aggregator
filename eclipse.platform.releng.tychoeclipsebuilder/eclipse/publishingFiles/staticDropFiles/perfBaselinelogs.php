<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>

<?php
$testresults="baseline";
include("buildproperties.php");
include ('testConfigs.php');
include ('utilityFunctions.php');

function checkPlatform($line) {

  if (preg_match("/win7|win32|linux|macosx/i", $line)) {
    return 1;
  } else {
    return 0;
  }

}


function checkFile($p) {

  if ((is_file($p)) && (preg_match("/.txt|.log|.png/i", $p)))  {
    return 1;
  } else {
    return 0;
  }

}


function listLogs($myDir) {
  $entries = array();
  $aDirectory = dir($myDir);
  if ($aDirectory === NULL || $aDirectory === FALSE)
  {
    return;
  }
  $index = 0;
  $cdir = getcwd();
  while ($anEntry = $aDirectory->read()) {
    $path = $cdir . "/" . $myDir . "/" . $anEntry;
    #            if ((is_file($path)) && (preg_match("/.txt/i", $path))) {
    $c = checkFile($path);
    if ($c == 1) {
      $entries[$index] = $anEntry;
      $index++;
    }
  }

  $aDirectory->close();
  if (count($entries) > 0) {
    sort($entries);
  }

  if ($index < 0) {
    echo "<br>There are no test logs for this build.";
    return;
  }
  for ($i = 0; $i < $index; $i++) {
    $anEntry = $entries[$i];
    $updateLine = 0;
    $updateLine = checkPlatform($anEntry);
    if (($updateLine == 0) && (preg_match("/\//",$myDir))) {
      $linktext = $myDir . "_" . $anEntry;
      # remove the directory name from the link to the log
      $dir = substr(strrchr($linktext, "/"), 1);
      $line = "<td><a href=\"$myDir/$anEntry\">$dir</a> " . fileSizeForDisplay("$myDir/$anEntry") . " </td>";
    } else {
      $line = "<td><a href=\"$myDir/$anEntry\">$anEntry</a> " . fileSizeForDisplay("$myDir/$anEntry") . " </td>";
    }
    echo "<li>$line</li>";
  }
}

function listDetailedLogs ($testresults, $machineplatform) {
  if (file_exists("$testresults/$machineplatform")) {
    echo "<strong>Individual $machineplatform test logs</strong><br />";
    listLogs("$testresults/$machineplatform");
  }
  if (file_exists("$testresults/$machineplatform/crashlogs")) {
    echo "<strong>Crash logs captured on $machineplatform</strong>";
    listLogs("$testresults/$machineplatform/crashlogs");
  }
  if (file_exists("$testresults/$machineplatform/timeoutScreens")) {
    echo "<strong>Screen captures for tests timing out on $machineplatform</strong>";
    listLogs("$testresults/$machineplatform/timeoutScreens");
  }
}


?>
<STYLE TYPE="text/css">
<!--
P {text-indent: 30pt;}
-->
</STYLE>


<title>Baseline Performance Test Logs</title>
<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1" />
<meta name="author" content="Eclipse Foundation, Inc." />
<meta name="keywords" content="eclipse,project,plug-ins,plugins,java,ide,swt,refactoring,free java ide,tools,platform,open source,development environment,development,ide" />
<link rel="stylesheet" type="text/css" href="../../../eclipse.org-common/stylesheets/visual.css" media="screen" />
<link rel="stylesheet" type="text/css" href="../../../eclipse.org-common/stylesheets/layout.css" media="screen" />
<link rel="stylesheet" type="text/css" href="../../../eclipse.org-common/stylesheets/print.css" media="print" />
<script type="text/javascript">

sfHover = function() {
  var sfEls = document.getElementById("leftnav").getElementsByTagName("LI");
  for (var i=0; i<sfEls.length; i++) {
    sfEls[i].onmouseover=function() {
      this.className+=" sfhover";
    }
    sfEls[i].onmouseout=function() {
      this.className=this.className.replace(new RegExp(" sfhover\\b"), "");
    }
  }
}
if (window.attachEvent) window.attachEvent("onload", sfHover);
</script>
</head>
<body>

<ul id="headernav">
<li class="first"><a href="/org/foundation/contact.php">Contact</a></li>
<li><a href="/legal/">Legal</a></li>
</ul>

<div id="leftcol">
<ul id="leftnav">
<li><a href="perflogs.php">Performance Logs</a></li>
<li><a href="perfBaselineResults.php#UnitTest">Baseline Performance Unit Test Results</a></li>

</ul>

</div>

<div id="midcolumn">
<div class="homeitem3col">
<?php


echo "<h2>Performance Unit Test Results for $BUILD_ID </h2>\n";

echo "<h3>Logs</h3>\n";

?>
</ul>
</li>




<li>
<ul>
<strong><a name="console" id="console"></a>Performance Test Console Logs</strong>
<p>These logs contain the console output captured while running the Performance JUnit automated tests.</p>
<?php

listLogs("$testresults/consolelogs");

listDetailedLogs($testresults,$expectedTestConfigs[0]);
listDetailedLogs($testresults,$expectedTestConfigs[1]);
listDetailedLogs($testresults,$expectedTestConfigs[2]);


?>
</ul>
</li>
</div>

</body>
</html>
