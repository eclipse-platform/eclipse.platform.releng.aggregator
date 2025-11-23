<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>

<?php
include ('buildproperties.php');
include ('utilityFunctions.php');

function listLogs($myDir, $filterNames) {

  $aDirectory = dir($myDir);
  $index = 0;
  $cdir = getcwd();
  while ($anEntry = $aDirectory->read()) {
    $path = $cdir . "/" . $myDir . "/" . $anEntry;
    if (is_file($path)) {
      $entries[$index] = $anEntry;
      $index++;
    }
  }

  $aDirectory->close();
  if (!empty($entries)) {
    sort($entries);
  }

  if ($index < 0) {
    echo "<br>There are no logs for this build.";
    return;
  }
  echo "<ul>";
  for ($i = 0; $i < $index; $i++) {
    $anEntry = $entries[$i];
    $label = $anEntry;
    if ($filterNames) {
      if (strpos($anEntry, 's') !== 0) {
        continue;
      }
      $label = preg_replace('/^s\d+|\.log$/', '', $anEntry);
      $label = str_replace('_', ' ', $label);
    }
    $line = "<a href=\"$myDir/$anEntry\">$label</a> " . fileSizeForDisplay("$myDir/$anEntry");
    echo "<li>$line</li>";
  }
  echo "</ul>";
}


?>
<STYLE>
P {text-indent: 30pt;}
</STYLE>


<title>Drop Test Results</title>
<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1" />
<meta name="author" content="Eclipse Foundation, Inc." />
<meta name="keywords" content="eclipse,project,plug-ins,plugins,java,ide,swt,refactoring,free java ide,tools,platform,open source,development environment,development,ide" />
<link rel="stylesheet" type="text/css" href="https://download.eclipse.org/eclipse/eclipse.org-common/stylesheets/visual.css" media="screen" />
<link rel="stylesheet" type="text/css" href="https://download.eclipse.org/eclipse/eclipse.org-common/stylesheets/layout.css" media="screen" />
<link rel="stylesheet" type="text/css" href="https://download.eclipse.org/eclipse/eclipse.org-common/stylesheets/print.css" media="print" />
<script>

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

<div id="leftcol">
<ul id="leftnav">
<li><a href="logs.php">Logs</a></li>
<li><a href="testResults.php#UnitTest">Unit Test Results</a></li>
<li><a href="testResults.php#PluginsErrors">Plugins Containing Compile Errors</a></li>

</ul>

</div>



<div id="midcolumn">
<h1>Release Engineering logs for <?= $BUILD_ID ?></h1>
<div class="homeitem3col">
<h3>Build Properties for <?= $BUILD_ID ?></h3>
<p><a href="mavenproperties.properties">Key Maven Properties</a></p>
<p><a href="buildproperties.properties">Other Build Properties</a></p>
<h3>Release Engineering Logs for <?= $BUILD_ID ?></h3>

<?php
listLogs("buildlogs", true);
?>

<h3>Comparator Logs for <?= $BUILD_ID ?></h3>
<p>For explaination, see <a href="https://wiki.eclipse.org/Platform-releng/Platform_Build_Comparator_Logs">Platform Build Comparator Logs</a> wiki.</p>
<?php
listLogs("buildlogs/comparatorlogs", false);
if (file_exists("buildlogs/comparatorlogs/artifactcomparisons.zip")) {
?>
  <p>For an archive of all relevant baseline-versus-current build artifact byte codes download and 'diff' matching files of 
  <a href="buildlogs/comparatorlogs/artifactcomparisons.zip">artifact comparisons</a>.</p>
<?php
}
?>

</div>
</div>

</body>
</html>

