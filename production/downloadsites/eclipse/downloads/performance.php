<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1">
<link rel="stylesheet" href="../../../default_style.css" type="text/css">
<TITLE>Performance Results</TITLE></head>
<body>
<p><b><FONT FACE="Verdana" SIZE="+3">Performance Results</FONT></b><BR><BR><A NAME="unit"></A></p><TABLE BORDER=0 CELLSPACING=5 CELLPADDING=2 WIDTH="100%" >
<TR> <TD ALIGN=LEFT VALIGN=TOP COLSPAN="3" BGCOLOR="#0080C0"><B><FONT COLOR="#FFFFFF" FACE="Arial,Helvetica">Performance
Unit Test Results for <?php echo "$buildName"; ?> </FONT></B></TD></TR> </TABLE><P></P><TABLE BORDER="0">
</TABLE>** test errors or failures here do not produce Red X's for now**<TABLE WIDTH="77%" BORDER="1">
<TR> <TD WIDTH="81%"><B>Tests Performed</B></TD><TD WIDTH="19%"><B>Errors &amp;
Failures</B></TD></TR> <tr><td><a href="performance/html/org.eclipse.ant.tests.ui_win32perf.html">org.eclipse.ant.tests.ui_win32perf</a></td><td>0</td></tr><tr><td><a href="performance/html/org.eclipse.core.tests_win32perf.html">org.eclipse.core.tests_win32perf</a></td><td>0</td></tr><tr><td><a href="performance/html/org.eclipse.help.tests_win32perf.html">org.eclipse.help.tests_win32perf</a></td><td>0</td></tr><tr><td><a href="performance/html/org.eclipse.jdt.core.tests.performance_win32perf.html">org.eclipse.jdt.core.tests.performance_win32perf</a></td><td>0</td></tr><tr><td><a href="performance/html/org.eclipse.jdt.debug.tests_win32perf.html">org.eclipse.jdt.debug.tests_win32perf</a></td><td>0</td></tr><tr><td><a href="performance/html/org.eclipse.jdt.text.tests_win32perf.html">org.eclipse.jdt.text.tests_win32perf</a></td><td>0</td></tr><tr><td><a href="performance/html/org.eclipse.jdt.ui.tests.refactoring_win32perf.html">org.eclipse.jdt.ui.tests.refactoring_win32perf</a></td><td>0</td></tr><tr><td><a href="performance/html/org.eclipse.jdt.ui.tests_win32perf.html">org.eclipse.jdt.ui.tests_win32perf</a></td><td>0</td></tr><tr><td><a href="performance/html/org.eclipse.swt.tests_win32perf.html">org.eclipse.swt.tests_win32perf</a></td><td>0</td></tr><tr><td><a href="performance/html/org.eclipse.team.tests.cvs_win32perf.html">org.eclipse.team.tests.cvs_win32perf</a></td><td>7</td></tr><tr><td><a href="performance/html/org.eclipse.ui.tests.rcp_win32perf.html">org.eclipse.ui.tests.rcp_win32perf</a></td><td>0</td></tr><tr><td><a href="performance/html/org.eclipse.ui.tests_win32perf.html">org.eclipse.ui.tests_win32perf</a></td><td>0</td></tr>
</TABLE><BR> <A NAME="consolelog"></A><TABLE BORDER=0 CELLSPACING=5 CELLPADDING=2 WIDTH="100%" >
<TR> <TD ALIGN=LEFT VALIGN=TOP COLSPAN="3" BGCOLOR="#0080C0"><B><FONT COLOR="#FFFFFF" FACE="Arial,Helvetica">
Console output logs <?php echo "$buildName"; ?> </FONT></B></TD></TR></TABLE><P>

 </P><TABLE BORDER=0 CELLSPACING=5 CELLPADDING=2 WIDTH="100%" > <TR>
<TD ALIGN=LEFT VALIGN=TOP COLSPAN="3" BGCOLOR="#0080C0"><B><FONT COLOR="#FFFFFF" FACE="Arial,Helvetica">
Detailed performance data grouped by scenario prefix</FONT></B></TD></TR></TABLE>

<?php
$aDirectory = dir("I-scenarios");
$index=0;

while ($aScenario = $aDirectory->read()) {
  if ($aScenario!= "." && $aScenario!= "..") {
    $parts=explode(".test","$aScenario");
    $packageprefixes[$index]=$parts[0];
    $index++;
  }
}
aDirectory.closedir();

$result=array_unique($packageprefixes);

sort($result);

for ($counter=0;$counter<count($result);$counter++){
  echo "<A HREF=\"displayScenarios.php?I&$result[$counter]\">$result[$counter]*</A><br>";
}
?>


</body>
</html>
