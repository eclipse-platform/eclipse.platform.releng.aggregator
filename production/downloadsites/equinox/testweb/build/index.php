<?php 
ini_set('display_errors', 1); ini_set('error_reporting', E_ALL);

$test = $_SERVER['DOCUMENT_ROOT'];
echo "document root $test";

require_once($_SERVER['DOCUMENT_ROOT'] . "/eclipse.org-common/system/app.class.php"); 
require_once($_SERVER['DOCUMENT_ROOT'] . "/eclipse.org-common/system/nav.class.php"); 
require_once($_SERVER['DOCUMENT_ROOT'] . "/eclipse.org-common/system/menu.class.php"); 
require_once("build.php");
 $App = new App(); $Nav	= new Nav(); $Menu = new Menu(); include($App->getProjectCommon());    # All on the same line to unclutter the user's desktop'
	# Begin: page-specific settings.  Change these. 
	$pageTitle = "Equinox @type@ Build: @buildid@";
	$pageKeywords = "equinox, osgi, framework, runtime, download";
	$pageAuthor = "Equinox committers";
	$html = <<<EOHTML


<script type="text/javascript" src="expand.js"></script>

<div id="midcolumn">
	<h3>Equinox Nightly Build: N20101006-2000</h3>
	<p><b>October 7, 2010</b></p> 
	<p>These downloads are provided under the <a href="http://www.eclipse.org/legal/epl/notice.html">
	Eclipse Foundation Software User Agreement</a>. Click <a href="verifyMD5.html">here</a> for instructions
	on verifying download integrity.</p>

	<p><a href="apitools/index.html">API Version Verification Report</a> -- Verification of this build's versions
	against those of Eclipse 3.6. See <a href="org.eclipse.releng/apiexclude/exclude_list_external.txt">here</a> for exclusions.</p> 

	<p><a href="apitools/apitoolsdeprecations/apideprecation.html">API Deprecation Report</a> --
	Report of API deprecated since 3.6.</p> 
 
	<!--p><a href="apitools/report.html"><b>API Tools Post-API Freeze Report</b></a> -- This report describes API changes 
	since 3.6M6.  Exclusions are listed in org.eclipse.releng/apiexclude/exclude_list.txt.</p--> 

	<div class="homeitem3col">
		<h3>All of Equinox</h3>
		<p> A complete set of all bundles and launchers produced by the Equinox project. This zip is also a p2 repo. </p>
		<table border="0" cellspacing="0" cellpadding="0" width="100%">
			<tr><td width="5%"/><td width="78%"/><td width="9%"/><td width="8%"/></tr>
		<tr><td align="center"><img src="OK.gif" alt="OK"/></td>
<td>&nbsp;&nbsp;<img src="repo.gif"/>&nbsp;&nbsp;<b><a href="download.php?dropFile=equinox-SDK-N20101006-2000.zip">equinox-SDK-N20101006-2000.zip</a></b></td>
<?php generateDropSize("equinox-SDK-N20101006-2000.zip");?><b><?php generateChecksumLinks("equinox-SDK-N20101006-2000.zip"); ?></b>
</tr>

		</table>
	</div>

	<div class="homeitem3col">
		<h3>Framework Only</h3>
		<p>The Equinox OSGi R4 <a href="http://eclipse.org/equinox/framework">framework</a> implementation in a standalone package.
		See the <a href="http://eclipse.org/equinox/quickstart.html">Quick Start Guide</a> for details on how to use this JAR.</p>
		<table border="0" cellspacing="0" cellpadding="0" width="100%">
			<tr><td width="5%"/><td width="78%"/><td width="9%"/><td width="8%"/></tr>
				<tr><td align="center"><img src="OK.gif" alt="OK"/></td>
<td>&nbsp;&nbsp;<b><a href="download.php?dropFile=org.eclipse.osgi_3.7.0.N20101006-2000.jar">org.eclipse.osgi_3.7.0.N20101006-2000.jar</a></b></td>
<?php generateDropSize("org.eclipse.osgi_3.7.0.N20101006-2000.jar");?><b><?php generateChecksumLinks("org.eclipse.osgi_3.7.0.N20101006-2000.jar"); ?></b>
</tr>

		</table>
	</div>

	<div class="homeitem3col">
		<h3><a onclick="expandCollapse('addon.bundles');"><img id="addon.bundles.button" src="http://eclipse.org/equinox/images/arrow.png"/></a>&nbsp;Add-on Bundles</h3>
		<p>Individual <a href="http://eclipse.org/equinox/bundles">bundles</a> that provide 
		standalone OSGi specified services or add-on mechanisms (e.g., the Eclipse extension registry) of interest to OSGi programmers.</p>
		<div id="addon.bundles" class="collapsable"> 
			<table border="0" cellspacing="0" cellpadding="0" width="100%">
				<tr><td width="5%"/><td width="78%"/><td width="9%"/><td width="8%"/></tr>
				<tr><td align="center"><img src="OK.gif" alt="OK"/></td>
<td>&nbsp;&nbsp;<b><a href="download.php?dropFile=org.eclipse.equinox.app_1.3.100.N20101006-2000.jar">org.eclipse.equinox.app_1.3.100.N20101006-2000.jar</a></b></td>
<?php generateDropSize("org.eclipse.equinox.app_1.3.100.N20101006-2000.jar");?><b><?php generateChecksumLinks("org.eclipse.equinox.app_1.3.100.N20101006-2000.jar"); ?></b>
</tr>
<tr><td align="center"><img src="OK.gif" alt="OK"/></td>
<td>&nbsp;&nbsp;<b><a href="download.php?dropFile=org.eclipse.equinox.cm_1.0.200.N20101006-2000.jar">org.eclipse.equinox.cm_1.0.200.N20101006-2000.jar</a></b></td>
<?php generateDropSize("org.eclipse.equinox.cm_1.0.200.N20101006-2000.jar");?><b><?php generateChecksumLinks("org.eclipse.equinox.cm_1.0.200.N20101006-2000.jar"); ?></b>
</tr>
<tr><td align="center"><img src="OK.gif" alt="OK"/></td>
<td>&nbsp;&nbsp;<b><a href="download.php?dropFile=org.eclipse.equinox.common_3.6.0.N20101006-2000.jar">org.eclipse.equinox.common_3.6.0.N20101006-2000.jar</a></b></td>
<?php generateDropSize("org.eclipse.equinox.common_3.6.0.N20101006-2000.jar");?><b><?php generateChecksumLinks("org.eclipse.equinox.common_3.6.0.N20101006-2000.jar"); ?></b>
</tr>
<tr><td align="center"><img src="OK.gif" alt="OK"/></td>
<td>&nbsp;&nbsp;<b><a href="download.php?dropFile=org.eclipse.equinox.device_1.0.200.N20101006-2000.jar">org.eclipse.equinox.device_1.0.200.N20101006-2000.jar</a></b></td>
<?php generateDropSize("org.eclipse.equinox.device_1.0.200.N20101006-2000.jar");?><b><?php generateChecksumLinks("org.eclipse.equinox.device_1.0.200.N20101006-2000.jar"); ?></b>
</tr>
<tr><td align="center"><img src="OK.gif" alt="OK"/></td>
<td>&nbsp;&nbsp;<b><a href="download.php?dropFile=org.eclipse.equinox.ds_1.2.100.N20101006-2000.jar">org.eclipse.equinox.ds_1.2.100.N20101006-2000.jar</a></b></td>
<?php generateDropSize("org.eclipse.equinox.ds_1.2.100.N20101006-2000.jar");?><b><?php generateChecksumLinks("org.eclipse.equinox.ds_1.2.100.N20101006-2000.jar"); ?></b>
</tr>
<tr><td align="center"><img src="OK.gif" alt="OK"/></td>
<td>&nbsp;&nbsp;<b><a href="download.php?dropFile=org.eclipse.equinox.event_1.2.1.N20101006-2000.jar">org.eclipse.equinox.event_1.2.1.N20101006-2000.jar</a></b></td>
<?php generateDropSize("org.eclipse.equinox.event_1.2.1.N20101006-2000.jar");?><b><?php generateChecksumLinks("org.eclipse.equinox.event_1.2.1.N20101006-2000.jar"); ?></b>
</tr>
<tr><td align="center"><img src="OK.gif" alt="OK"/></td>
<td>&nbsp;&nbsp;<b><a href="download.php?dropFile=org.eclipse.equinox.io_1.0.200.N20101006-2000.jar">org.eclipse.equinox.io_1.0.200.N20101006-2000.jar</a></b></td>
<?php generateDropSize("org.eclipse.equinox.io_1.0.200.N20101006-2000.jar");?><b><?php generateChecksumLinks("org.eclipse.equinox.io_1.0.200.N20101006-2000.jar"); ?></b>
</tr>
<tr><td align="center"><img src="OK.gif" alt="OK"/></td>
<td>&nbsp;&nbsp;<b><a href="download.php?dropFile=org.eclipse.equinox.ip_1.1.100.N20101006-2000.jar">org.eclipse.equinox.ip_1.1.100.N20101006-2000.jar</a></b></td>
<?php generateDropSize("org.eclipse.equinox.ip_1.1.100.N20101006-2000.jar");?><b><?php generateChecksumLinks("org.eclipse.equinox.ip_1.1.100.N20101006-2000.jar"); ?></b>
</tr>
<tr><td align="center"><img src="OK.gif" alt="OK"/></td>
<td>&nbsp;&nbsp;<b><a href="download.php?dropFile=org.eclipse.equinox.launcher_1.2.0.N20101006-2000.jar">org.eclipse.equinox.launcher_1.2.0.N20101006-2000.jar</a></b></td>
<?php generateDropSize("org.eclipse.equinox.launcher_1.2.0.N20101006-2000.jar");?><b><?php generateChecksumLinks("org.eclipse.equinox.launcher_1.2.0.N20101006-2000.jar"); ?></b>
</tr>
<tr><td align="center"><img src="OK.gif" alt="OK"/></td>
<td>&nbsp;&nbsp;<b><a href="download.php?dropFile=org.eclipse.equinox.log_1.2.100.N20101006-2000.jar">org.eclipse.equinox.log_1.2.100.N20101006-2000.jar</a></b></td>
<?php generateDropSize("org.eclipse.equinox.log_1.2.100.N20101006-2000.jar");?><b><?php generateChecksumLinks("org.eclipse.equinox.log_1.2.100.N20101006-2000.jar"); ?></b>
</tr>
<tr><td align="center"><img src="OK.gif" alt="OK"/></td>
<td>&nbsp;&nbsp;<b><a href="download.php?dropFile=org.eclipse.equinox.jsp.jasper_1.0.200.N20101006-2000.jar">org.eclipse.equinox.jsp.jasper_1.0.200.N20101006-2000.jar</a></b></td>
<?php generateDropSize("org.eclipse.equinox.jsp.jasper_1.0.200.N20101006-2000.jar");?><b><?php generateChecksumLinks("org.eclipse.equinox.jsp.jasper_1.0.200.N20101006-2000.jar"); ?></b>
</tr>
<tr><td align="center"><img src="OK.gif" alt="OK"/></td>
<td>&nbsp;&nbsp;<b><a href="download.php?dropFile=org.eclipse.equinox.jsp.jasper.registry_1.0.200.N20101006-2000.jar">org.eclipse.equinox.jsp.jasper.registry_1.0.200.N20101006-2000.jar</a></b></td>
<?php generateDropSize("org.eclipse.equinox.jsp.jasper.registry_1.0.200.N20101006-2000.jar");?><b><?php generateChecksumLinks("org.eclipse.equinox.jsp.jasper.registry_1.0.200.N20101006-2000.jar"); ?></b>
</tr>
<tr><td align="center"><img src="OK.gif" alt="OK"/></td>
<td>&nbsp;&nbsp;<b><a href="download.php?dropFile=org.eclipse.equinox.metatype_1.0.100.N20101006-2000.jar">org.eclipse.equinox.metatype_1.0.100.N20101006-2000.jar</a></b></td>
<?php generateDropSize("org.eclipse.equinox.metatype_1.0.100.N20101006-2000.jar");?><b><?php generateChecksumLinks("org.eclipse.equinox.metatype_1.0.100.N20101006-2000.jar"); ?></b>
</tr>
<tr><td align="center"><img src="OK.gif" alt="OK"/></td>
<td>&nbsp;&nbsp;<b><a href="download.php?dropFile=org.eclipse.equinox.preferences_3.3.0.N20101006-2000.jar">org.eclipse.equinox.preferences_3.3.0.N20101006-2000.jar</a></b></td>
<?php generateDropSize("org.eclipse.equinox.preferences_3.3.0.N20101006-2000.jar");?><b><?php generateChecksumLinks("org.eclipse.equinox.preferences_3.3.0.N20101006-2000.jar"); ?></b>
</tr>
<tr><td align="center"><img src="OK.gif" alt="OK"/></td>
<td>&nbsp;&nbsp;<b><a href="download.php?dropFile=org.eclipse.equinox.registry_3.5.0.N20101006-2000.jar">org.eclipse.equinox.registry_3.5.0.N20101006-2000.jar</a></b></td>
<?php generateDropSize("org.eclipse.equinox.registry_3.5.0.N20101006-2000.jar");?><b><?php generateChecksumLinks("org.eclipse.equinox.registry_3.5.0.N20101006-2000.jar"); ?></b>
</tr>
<tr><td align="center"><img src="OK.gif" alt="OK"/></td>
<td>&nbsp;&nbsp;<b><a href="download.php?dropFile=org.eclipse.osgi.services_3.2.100.N20101006-2000.jar">org.eclipse.osgi.services_3.2.100.N20101006-2000.jar</a></b></td>
<?php generateDropSize("org.eclipse.osgi.services_3.2.100.N20101006-2000.jar");?><b><?php generateChecksumLinks("org.eclipse.osgi.services_3.2.100.N20101006-2000.jar"); ?></b>
</tr>
<tr><td align="center"><img src="OK.gif" alt="OK"/></td>
<td>&nbsp;&nbsp;<b><a href="download.php?dropFile=org.eclipse.equinox.supplement_1.3.100.N20101006-2000.jar">org.eclipse.equinox.supplement_1.3.100.N20101006-2000.jar</a></b></td>
<?php generateDropSize("org.eclipse.equinox.supplement_1.3.100.N20101006-2000.jar");?><b><?php generateChecksumLinks("org.eclipse.equinox.supplement_1.3.100.N20101006-2000.jar"); ?></b>
</tr>
<tr><td align="center"><img src="OK.gif" alt="OK"/></td>
<td>&nbsp;&nbsp;<b><a href="download.php?dropFile=org.eclipse.equinox.transforms.xslt_1.0.200.N20101006-2000.jar">org.eclipse.equinox.transforms.xslt_1.0.200.N20101006-2000.jar</a></b></td>
<?php generateDropSize("org.eclipse.equinox.transforms.xslt_1.0.200.N20101006-2000.jar");?><b><?php generateChecksumLinks("org.eclipse.equinox.transforms.xslt_1.0.200.N20101006-2000.jar"); ?></b>
</tr>
<tr><td align="center"><img src="OK.gif" alt="OK"/></td>
<td>&nbsp;&nbsp;<b><a href="download.php?dropFile=org.eclipse.equinox.transforms.hook_1.0.300.N20101006-2000.jar">org.eclipse.equinox.transforms.hook_1.0.300.N20101006-2000.jar</a></b></td>
<?php generateDropSize("org.eclipse.equinox.transforms.hook_1.0.300.N20101006-2000.jar");?><b><?php generateChecksumLinks("org.eclipse.equinox.transforms.hook_1.0.300.N20101006-2000.jar"); ?></b>
</tr>
<tr><td align="center"><img src="OK.gif" alt="OK"/></td>
<td>&nbsp;&nbsp;<b><a href="download.php?dropFile=org.eclipse.equinox.util_1.0.200.N20101006-2000.jar">org.eclipse.equinox.util_1.0.200.N20101006-2000.jar</a></b></td>
<?php generateDropSize("org.eclipse.equinox.util_1.0.200.N20101006-2000.jar");?><b><?php generateChecksumLinks("org.eclipse.equinox.util_1.0.200.N20101006-2000.jar"); ?></b>
</tr>
<tr><td align="center"><img src="OK.gif" alt="OK"/></td>
<td>&nbsp;&nbsp;<b><a href="download.php?dropFile=org.eclipse.equinox.useradmin_1.1.200.N20101006-2000.jar">org.eclipse.equinox.useradmin_1.1.200.N20101006-2000.jar</a></b></td>
<?php generateDropSize("org.eclipse.equinox.useradmin_1.1.200.N20101006-2000.jar");?><b><?php generateChecksumLinks("org.eclipse.equinox.useradmin_1.1.200.N20101006-2000.jar"); ?></b>
</tr>
<tr><td align="center"><img src="OK.gif" alt="OK"/></td>
<td>&nbsp;&nbsp;<b><a href="download.php?dropFile=org.eclipse.osgi.util_3.2.100.N20101006-2000.jar">org.eclipse.osgi.util_3.2.100.N20101006-2000.jar</a></b></td>
<?php generateDropSize("org.eclipse.osgi.util_3.2.100.N20101006-2000.jar");?><b><?php generateChecksumLinks("org.eclipse.osgi.util_3.2.100.N20101006-2000.jar"); ?></b>
</tr>
<tr><td align="center"><img src="OK.gif" alt="OK"/></td>
<td>&nbsp;&nbsp;<b><a href="download.php?dropFile=org.eclipse.equinox.servletbridge_1.2.0.N20101006-2000.jar">org.eclipse.equinox.servletbridge_1.2.0.N20101006-2000.jar</a></b></td>
<?php generateDropSize("org.eclipse.equinox.servletbridge_1.2.0.N20101006-2000.jar");?><b><?php generateChecksumLinks("org.eclipse.equinox.servletbridge_1.2.0.N20101006-2000.jar"); ?></b>
</tr>
<tr><td align="center"><img src="OK.gif" alt="OK"/></td>
<td>&nbsp;&nbsp;<b><a href="download.php?dropFile=org.eclipse.equinox.http_1.0.400.N20101006-2000.jar">org.eclipse.equinox.http_1.0.400.N20101006-2000.jar</a></b></td>
<?php generateDropSize("org.eclipse.equinox.http_1.0.400.N20101006-2000.jar");?><b><?php generateChecksumLinks("org.eclipse.equinox.http_1.0.400.N20101006-2000.jar"); ?></b>
</tr>
<tr><td align="center"><img src="OK.gif" alt="OK"/></td>
<td>&nbsp;&nbsp;<b><a href="download.php?dropFile=org.eclipse.equinox.http.jetty_1.1.100.N20101006-2000.jar">org.eclipse.equinox.http.jetty_1.1.100.N20101006-2000.jar</a></b></td>
<?php generateDropSize("org.eclipse.equinox.http.jetty_1.1.100.N20101006-2000.jar");?><b><?php generateChecksumLinks("org.eclipse.equinox.http.jetty_1.1.100.N20101006-2000.jar"); ?></b>
</tr>
<tr><td align="center"><img src="OK.gif" alt="OK"/></td>
<td>&nbsp;&nbsp;<b><a href="download.php?dropFile=org.eclipse.equinox.http.jetty_2.0.0.N20101006-2000.jar">org.eclipse.equinox.http.jetty_2.0.0.N20101006-2000.jar</a></b></td>
<?php generateDropSize("org.eclipse.equinox.http.jetty_2.0.0.N20101006-2000.jar");?><b><?php generateChecksumLinks("org.eclipse.equinox.http.jetty_2.0.0.N20101006-2000.jar"); ?></b>
</tr>
<tr><td align="center"><img src="OK.gif" alt="OK"/></td>
<td>&nbsp;&nbsp;<b><a href="download.php?dropFile=org.eclipse.equinox.http.registry_1.1.0.N20101006-2000.jar">org.eclipse.equinox.http.registry_1.1.0.N20101006-2000.jar</a></b></td>
<?php generateDropSize("org.eclipse.equinox.http.registry_1.1.0.N20101006-2000.jar");?><b><?php generateChecksumLinks("org.eclipse.equinox.http.registry_1.1.0.N20101006-2000.jar"); ?></b>
</tr>
<tr><td align="center"><img src="OK.gif" alt="OK"/></td>
<td>&nbsp;&nbsp;<b><a href="download.php?dropFile=org.eclipse.equinox.http.servletbridge_1.0.200.N20101006-2000.jar">org.eclipse.equinox.http.servletbridge_1.0.200.N20101006-2000.jar</a></b></td>
<?php generateDropSize("org.eclipse.equinox.http.servletbridge_1.0.200.N20101006-2000.jar");?><b><?php generateChecksumLinks("org.eclipse.equinox.http.servletbridge_1.0.200.N20101006-2000.jar"); ?></b>
</tr>
<tr><td align="center"><img src="OK.gif" alt="OK"/></td>
<td>&nbsp;&nbsp;<b><a href="download.php?dropFile=org.eclipse.equinox.http.servlet_1.1.0.N20101006-2000.jar">org.eclipse.equinox.http.servlet_1.1.0.N20101006-2000.jar</a></b></td>
<?php generateDropSize("org.eclipse.equinox.http.servlet_1.1.0.N20101006-2000.jar");?><b><?php generateChecksumLinks("org.eclipse.equinox.http.servlet_1.1.0.N20101006-2000.jar"); ?></b>
</tr>
<tr><td align="center"><img src="OK.gif" alt="OK"/></td>
<td>&nbsp;&nbsp;<b><a href="download.php?dropFile=org.eclipse.equinox.wireadmin_1.0.200.N20101006-2000.jar">org.eclipse.equinox.wireadmin_1.0.200.N20101006-2000.jar</a></b></td>
<?php generateDropSize("org.eclipse.equinox.wireadmin_1.0.200.N20101006-2000.jar");?><b><?php generateChecksumLinks("org.eclipse.equinox.wireadmin_1.0.200.N20101006-2000.jar"); ?></b>
</tr>
<tr><td align="center"><img src="OK.gif" alt="OK"/></td>
<td>&nbsp;&nbsp;<b><a href="download.php?dropFile=org.eclipse.equinox.simpleconfigurator_1.0.200.N20101006-2000.jar">org.eclipse.equinox.simpleconfigurator_1.0.200.N20101006-2000.jar</a></b></td>
<?php generateDropSize("org.eclipse.equinox.simpleconfigurator_1.0.200.N20101006-2000.jar");?><b><?php generateChecksumLinks("org.eclipse.equinox.simpleconfigurator_1.0.200.N20101006-2000.jar"); ?></b>
</tr>
<tr><td align="center"><img src="OK.gif" alt="OK"/></td>
<td>&nbsp;&nbsp;<b><a href="download.php?dropFile=org.eclipse.equinox.concurrent_1.0.100.N20101006-2000.jar">org.eclipse.equinox.concurrent_1.0.100.N20101006-2000.jar</a></b></td>
<?php generateDropSize("org.eclipse.equinox.concurrent_1.0.100.N20101006-2000.jar");?><b><?php generateChecksumLinks("org.eclipse.equinox.concurrent_1.0.100.N20101006-2000.jar"); ?></b>
</tr>

			</table>
		</div>
	</div>

	<div class="homeitem3col">
		<h3><a onclick="expandCollapse('other.bundles');"><img id="other.bundles.button" src="http://eclipse.org/equinox/images/arrow.png"/></a>&nbsp;Other Required Bundles</h3>
		<p>A convenient set of bundles that are required by some of the Equinox bundles.</p>
		<div id="other.bundles" class="collapsable"> 
			<table border="0" cellspacing="0" cellpadding="0" width="100%">
				<tr><td width="5%"/><td width="78%"/><td width="9%"/><td width="8%"/></tr>
				<tr><td align="center"><img src="OK.gif" alt="OK"/></td>
<td>&nbsp;&nbsp;<b><a href="download.php?dropFile=javax.servlet_2.4.0.v200806031604.jar">javax.servlet_2.4.0.v200806031604.jar</a></b></td>
<?php generateDropSize("javax.servlet_2.4.0.v200806031604.jar");?><b><?php generateChecksumLinks("javax.servlet_2.4.0.v200806031604.jar"); ?></b>
</tr>
<tr><td align="center"><img src="OK.gif" alt="OK"/></td>
<td>&nbsp;&nbsp;<b><a href="download.php?dropFile=javax.servlet_2.5.0.v200910301333.jar">javax.servlet_2.5.0.v200910301333.jar</a></b></td>
<?php generateDropSize("javax.servlet_2.5.0.v200910301333.jar");?><b><?php generateChecksumLinks("javax.servlet_2.5.0.v200910301333.jar"); ?></b>
</tr>
<tr><td align="center"><img src="OK.gif" alt="OK"/></td>
<td>&nbsp;&nbsp;<b><a href="download.php?dropFile=javax.servlet.jsp_2.0.0.v200806031607.jar">javax.servlet.jsp_2.0.0.v200806031607.jar</a></b></td>
<?php generateDropSize("javax.servlet.jsp_2.0.0.v200806031607.jar");?><b><?php generateChecksumLinks("javax.servlet.jsp_2.0.0.v200806031607.jar"); ?></b>
</tr>
<tr><td align="center"><img src="OK.gif" alt="OK"/></td>
<td>&nbsp;&nbsp;<b><a href="download.php?dropFile=org.apache.commons.el_1.0.0.v201004212143.jar">org.apache.commons.el_1.0.0.v201004212143.jar</a></b></td>
<?php generateDropSize("org.apache.commons.el_1.0.0.v201004212143.jar");?><b><?php generateChecksumLinks("org.apache.commons.el_1.0.0.v201004212143.jar"); ?></b>
</tr>
<tr><td align="center"><img src="OK.gif" alt="OK"/></td>
<td>&nbsp;&nbsp;<b><a href="download.php?dropFile=org.apache.commons.logging_1.0.4.v201005080501.jar">org.apache.commons.logging_1.0.4.v201005080501.jar</a></b></td>
<?php generateDropSize("org.apache.commons.logging_1.0.4.v201005080501.jar");?><b><?php generateChecksumLinks("org.apache.commons.logging_1.0.4.v201005080501.jar"); ?></b>
</tr>
<tr><td align="center"><img src="OK.gif" alt="OK"/></td>
<td>&nbsp;&nbsp;<b><a href="download.php?dropFile=org.mortbay.jetty_5.1.14.v200806031611.jar">org.mortbay.jetty_5.1.14.v200806031611.jar</a></b></td>
<?php generateDropSize("org.mortbay.jetty_5.1.14.v200806031611.jar");?><b><?php generateChecksumLinks("org.mortbay.jetty_5.1.14.v200806031611.jar"); ?></b>
</tr>
<tr><td align="center"><img src="OK.gif" alt="OK"/></td>
<td>&nbsp;&nbsp;<b><a href="download.php?dropFile=org.mortbay.jetty.util_6.1.23.v201004211559.jar">org.mortbay.jetty.util_6.1.23.v201004211559.jar</a></b></td>
<?php generateDropSize("org.mortbay.jetty.util_6.1.23.v201004211559.jar");?><b><?php generateChecksumLinks("org.mortbay.jetty.util_6.1.23.v201004211559.jar"); ?></b>
</tr>
<tr><td align="center"><img src="OK.gif" alt="OK"/></td>
<td>&nbsp;&nbsp;<b><a href="download.php?dropFile=org.mortbay.jetty.server_6.1.23.v201004211559.jar">org.mortbay.jetty.server_6.1.23.v201004211559.jar</a></b></td>
<?php generateDropSize("org.mortbay.jetty.server_6.1.23.v201004211559.jar");?><b><?php generateChecksumLinks("org.mortbay.jetty.server_6.1.23.v201004211559.jar"); ?></b>
</tr>

			</table>
		</div>
	</div>

	<div class="homeitem3col">
		<h3><a onclick="expandCollapse('incubator.bundles');"><img id="incubator.bundles.button" src="http://eclipse.org/equinox/images/arrow.png"/></a>&nbsp;Incubator</h3>
		<p>Equinox Incubator downloads supplied as a convenience. While some are quite complete, all should be considered experimental.</p>
		<div id="incubator.bundles" class="collapsable"> 
			<table border="0" cellspacing="0" cellpadding="0" width="100%">
				<tr><td width="5%"/><td width="78%"/><td width="9%"/><td width="8%"/></tr>
					<tr><td align="center"><img src="OK.gif" alt="OK"/></td>
<td>&nbsp;&nbsp;<img src="egg.gif"/>&nbsp;&nbsp;<b><a href="download.php?dropFile=org.eclipse.equinox.initializer_1.0.0.N20101006-2000.jar">org.eclipse.equinox.initializer_1.0.0.N20101006-2000.jar</a></b></td>
<?php generateDropSize("org.eclipse.equinox.initializer_1.0.0.N20101006-2000.jar");?><b><?php generateChecksumLinks("org.eclipse.equinox.initializer_1.0.0.N20101006-2000.jar"); ?></b>
</tr>
<tr><td align="center"><img src="OK.gif" alt="OK"/></td>
<td>&nbsp;&nbsp;<img src="egg.gif"/>&nbsp;&nbsp;<img src="repo.gif"/>&nbsp;&nbsp;<b><a href="download.php?dropFile=equinox-jmx-common-N20101006-2000.zip">equinox-jmx-common-N20101006-2000.zip</a></b></td>
<?php generateDropSize("equinox-jmx-common-N20101006-2000.zip");?><b><?php generateChecksumLinks("equinox-jmx-common-N20101006-2000.zip"); ?></b>
</tr>
<tr><td align="center"><img src="OK.gif" alt="OK"/></td>
<td>&nbsp;&nbsp;<img src="egg.gif"/>&nbsp;&nbsp;<img src="repo.gif"/>&nbsp;&nbsp;<b><a href="download.php?dropFile=equinox-jmx-client-N20101006-2000.zip">equinox-jmx-client-N20101006-2000.zip</a></b></td>
<?php generateDropSize("equinox-jmx-client-N20101006-2000.zip");?><b><?php generateChecksumLinks("equinox-jmx-client-N20101006-2000.zip"); ?></b>
</tr>
<tr><td align="center"><img src="OK.gif" alt="OK"/></td>
<td>&nbsp;&nbsp;<img src="egg.gif"/>&nbsp;&nbsp;<img src="repo.gif"/>&nbsp;&nbsp;<b><a href="download.php?dropFile=equinox-jmx-server-N20101006-2000.zip">equinox-jmx-server-N20101006-2000.zip</a></b></td>
<?php generateDropSize("equinox-jmx-server-N20101006-2000.zip");?><b><?php generateChecksumLinks("equinox-jmx-server-N20101006-2000.zip"); ?></b>
</tr>

			</table>
		</div>
	</div>

	<div class="homeitem3col">
		<h3><a onclick="expandCollapse('provisioning.bundles');"><img  id="provisioning.bundles.button" src="http://eclipse.org/equinox/images/arrow.png"/></a>&nbsp;p2 Provisioning Tools</h3>
		<p>The following downloads are produced by the Provisioning team. For more about provisoning, see 
		the <a href="http://wiki.eclipse.org/Category:Provisioning">provisioning articles</a> on the eclipse.org wiki.</p>
		<div id="provisioning.bundles" class="collapsable"> 
			<table border="0" cellspacing="0" cellpadding="0" width="100%">
				<tr><td width="5%"/><td width="78%"/><td width="9%"/><td width="8%"/></tr>
					<tr><td align="center"><img src="OK.gif" alt="OK"/></td>
<td>&nbsp;&nbsp;<img src="egg.gif"/>&nbsp;&nbsp;<b><a href="download.php?dropFile=equinox-p2-adminui-N20101006-2000-win32.zip">equinox-p2-adminui-N20101006-2000-win32.zip</a></b></td>
<?php generateDropSize("equinox-p2-adminui-N20101006-2000-win32.zip");?><b><?php generateChecksumLinks("equinox-p2-adminui-N20101006-2000-win32.zip"); ?></b>
</tr>
<tr><td align="center"><img src="OK.gif" alt="OK"/></td>
<td>&nbsp;&nbsp;<img src="egg.gif"/>&nbsp;&nbsp;<b><a href="download.php?dropFile=equinox-p2-adminui-N20101006-2000-linux-gtk-x86.tar.gz">equinox-p2-adminui-N20101006-2000-linux-gtk-x86.tar.gz</a></b></td>
<?php generateDropSize("equinox-p2-adminui-N20101006-2000-linux-gtk-x86.tar.gz");?><b><?php generateChecksumLinks("equinox-p2-adminui-N20101006-2000-linux-gtk-x86.tar.gz"); ?></b>
</tr>
<tr><td align="center"><img src="OK.gif" alt="OK"/></td>
<td>&nbsp;&nbsp;<img src="egg.gif"/>&nbsp;&nbsp;<b><a href="download.php?dropFile=equinox-p2-adminui-N20101006-2000-macosx-cocoa-x86.tar.gz">equinox-p2-adminui-N20101006-2000-macosx-cocoa-x86.tar.gz</a></b></td>
<?php generateDropSize("equinox-p2-adminui-N20101006-2000-macosx-cocoa-x86.tar.gz");?><b><?php generateChecksumLinks("equinox-p2-adminui-N20101006-2000-macosx-cocoa-x86.tar.gz"); ?></b>
</tr>
<tr><td align="center"><img src="OK.gif" alt="OK"/></td>
<td>&nbsp;&nbsp;<img src="egg.gif"/>&nbsp;&nbsp;<b><a href="download.php?dropFile=equinox.p2.installer-N20101006-2000-win32.win32.x86.zip">equinox.p2.installer-N20101006-2000-win32.win32.x86.zip</a></b></td>
<?php generateDropSize("equinox.p2.installer-N20101006-2000-win32.win32.x86.zip");?><b><?php generateChecksumLinks("equinox.p2.installer-N20101006-2000-win32.win32.x86.zip"); ?></b>
</tr>
<tr><td align="center"><img src="OK.gif" alt="OK"/></td>
<td>&nbsp;&nbsp;<img src="egg.gif"/>&nbsp;&nbsp;<b><a href="download.php?dropFile=equinox.p2.installer-N20101006-2000-linux.gtk.x86.tar.gz">equinox.p2.installer-N20101006-2000-linux.gtk.x86.tar.gz</a></b></td>
<?php generateDropSize("equinox.p2.installer-N20101006-2000-linux.gtk.x86.tar.gz");?><b><?php generateChecksumLinks("equinox.p2.installer-N20101006-2000-linux.gtk.x86.tar.gz"); ?></b>
</tr>
<tr><td align="center"><img src="OK.gif" alt="OK"/></td>
<td>&nbsp;&nbsp;<img src="egg.gif"/>&nbsp;&nbsp;<b><a href="download.php?dropFile=equinox.p2.installer-N20101006-2000-macosx.carbon.x86.tar.gz">equinox.p2.installer-N20101006-2000-macosx.carbon.x86.tar.gz</a></b></td>
<?php generateDropSize("equinox.p2.installer-N20101006-2000-macosx.carbon.x86.tar.gz");?><b><?php generateChecksumLinks("equinox.p2.installer-N20101006-2000-macosx.carbon.x86.tar.gz"); ?></b>
</tr>

			</table>
		</div>
	</div>

	<div class="homeitem3col">
		<h3><a onclick="expandCollapse('launcher.bundles');"><img  id="launcher.bundles.button" src="http://eclipse.org/equinox/images/arrow.png"/></a>&nbsp;Native Launchers</h3>
		<p>Platform-specific native launchers (e.g., eclipse.exe) for the Equinox framework. See the list 
		of <a href="http://www.eclipse.org/projects/project-plan.php?projectid=eclipse#target_environments">supported OS configurations</a>.</>
		<div id="launcher.bundles" class="collapsable"> 
			<table border="0" cellspacing="0" cellpadding="0" width="100%">
				<tr><td width="5%"/><td width="78%"/><td width="9%"/><td width="8%"/></tr>
					<tr><td align="center"><img src="OK.gif" alt="OK"/></td>
<td>&nbsp;&nbsp;<b><a href="download.php?dropFile=launchers-win32.win32.x86.N20101006-2000.zip">launchers-win32.win32.x86.N20101006-2000.zip</a></b></td>
<?php generateDropSize("launchers-win32.win32.x86.N20101006-2000.zip");?><b><?php generateChecksumLinks("launchers-win32.win32.x86.N20101006-2000.zip"); ?></b>
</tr>
<tr><td align="center"><img src="OK.gif" alt="OK"/></td>
<td>&nbsp;&nbsp;<b><a href="download.php?dropFile=launchers-win32.win32.x86_64.N20101006-2000.zip">launchers-win32.win32.x86_64.N20101006-2000.zip</a></b></td>
<?php generateDropSize("launchers-win32.win32.x86_64.N20101006-2000.zip");?><b><?php generateChecksumLinks("launchers-win32.win32.x86_64.N20101006-2000.zip"); ?></b>
</tr>
<tr><td align="center"><img src="OK.gif" alt="OK"/></td>
<td>&nbsp;&nbsp;<b><a href="download.php?dropFile=launchers-linux.gtk.x86.N20101006-2000.tar.gz">launchers-linux.gtk.x86.N20101006-2000.tar.gz</a></b></td>
<?php generateDropSize("launchers-linux.gtk.x86.N20101006-2000.tar.gz");?><b><?php generateChecksumLinks("launchers-linux.gtk.x86.N20101006-2000.tar.gz"); ?></b>
</tr>
<tr><td align="center"><img src="OK.gif" alt="OK"/></td>
<td>&nbsp;&nbsp;<b><a href="download.php?dropFile=launchers-linux.gtk.x86_64.N20101006-2000.tar.gz">launchers-linux.gtk.x86_64.N20101006-2000.tar.gz</a></b></td>
<?php generateDropSize("launchers-linux.gtk.x86_64.N20101006-2000.tar.gz");?><b><?php generateChecksumLinks("launchers-linux.gtk.x86_64.N20101006-2000.tar.gz"); ?></b>
</tr>
<tr><td align="center"><img src="OK.gif" alt="OK"/></td>
<td>&nbsp;&nbsp;<b><a href="download.php?dropFile=launchers-linux.gtk.ppc64.N20101006-2000.tar.gz">launchers-linux.gtk.ppc64.N20101006-2000.tar.gz</a></b></td>
<?php generateDropSize("launchers-linux.gtk.ppc64.N20101006-2000.tar.gz");?><b><?php generateChecksumLinks("launchers-linux.gtk.ppc64.N20101006-2000.tar.gz"); ?></b>
</tr>
<tr><td align="center"><img src="OK.gif" alt="OK"/></td>
<td>&nbsp;&nbsp;<b><a href="download.php?dropFile=launchers-solaris.gtk.sparc.N20101006-2000.zip">launchers-solaris.gtk.sparc.N20101006-2000.zip</a></b></td>
<?php generateDropSize("launchers-solaris.gtk.sparc.N20101006-2000.zip");?><b><?php generateChecksumLinks("launchers-solaris.gtk.sparc.N20101006-2000.zip"); ?></b>
</tr>
<tr><td align="center"><img src="OK.gif" alt="OK"/></td>
<td>&nbsp;&nbsp;<b><a href="download.php?dropFile=launchers-solaris.gtk.x86.N20101006-2000.zip">launchers-solaris.gtk.x86.N20101006-2000.zip</a></b></td>
<?php generateDropSize("launchers-solaris.gtk.x86.N20101006-2000.zip");?><b><?php generateChecksumLinks("launchers-solaris.gtk.x86.N20101006-2000.zip"); ?></b>
</tr>
<tr><td align="center"><img src="OK.gif" alt="OK"/></td>
<td>&nbsp;&nbsp;<b><a href="download.php?dropFile=launchers-hpux.motif.ia64_32.N20101006-2000.zip">launchers-hpux.motif.ia64_32.N20101006-2000.zip</a></b></td>
<?php generateDropSize("launchers-hpux.motif.ia64_32.N20101006-2000.zip");?><b><?php generateChecksumLinks("launchers-hpux.motif.ia64_32.N20101006-2000.zip"); ?></b>
</tr>
<tr><td align="center"><img src="OK.gif" alt="OK"/></td>
<td>&nbsp;&nbsp;<b><a href="download.php?dropFile=launchers-aix.motif.ppc.N20101006-2000.zip">launchers-aix.motif.ppc.N20101006-2000.zip</a></b></td>
<?php generateDropSize("launchers-aix.motif.ppc.N20101006-2000.zip");?><b><?php generateChecksumLinks("launchers-aix.motif.ppc.N20101006-2000.zip"); ?></b>
</tr>
<tr><td align="center"><img src="OK.gif" alt="OK"/></td>
<td>&nbsp;&nbsp;<b><a href="download.php?dropFile=launchers-aix.gtk.ppc64.N20101006-2000.zip">launchers-aix.gtk.ppc64.N20101006-2000.zip</a></b></td>
<?php generateDropSize("launchers-aix.gtk.ppc64.N20101006-2000.zip");?><b><?php generateChecksumLinks("launchers-aix.gtk.ppc64.N20101006-2000.zip"); ?></b>
</tr>
<tr><td align="center"><img src="OK.gif" alt="OK"/></td>
<td>&nbsp;&nbsp;<b><a href="download.php?dropFile=launchers-macosx.carbon.x86.N20101006-2000.tar.gz">launchers-macosx.carbon.x86.N20101006-2000.tar.gz</a></b></td>
<?php generateDropSize("launchers-macosx.carbon.x86.N20101006-2000.tar.gz");?><b><?php generateChecksumLinks("launchers-macosx.carbon.x86.N20101006-2000.tar.gz"); ?></b>
</tr>
<tr><td align="center"><img src="OK.gif" alt="OK"/></td>
<td>&nbsp;&nbsp;<b><a href="download.php?dropFile=launchers-macosx.cocoa.x86.N20101006-2000.tar.gz">launchers-macosx.cocoa.x86.N20101006-2000.tar.gz</a></b></td>
<?php generateDropSize("launchers-macosx.cocoa.x86.N20101006-2000.tar.gz");?><b><?php generateChecksumLinks("launchers-macosx.cocoa.x86.N20101006-2000.tar.gz"); ?></b>
</tr>
<tr><td align="center"><img src="OK.gif" alt="OK"/></td>
<td>&nbsp;&nbsp;<b><a href="download.php?dropFile=launchers-macosx.cocoa.x86_64.N20101006-2000.tar.gz">launchers-macosx.cocoa.x86_64.N20101006-2000.tar.gz</a></b></td>
<?php generateDropSize("launchers-macosx.cocoa.x86_64.N20101006-2000.tar.gz");?><b><?php generateChecksumLinks("launchers-macosx.cocoa.x86_64.N20101006-2000.tar.gz"); ?></b>
</tr>

			</table>
		</div>
	</div>

	<div class="homeitem3col"> 
		<h3>Legend</h3> 
		<table border="0" cellspacing="0" cellpadding="0" width="100%"> 
			<tr><td width="5%"/></tr> 
			<tr> 
				<td align="center"><img src="OK.gif"/></td> 
				<td>Build completed OK</td> 
			</tr> 
			<tr> 
				<td align="center"><img src="FAIL.gif"/></td> 
				<td>Build failed to complete</td> 
			</tr> 
			<tr> 
				<td align="center"><img src="pending.gif"/></td> 
				<td>Build is still pending</td> 
			</tr> 
			<tr> 
				<td align="center"><img src="repo.gif"/></td> 
				<td>Build artifact is also a p2 repository</td> 
			</tr> 
			<tr> 
				<td align="center"><img src="md5.png"/></td> 
				<td>Click to get the MD5 hash for the corresponding build artifact</td> 
			</tr> 
			<tr> 
				<td align="center"><img src="sha1.png"/></td> 
				<td>Click to get the SHA1 hash for the corresponding build artifact</td> 
			</tr> 
			<tr> 
				<td align="center"><img src="egg.gif"/></td> 
				<td>Build artifact is from the Equinox incubator and may not be fully complete or tested.</td> 
			</tr> 
		</table> 
	</div>
</div>

EOHTML;
	generateRapPage( $App, $Menu, $Nav, $pageAuthor, $pageKeywords, $pageTitle, $html );
?>
