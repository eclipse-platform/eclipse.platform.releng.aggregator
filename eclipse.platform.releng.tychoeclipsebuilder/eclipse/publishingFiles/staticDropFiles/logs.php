<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<?php
$testresults="testresults";
include("buildproperties.php");
include ('testConfigs.php');
include ('utilityFunctions.php');
include ('logPhpUtils.php');
?>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
<meta name="author" content="Eclipse Foundation, Inc." />
<meta name="keywords" content="eclipse,project,plug-ins,plugins,java,ide,swt,refactoring,free java ide,tools,platform,open source,development environment,development,ide" />
<link rel="stylesheet" type="text/css" href="https://download.eclipse.org/eclipse/eclipse.org-common/stylesheets/visual.css" media="screen" />
<link rel="stylesheet" type="text/css" href="https://download.eclipse.org/eclipse/eclipse.org-common/stylesheets/layout.css" media="screen" />
<link rel="stylesheet" type="text/css" href="https://download.eclipse.org/eclipse/eclipse.org-common/stylesheets/print.css" media="print" />

<title>Test Logs for <?= $BUILD_ID ?></title>
<style>
  p {text-indent: 30pt;}
</style>

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
<h1>Unit Test Logs for <?= $BUILD_ID ?></h1>
<div class="homeitem3col">

<h2 id="console">Console Logs</h2>
<p>These logs contain the console output captured while running the JUnit automated tests.</p>

<?php
listLogs("$testresults/consolelogs");
foreach ($expectedTestConfigs as $expectedTestConfig) {
	listDetailedLogs($testresults, $expectedTestConfig);
}
?>

</div>
</div>
</body>
</html>
 
