<?php
# Set the theme for your project's web pages.
# See the Committer Tools "How Do I" for list of themes
# https://dev.eclipse.org/committers/
# Largely copied from the RAP team

//  ini_set('display_errors', 1); ini_set('error_reporting', E_ALL);

$Nav->setLinkList( array() );
$Nav->addNavSeparator("Work Areas", "");
$Nav->addCustomNav("Bundles", "https://eclipse.org/equinox/bundles", "_self", 1);
$Nav->addCustomNav("Framework", "https://eclipse.org/equinox/framework", "_self", 1);
$Nav->addCustomNav("Incubator", "https://eclipse.org/equinox/incubator", "_self", 1);
$Nav->addCustomNav("p2", "https://eclipse.org/equinox/p2", "_self", 1);
$Nav->addCustomNav("Security", "https://eclipse.org/equinox/security", "_self", 1);
$Nav->addCustomNav("Server", "https://eclipse.org/equinox/server", "_self", 1);

if (file_exists("component-links.php"))
  include "component-links.php";
else
  if (file_exists("../component-links.php"))
    include "../component-links.php";

$Nav->addNavSeparator("Related", "");
$Nav->addCustomNav("RT", "https://eclipse.org/rt", "_self", 1);
$Nav->addCustomNav("Eclipse", "https://eclipse.org/eclipse", "_self", 1);
$Nav->addCustomNav("PDE", "https://eclipse.org/pde", "_self", 1);
$Nav->addCustomNav("RAP", "https://eclipse.org/rap", "_self", 1);
$Nav->addCustomNav("ECF", "https://eclipse.org/ecf", "_self", 1);
$Nav->addCustomNav("Development", "https://eclipse.org/eclipse/development", "_self", 1);

$Menu->setMenuItemList( array() );
$Menu->addMenuItem( "Home", "https://eclipse.org/equinox/", "_self" );
$Menu->addMenuItem( "Get Started", "https://eclipse.org/equinox/documents/quickstart.php", "_self" );
$Menu->addMenuItem( "Downloads", "https://download.eclipse.org/equinox", "_self" );
$Menu->addMenuItem( "Documents", "https://eclipse.org/equinox/documents/", "_self" );
$Menu->addMenuItem( "Resources", "https://eclipse.org/equinox/resources.php", "_self" );
$Menu->addMenuItem( "FAQ", "https://eclipse.org/equinox/faq.php", "_self" );
$Menu->addMenuItem( "Wiki", "https://wiki.eclipse.org/Equinox", "_self" );

$App->AddExtraHtmlHeader( '<link rel="stylesheet" type="text/css" href="https://eclipse.org/equinox/equinox.css"/>' );
$App->AddExtraHtmlHeader( '<link rel="stylesheet" type="text/css" href="https://eclipse.org/equinox/rap-layout-fixes.css"/>' );
$App->AddExtraHtmlHeader( '<!--[if lt IE 8]><link rel="stylesheet" type="text/css" href="https://eclipse.org/equinox/rap-layout-fixes-ie.css"/><![endif]-->' );
$App->AddExtraHtmlHeader( '<link rel="shortcut icon" href="https://eclipse.org/rt/images/favicon.ico" />');
$App->AddExtraHtmlHeader( '<link rel="stylesheet" type="text/css" href="https://eclipse.org/equinox/rap-posts.css"/>' );
$App->AddExtraHtmlHeader( '<script type="text/javascript" src="http://code.jquery.com/jquery-1.4.2.min.js"></script>' );
$App->AddExtraHtmlHeader( '<script type="text/javascript" src="https://eclipse.org/equinox/jquery.zrssfeed.min.js"></script>' );

//  $App->Promotion = TRUE; # set true to enable current eclipse.org site-wide promo

function createRapNavigation() {
  $html = <<<EOHTML
<div id="rap-small-header">
  <a href="https://eclipse.org/equinox/"><div id="rap-small-logo"></div></a>
</div>
<script type="text/javascript">
  // logo
  var logo = $( '#logo:first-child' );
  var newLogo = '<div id="logo"><a href="https://eclipse.org">'
                + logo.html() + '</a></div>';
  logo.replaceWith( newLogo );
</script>


EOHTML;

  return $html;
}

function generateRapPage( $App, $Menu, $Nav, $author, $keywords, $title, $html )
{
  $theme = "Nova";
  $pageHtml = createRapNavigation() . $html;
  $pageHtml .= file_get_contents($_SERVER['DOCUMENT_ROOT'] . "/equinox/right-links.html");
  $App->generatePage( $theme, $Menu, $Nav, $author, $keywords, $title, $pageHtml );
}

function xslt( $xmlFile, $xslFile ) {
  $xslDoc = new DOMDocument();
  $xslDoc->load( $xslFile );
  $xmlDoc = new DOMDocument();
  $xmlDoc->load( $xmlFile );
  $proc = new XSLTProcessor();
  $proc->importStylesheet( $xslDoc );
  return $proc->transformToXML( $xmlDoc );
}

?>
