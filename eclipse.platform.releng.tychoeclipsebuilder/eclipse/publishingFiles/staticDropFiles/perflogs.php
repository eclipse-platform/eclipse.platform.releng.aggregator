<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<?php
$testresults="performance";
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

<title>Performance Test Logs for <?= $BUILD_ID ?></title>
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
<li><a href="perflogs.php">Performance Logs</a></li>
<li><a href="performance/performance.php#UnitTest">Performance Unit Test Results</a></li>
</ul>

</div>

<div id="midcolumn">
<div class="homeitem3col">
<h1>Performance Unit Test Logs for <?= $BUILD_ID ?></h1>


<!-- 
 No Javadoc logs.










-->

<h2><a name="console" id="console"></a>Console Logs</h2>
<p>These logs contain the console output captured while running the JUnit automated tests.</p>

<?php
listLogs("$testresults/consolelogs");
listDetailedLogs($testresults,$expectedTestConfigs[0]);
listDetailedLogs($testresults,$expectedTestConfigs[1]);
listDetailedLogs($testresults,$expectedTestConfigs[2]);
?>

</div>
</div>
</body>
</html>
