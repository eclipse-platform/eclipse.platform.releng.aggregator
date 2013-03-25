<?php

function generateDropSize($zipfile) {
		$filesize = getDropSize($zipfile);
		 return "<td>$filesize</td>";
}

function getDropSize($zipfile) {
		 $filesize = "N/A";
		 $filesizebytes  = filesize($zipfile);
		 if($filesizebytes > 0) {
			if($filesizebytes < 1048576)
				$filesize = round($filesizebytes / 1024, 0) . "K";
			else if ($filesizebytes >= 1048576 && $filesizebytes < 10485760)
				$filesize = round($filesizebytes / 1048576, 1) . "M";
			else
        		 	$filesize = round($filesizebytes / 1048576, 0) . "M";
		 }
		 return($filesize);
}

function generateChecksumLinks($zipfile, $buildlabel) {
     return "<td><a href=\"http://download.eclipse.org/equinox/drops/$buildlabel/checksum/$zipfile.md5\"><img src=\"/equinox/images/md5.png\" alt=\"md5\"/></a><a href=\"http://download.eclipse.org/equinox/drops/$buildlabel/checksum/$zipfile.sha1\"><img src=\"/equinox/images/sha1.png\" alt=\"sha1\"/></a></td>";
}
?>
