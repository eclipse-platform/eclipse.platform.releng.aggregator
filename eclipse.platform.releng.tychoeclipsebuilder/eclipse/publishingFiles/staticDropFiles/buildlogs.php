<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>

<?php
include ('buildproperties.php');
include ('utilityFunctions.php');

function listLogs($myDir) {

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
  for ($i = 0; $i < $index; $i++) {
    $anEntry = $entries[$i];
    $line = "<td><a href=\"$myDir/$anEntry\">$anEntry</a>" . fileSizeForDisplay("$myDir/$anEntry") . "</td>";
    echo "<li>$line</li>";
  }
}


?>
<STYLE TYPE="text/css">
<!--
P {text-indent: 30pt;}
-->
</STYLE>


<title>Drop Test Results</title>
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

</div>


</div>

<div id="leftcol">
<ul id="leftnav">
<li><a href="logs.php">Logs</a></li>
<li><a href="testResults.php#UnitTest">Unit Test Results</a></li>
<li><a href="testResults.php#PluginsErrors">Plugins Containing Compile Errors</a></li>

</ul>

</div>



<div id="midcolumn">
<div class="homeitem3col">
<?php
echo "<title>Release Engineering logs for $BUILD_ID</title>\n";
echo "<h3>Build Properties for $BUILD_ID</h3>\n";
echo "<p><a href=\"mavenproperties.properties\">Key Maven Properties</a></p>\n";
echo "<p><a href=\"buildproperties.properties\">Other Build Properties</a></p>\n";
echo "<h3>Release Engineering Logs for $BUILD_ID</h3>\n";
?>

<?php
listLogs("buildlogs");
?>

<?php
echo "<h3>Comparator Logs for $BUILD_ID</h3>\n";
echo "<p>For explaination, see <a href=\"http://wiki.eclipse.org/Platform-releng/Platform_Build_Comparator_Logs\">Platform Build Comparator Logs</a> wiki.</p>\n";
listLogs("buildlogs/comparatorlogs");
?>

</li>
</ul>
</div>
</div>

</body>
</html>

