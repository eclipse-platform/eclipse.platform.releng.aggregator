<?php
    //ini_set("display_errors", "true");
    //error_reporting (E_ALL);

    require_once($_SERVER['DOCUMENT_ROOT'] . "/eclipse.org-common/system/app.class.php");
    require_once($_SERVER['DOCUMENT_ROOT'] . "/eclipse.org-common/system/nav.class.php");
  require_once($_SERVER['DOCUMENT_ROOT'] . "/eclipse.org-common/system/menu.class.php");
  require_once("build.php");

    // we rely on this file to get $BUILD_ID, etc.
    include("buildproperties.php");

  $App = new App();
  $Nav = new Nav();
  $Menu = new Menu();
  include($App->getProjectCommon());
  # Begin: page-specific settings.  Change these.
  $pageTitle = "Equinox $BUILD_TYPE_NAME Build: $BUILD_ID";
  $pageKeywords = "equinox, osgi, framework, runtime, download";
  $pageAuthor = "Equinox committers";
  $generateDropSize = 'generateDropSize';
  $generateChecksumLinks = 'generateChecksumLinks';
  $buildlabel = "$EQ_BUILD_DIR_SEG";
  $sums512file = "checksum/equinox-$BUILD_ID-SUMSSHA512";
  if (file_exists($sums512file)) {
      $gpgchecksumline = "<p style=\"text-indent: 3em;\"><a href=\"$sums512file\">SHA512 Checksums for $BUILD_ID</a>&nbsp;(<a href=\"$sums512file.asc\">GPG</a>)</p>";
  }
  $html = <<<EOHTML


<script type="text/javascript" src="https://eclipse.org/equinox/expand.js"></script>

<div id="midcolumn">
  <h3>Equinox $BUILD_TYPE_NAME Build: $BUILD_ID</h3>
  <p><b>$BUILD_PRETTY_DATE</b></p>

  <div class="homeitem3col">
    <h3>All of Equinox</h3>
    <p> A complete set of all bundles and launchers produced by the Equinox project. This zip is also a p2 repo. </p>
    <table border="0" cellspacing="0" cellpadding="0" width="100%">
      <tr><td width="78%"/><td width="9%"/><td width="8%"/></tr>
    %equinox%
    </table>
  </div>

  <div class="homeitem3col">
    <h3>Framework Only</h3>
    <p>The Equinox OSGi R4 <a href="https://eclipse.org/equinox/framework">framework</a> implementation in a standalone package.
    </p>
    <table border="0" cellspacing="0" cellpadding="0" width="100%">
      <tr><td width="78%"/><td width="9%"/><td width="8%"/></tr>
        %framework%
    </table>
  </div>

  <div class="homeitem3col">
    <h3><a onclick="expandCollapse('addon.bundles');"><img id="addon.bundles.button" src="https://eclipse.org/equinox/images/arrow.png"/></a>&nbsp;Add-on Bundles</h3>
    <p>Individual <a href="https://eclipse.org/equinox/bundles">bundles</a> that provide
    standalone OSGi specified services or add-on mechanisms (e.g., the Eclipse extension registry) of interest to OSGi programmers.</p>
    <div id="addon.bundles" class="collapsable">
      <table border="0" cellspacing="0" cellpadding="0" width="100%">
        <tr><td width="78%"/><td width="9%"/><td width="8%"/></tr>
        %extrabundles%
      </table>
    </div>
  </div>


  <div class="homeitem3col">
    <h3><a onclick="expandCollapse('other.bundles');"><img id="other.bundles.button" src="https://eclipse.org/equinox/images/arrow.png"/></a>&nbsp;Other Required Bundles</h3>
    <p>A convenient set of bundles that are required by some of the Equinox bundles.</p>
    <div id="other.bundles" class="collapsable">
      <table border="0" cellspacing="0" cellpadding="0" width="100%">
        <tr><td width="78%"/><td width="9%"/><td width="8%"/></tr>
        %other%
      </table>
    </div>
  </div>

<!-- provisioning removed per bug 368488
  <div class="homeitem3col">
    <h3 name="provisioning"><a onclick="expandCollapse('provisioning.bundles');"><img  id="provisioning.bundles.button" src="https://eclipse.org/equinox/images/arrow.png"/></a>&nbsp;p2 Provisioning Tools</h3>
    <p>The following downloads are produced by the Provisioning team. For more about provisoning, see
    the <a href="https://wiki.eclipse.org/Category:Provisioning">provisioning articles</a> on the eclipse.org wiki.</p>
    <div id="provisioning.bundles" class="collapsable">
      <table border="0" cellspacing="0" cellpadding="0" width="100%">
        <tr><td width="78%"/><td width="9%"/><td width="8%"/></tr>
          %provisioning%
      </table>
    </div>
  </div>
-->
  <div class="homeitem3col">
    <h3 name="launchers"><a onclick="expandCollapse('launcher.bundles');"><img  id="launcher.bundles.button" src="https://eclipse.org/equinox/images/arrow.png"/></a>&nbsp;Native Launchers</h3>
    <p>Platform-specific native launchers (e.g., eclipse.exe) for the Equinox framework. See the list
    of <a href="https://www.eclipse.org/projects/project-plan.php?projectid=eclipse#target_environments">supported OS configurations</a>.</>
    <div id="launcher.bundles" class="collapsable">
      <table border="0" cellspacing="0" cellpadding="0" width="100%">
        <tr><td width="78%"/><td width="9%"/><td width="8%"/></tr>
          %launchers%
      </table>
    </div>
  </div>

  <div class="homeitem3col">
    <h3><a onclick="expandCollapse('osgistarterkits.bundles');"><img  id="osgistarterkits.bundles.button" src="https://eclipse.org/equinox/images/arrow.png"/></a>&nbsp;OSGi starter kits</h3>
    <p>A useful collection of Equinox bundles packaged as a ready to run system.  This include the framework, p2 and other frequently used service implementations. </p>
    <div id="osgistarterkits.bundles" class="collapsable">
      <table border="0" cellspacing="0" cellpadding="0" width="100%">
        <tr><td width="78%"/><td width="9%"/><td width="8%"/></tr>
          %osgistarterkits%
      </table>
    </div>
  </div>

  <div class="homeitem3col">
    <h3>Other Information</h3>
       <p><a href="https://wiki.eclipse.org/Platform-releng/How_to_check_integrity_of_downloads">How to verify a download.</a></p>
       $gpgchecksumline
  </div>


</div>

EOHTML;
  generateRapPage( $App, $Menu, $Nav, $pageAuthor, $pageKeywords, $pageTitle, $html );
?>