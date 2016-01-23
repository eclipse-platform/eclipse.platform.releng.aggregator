<?php

function column($servername) {
      echo "<td width=\"7%\"><b>Download</b></td>";
      echo "<td width=\"7%\"><b>Size</b></td>";
      echo "<td width=\"40%\"><b>File</b></td>";
      echo "<td width=\"11%\"><b>Checksum</b></td>";
      return;
}

function generateDropSize($zipfile) {
		$filesize = getDropSize($zipfile);
		 echo "<td>$filesize</td>";
}

function getDropSize($zipfile) {
		 $filesize = "N/A";
		 $filesizebytes  = filesize($zipfile);
		 if($filesizebytes > 0) {
			if($filesizebytes < 1048576)
				$filesize = round($filesizebytes / 1048576, 2) . " MB";
			else if ($filesizebytes >= 1048576 && $filesizebytes < 10485760)
				$filesize = round($filesizebytes / 1048576, 1) . " MB";
			else
        		 	$filesize = round($filesizebytes / 1048576, 0) . " MB";
		 }
		 return($filesize);
}

function generateChecksumLinks($zipfile) {
     echo "<td><a href=\"http://download.eclipse.org/equinox/drops/@buildlabel@/checksum/$zipfile.md5\"><img src=\"md5.png\" alt=\"md5\"/></a>";
     echo "<a href=\"http://download.eclipse.org/equinox/drops/@buildlabel@/checksum/$zipfile.sha1\"><img src=\"sha1.png\" alt=\"sha1\"/></a></td>";
}

function genLinks($servername,$buildlabel,$zipfile) {
     $httplink = "download.php?dropFile=$zipfile";
     $httplabel = "(http)";
     $httpline =  "<div align=\"left\"><a href=\"$httplink\">$httplabel</a>";
     echo "$httpline";
     echo "&nbsp;&nbsp";
     generateDropSize($zipfile);
     echo "<td>$zipfile</td>";
     echo "<td><a href=\"http://download.eclipse.org/equinox/drops/@buildlabel@/checksum/$zipfile.md5\">(md5)</a>";
     echo " <a href=\"http://download.eclipse.org/equinox/drops/@buildlabel@/checksum/$zipfile.sha1\">(sha1)</a></td>";
}
?>
