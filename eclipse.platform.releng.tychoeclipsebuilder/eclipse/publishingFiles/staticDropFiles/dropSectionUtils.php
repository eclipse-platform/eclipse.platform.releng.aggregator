<?php

  function startTable() {
    echo "<table class=\"dropSection table table-striped table-hover table-condensed\">";
    return;
  }

  // The 'col-md-n' classes refer to Bootstraps grid system, and must total 12.
  // See http://www.w3schools.com/bootstrap/bootstrap_grid_system.asp  
  // and especially http://donnapeplinskie.com/using-tables-with-bootstrap-grids/
 function columnHeads() {
    echo "<th class=\"col-md-3\">Platform</th>";
    echo "<th class=\"col-md-5\">Download</th>";
    echo "<th class=\"col-md-2\">Size</th>";
    echo "<th class=\"col-md-2\">Checksum</th>";
    return;
  }

  function getDropSize($zipfile) {

    $filesize = "N/A";
    $filesizebytes  = filesize($zipfile);
    if($filesizebytes > 0) {
      if($filesizebytes < 1048576) {
        $filesize = round($filesizebytes / 1048576, 2) . " MB";
      } elseif ($filesizebytes >= 1048576 && $filesizebytes < 10485760) {
        $filesize = round($filesizebytes / 1048576, 1) . " MB";
      } else {
        $filesize = round($filesizebytes / 1048576, 0) . " MB";
      }
    }
    return($filesize);
  }

  // This function is generated "in line" by the "index" custom ant task.
  // Hence, must "stay coordinated" with what is there.
  function genLinks($zipfile) {

    global $clickthroughstr;
    global $BUILD_DIR_SEG;

    $filetarget = "$clickthroughstr$zipfile";
    $filelink =  "<a style=\"align:left\" href=\"$filetarget\">$zipfile</a>";
    $filesize = getDropSize($zipfile);

    echo "<td>$filelink</td>\n";
    echo "<td>$filesize</td>\n";
    // TODO: investgate using https:// to Eclipse Foundation's database of checksums
    // TODO: can make some future variables so it has one value on "build" server, and another value on
    // downloads, such as in buildproperties.php (value would get changed in "promtoe" script).
    // Also handy/needed when doing "local test builds".
    // Remember, md5 and sha1 are still in "checksum" directory, for at least initial Luna release, just not linked to. See bug
    // https://bugs.eclipse.org/bugs/show_bug.cgi?id=423714
    echo "<td><a href=\"http://download.eclipse.org/eclipse/downloads/drops4/$BUILD_DIR_SEG/checksum/$zipfile.sha512\">[SHA512]</a></td>";
  }

