<html>
<body>

<?php
  echo "<h2>Scenarios: $QUERY_STRING*</h2>";

  $packageprefix=$QUERY_STRING;

  $aDirectory=dir("scenarios");
  $index = 0;

  while ($anEntry = $aDirectory->read()) {
    if ($anEntry != "." && $anEntry != "..") {
      if (strstr($anEntry,$packageprefix)){
        $line = "<a href=\"scenarios/$anEntry\">$anEntry</a><br>";
        echo "$line";
      }
    }
  }

$aDirectory->close();
?>

</body>
</html>

