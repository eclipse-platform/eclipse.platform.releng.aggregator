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
           // /eclipse/downloads/drops4pdebased, etc.
           // function can return empty string
           $refurl=$_SERVER["REQUEST_URI"];
           // We expect $BUILD_ID to be defined in buildproperties.php
           $dlprefix=computeMirrorKey($refurl,$BUILD_ID);
        }
    }
    else {
        // not sure what to put here (we are essentially not running on a host?)
        // we _might_ need to assume "downloads" here, for "convert to html to work?"
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

		if ($qstring) {
		    $url = "http://$servername$script?$qstring";
		} else {
		    $url = "http://$servername$path$script";
		}

        $mirror=true;
        if (strstr($servername,"eclipse.org")) {
#       if (strstr($servername,"ibm.com")) {
        	$mirror=false;
        	$eclipselink="$dlprefix/$buildLabel/$dropFile";
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
	 }
?>
</body>
</html>
