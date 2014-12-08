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
<link rel="stylesheet" type="text/css" href="../../../eclipse.org-common/stylesheets/visual.css" media="screen" />
<link rel="stylesheet" type="text/css" href="../../../eclipse.org-common/stylesheets/layout.css" media="screen" />
<link rel="stylesheet" type="text/css" href="../../../eclipse.org-common/stylesheets/print.css" media="print" />

<title>Test Logs for <?= $BUILD_ID ?></title>
<style type="text/css">
  p {text-indent: 30pt;}
</style>

<script type="text/javascript">
<![CDATA[
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
]]>
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
<div class="homeitem3col">
<h1>Unit Test Logs for <?= $BUILD_ID ?></h1>


<!-- 
     javaDoc logs are "at the top" of the compile logs directory, having been 
     copied there by "helper.xml". Seems they could easily go into their own directory, 
     and if so, then there is a releng test that would have to change too, either simply 
     changing their location in the test.xml, or, changing to whole test to it would know
     where to find their special directory, and then loop through the whole directory. 
-->
<h2><a name="javadoc" id="javadoc"></a>Javadoc Logs</h2>
<ul>
<?php
listLogs("compilelogs");
?>
</ul>

<h2><a name="console" id="console"></a>Console Logs</h2>
<p>These logs contain the console output captured while running the JUnit automated tests.</p>

<ul>
<?php
listLogs("$testresults/consolelogs");
listDetailedLogs($testresults,$expectedTestConfigs[0]);
listDetailedLogs($testresults,$expectedTestConfigs[1]);
listDetailedLogs($testresults,$expectedTestConfigs[2]);
?>
</ul>

</div>
</div>
</body>
</html>
 