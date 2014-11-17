<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<?php
//ini_set("display_errors", "true");
//error_reporting (E_ALL);
include ('buildproperties.php');
include ('testConfigs.php');
if (array_key_exists("SERVER_NAME", $_SERVER)) {
    $servername = $_SERVER["SERVER_NAME"];
    if ($servername === "build.eclipse.org") {
          $imagesource="http://download.eclipse.org/eclipse.org-common/themes/Phoenix/images";
          $csssource="http://download.eclipse.org/eclipse.org-common//themes/Phoenix/css";
          $appsource="/home/data/httpd/download.eclipse.org/eclipse.org-common/system";
          $clickthroughstr="";
      }
      else {
          $imagesource="../../../eclipse.org-common/stylesheets";
          $csssource="../../../eclipse.org-common/stylesheets";
          $appsource="../../../eclipse.org-common/system";
          $clickthroughstr="download.php?dropFile=";

      }
}
else {
    $servername = "localhost";
          $imagesource="http://download.eclipse.org/eclipse.org-common/themes/Phoenix/images";
          $csssource="http://download.eclipse.org/eclipse.org-common//themes/Phoenix/css";
          $appsource="NONE";
          $clickthroughstr="";
}

?>
<head>

<?php

     echo "<title>Test Results for $BUILD_ID Performance Unit Tests</title>";
?>



<STYLE TYPE="text/css">
<!--
P {text-indent: 30pt; margin: inherit}
-->
</STYLE>


<title>Drop Test Results</title>
     <meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1" />
     <meta name="author" content="Eclipse Foundation, Inc." />
     <meta name="keywords" content="eclipse,project,plug-ins,plugins,java,ide,swt,refactoring,free java ide,tools,platform,open source,development environment,development,ide" />
     <link rel="stylesheet" type="text/css" href="<?php echo $csssource;?>/visual.css" media="screen" />
     <link rel="stylesheet" type="text/css" href="<?php echo $csssource;?>/layout.css" media="screen" />
     <link rel="stylesheet" type="text/css" href="<?php echo $csssource;?>/print.css" media="print" />
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

<?php if (! isset ($BUILD_FAILED) ) { ?>

<div id="leftcol">
<ul id="leftnav">
<li><a href="#Logs">Logs</a></li>
<li><a href="#UnitTest">Performance Unit Test Results</a></li>

</ul>

</div>
<!-- end 'not build failed' -->
<?php } ?>

<div id="midcolumn">
<p><b><font face="Verdana" size="+3">Test Results</font></b> </p>
<div class="homeitem3col">
<h3><a name="Logs"> Logs <?php echo "$BUILD_ID"; ?></h3>
<ul>

<li> <a href="perfBaselinelogs.php#console"><b> Console Output Logs </b></a>
</li>

</div>

<div class="homeitem3col">
<h3><a name="UnitTest">Performance Unit Test Results for <?php echo "$BUILD_ID"; ?> </a></h3>

<p>The table shows the unit test results for this build on the platforms
tested. You may access the test results page specific to each
component on a specific platform by clicking the cell link.
Normally, the number of errors is indicated in the cell.
A "-1" or "DNF" means the test "Did Not Finish" for unknown reasosns
and hence no results page is available. In that case,
more information can sometimes be found in
the <a href="perfBaselinelogs.php#console">console logs</a>.</p>
<?php
if (file_exists("testNotes.html")) {
    $my_file = file_get_contents("testNotes.html");
    echo $my_file;
}
?>

<table width="85%" border="1" bgcolor="#EEEEEE" rules="groups" align="center">
<tr bgcolor="#9999CC">
<th rowspan="2" width="40%" align="center"> org.eclipse <br> Component </th>
<th colspan="5" align="center"> Test Configurations </th></tr>
<tr bgcolor="#9999CC">
<!-- The order of the columns is "hard coded". Linux, Mac, Windows -->
<th width="20%"><?= $expectedTestConfigs[0] ?></th>
<th width="20%"><?= $expectedTestConfigs[1] ?></th>
<th width="20%"><?= $expectedTestConfigs[2] ?></th>
<th><th width="20%"></th>
</tr>
%testresults%
</table>
</br>
</div>

</body>
</html>
