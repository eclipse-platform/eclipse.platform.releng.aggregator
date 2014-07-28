<html>
<head>
<title>Eclipse Download Click Through</title>
<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1">
<link rel="stylesheet" href="../../../default_style.css" type="text/css">
<?php

include("buildproperties.php");

function computeMirrorKey ($refurl, $buildid) {
  $dropsuffix="";
  $edpos=strpos($refurl,"/eclipse/downloads/");
  if ($edpos !== false) {
    $bidpos=strrpos($refurl,$buildid);
    if ($bidpos !== false) {
      //echo "edpos: $edpos\n";
      //echo "bidpos: $bidpos\n";
      // sanity check
      if ($bidpos > $edpos) {
        $dropsuffix=substr($refurl,$edpos,($bidpos - $edpos -1));
      }
    }
  }
  return $dropsuffix;
}


if (array_key_exists("SERVER_NAME", $_SERVER)) {
  $servername = $_SERVER["SERVER_NAME"];
  if ($servername === "build.eclipse.org") {
    // leave relative
    $dlprefix="";
  } else {
    // if not on build.elcipse.org, assume we are on downloads.
    // we "compute" based on matching /eclipse/downloads/*/$BUILD_ID  in the request URI.
    // we want the /eclipse/downloads/* part, such as
    // /eclipse/downloads/drops4, or
    // /eclipse/downloads/drops, or
    // function can return empty string
    $refurl=$_SERVER["REQUEST_URI"];
    // We expect $BUILD_ID to be defined in buildproperties.php
    // But it can be defined several times in reference URI, such as once in directory name,
    // and once in filename. We want the directory-like part.
    // And to complicate things, in S and R builds, the segment is no longer BUILD_ID,
    // but a more complicated concatination. And M builds even more complicated, since
    // there are two types, some are "RCs", and some not.

    $pos = strpos($BUILD_ID, "RC");
    if ($pos === false) {
      $isRC = false;
    } else {
      $isRC = true;
    }

    if ($BUILD_TYPE === "N" || $BUILD_TYPE === "I" || $BUILD_TYPE === "P" || $BUILD_TYPE === "X" || $BUILD_TYPE === "Y" || ($BUILD_TYPE === "M" && ! $isRC)) {
      $BUILD_DIR_NAME = $BUILD_ID;
    } else {
      if ($BUILD_TYPE === "R" || $BUILD_TYPE === "S" || $BUILD_TYPE === "P" || $BUILD_TYPE === "X" || $BUILD_TYPE === "Y" || ($BUILD_TYPE === "M" && $isRC)) {
        $timestamp = str_replace('-', '', $TIMESTAMP);
        $BUILD_DIR_NAME = $BUILD_TYPE."-".$BUILD_ID."-".$timestamp;
      } else {
        echo "Unexpected value of BUILD_TYPE: $BUILD_TYPE. <br />";
        // We'll make an assumption that might work.
        $BUILD_DIR_NAME = $BUILD_ID;
      }
    }
    $dlprefix=computeMirrorKey($refurl,$BUILD_DIR_NAME."/");
  }
}
else {
  // not sure what to put here (we are essentially not running on a host?)
  // we _might_ need to assume "downloads" here, for "convert to html to work?"
  // or, on build machine?
  $servername=localhost;
}

$script = $_SERVER['SCRIPT_NAME'];
$patharray = pathinfo($_SERVER['SCRIPT_NAME']);
$path = $patharray['dirname'];
$buildLabel = array_pop(split("/",$path,-1));
// this script should nearly always have a query string,
// but we check, to avoid warning when testing
if (array_key_exists("QUERY_STRING", $_SERVER)) {
  $qstring = $_SERVER['QUERY_STRING'];
  $dropFile=array_pop(split("=",$qstring,-1));
}


$mirror=true;
if (strstr($servername,"eclipse.org")) {
  $mirror=false;
  $eclipselink="http://www.eclipse.org/downloads/download.php?file="."$dlprefix/$buildLabel/$dropFile";
} else {
  $mirrorlink  = "http://$servername$path/$dropFile";
}

$clickFile = "clickThroughs/";
$clickFileName = str_replace("-$BUILD_ID","",$dropFile);
$clickFile = $clickFile.$clickFileName.".txt";

if (file_exists($clickFile)) {
  $fileHandle = fopen($clickFile, "r");
  while (!feof($fileHandle)) {
    $aLine = fgets($fileHandle, 4096);
    $result = $result.$aLine;
  }
  fclose($fileHandle);
} else {
  if ($mirror) {
    echo '<META HTTP-EQUIV="Refresh" CONTENT="0;URL='.$dropFile.'">';
    echo '<b><font size "+4">Downloading: '.$mirrorlink.'</font></b>';
  } else {
    echo '<META HTTP-EQUIV="Refresh" CONTENT="0;URL='.$eclipselink.'">';
    echo '<b><font size "+4">Downloading: '.$eclipselink.'</font></b>';
  }
  echo '<BR>';
  echo '<BR>';
  if ($mirror) {
    echo 'If your download does not begin automatically click <a href='.$dropFile.'>here</a>.';
  } else {
    echo 'If your download does not begin automatically click <a href='.$eclipselink.'>here</a>.';
  }
}
?>
</head>

<body bgcolor="#FFFFFF" text="#000000">
<?php
if (file_exists($clickFile)) {
  echo '<p><b><font size="+4">Important Notes<BR>';
  echo '</font></b></font></p>
    <p>It is very important to read the following notes in order to run this version
    of Eclipse. Once you have read the notes you can click on the Download link
    to download the drop.</p>
    ';
  echo '<textarea name="textfield" cols="80" rows="18" wrap="PHYSICAL">'.$result;
  echo '</textarea>';
  echo '<BR>';
  echo '<BR>';

  if ($mirror) {
    echo '<a href="'.$dropFile.'">Download</a>';
  } else {
    echo '<a href="'.$eclipselink.'">Download</a>';
  }

  echo "<!-- dropFile: $dropFile -->";
  echo "<!-- eclipselink: $eclipselink -->";
}
?>
</body>
</html>
