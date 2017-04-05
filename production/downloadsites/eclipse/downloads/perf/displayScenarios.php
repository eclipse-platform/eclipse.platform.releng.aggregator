<html>
<body>

<?php
	$parts=explode("&","$QUERY_STRING");
	$buildType=trim($parts[0]);
	$packageprefix=trim($parts[1]);

	$aDirectory=dir("$buildType-scenarios");
	$index = 0;

	while ($anEntry = $aDirectory->read()) {

		if ($anEntry != "." && $anEntry != "..") {
			if (strstr($anEntry,$packageprefix) && strstr($anEntry,".html")){
				$scenarioname=substr($anEntry,0,-5);
				$scenarios[$index]=$scenarioname;
				$index++;

			}
		}
	}

	$scenarioCount=count($scenarios);
	if ($scenarioCount==0){
		echo "Results being generated.";
	}
	else{
	sort($scenarios);
	echo "<h2>$packageprefix* ($scenarioCount scenarios)</h2>";

	for ($counter=0;$counter<count($scenarios);$counter++){
		$line = "<a href=\"$buildType-scenarios/$scenarios[$counter].html\">$scenarios[$counter]</a><br>";
	 	echo "$line";
	}
	}
	aDirectory.closedir();
?>

</body>
</html>

