<?php
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
    echo "<p>There are no test logs for this build.</p>\n";
    return;
  }
  echo "<ul>\n";
  for ($i = 0; $i < $index; $i++) {
    $anEntry = $entries[$i];
    $updateLine = 0;
    $updateLine = checkPlatform($anEntry);
    if (($updateLine == 0) && (preg_match("/\//",$myDir))) {
      $linktext = $myDir . "_" . $anEntry;
      # remove the directory name from the link to the log
      $dir = substr(strrchr($linktext, "/"), 1);
      $line = "<a href=\""."$myDir/$anEntry"."\">".$dir."</a> " . fileSizeForDisplay("$myDir/$anEntry");

    } else {
      $line = "<a href=\""."$myDir/$anEntry"."\">".$anEntry."</a> " . fileSizeForDisplay("$myDir/$anEntry");
    }
    echo "<li>$line</li>\n";
  }
  echo "</ul>\n";
}

function listDetailedLogs ($testresults, $machineplatform) {
  if (file_exists("$testresults/$machineplatform")) {
    echo "<h4>Individual $machineplatform test logs</h4>\n";
    listLogs("$testresults/$machineplatform");
  }
  if (file_exists("$testresults/$machineplatform/crashlogs")) {
    echo "<h4>Crash logs captured on $machineplatform</h4>\n";
    listLogs("$testresults/$machineplatform/crashlogs");
  }
  if (file_exists("$testresults/$machineplatform/timeoutScreens")) {
    echo "<h4>Screen captures for tests timing out on $machineplatform</h4>\n";
    listLogs("$testresults/$machineplatform/timeoutScreens");
  }
    if (file_exists("$testresults/$machineplatform/directorLogs")) {
    echo "<h4>p2 director logs while installing tests on $machineplatform</h4>\n";
    listLogs("$testresults/$machineplatform/directorLogs");
  }
}
