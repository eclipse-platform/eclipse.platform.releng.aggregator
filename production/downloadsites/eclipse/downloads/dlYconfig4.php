<?php
$dropPrefix = array();
$dropPrefix[]="Y";
$dropPrefix[]="U";
$dropPrefix[]="P";
$dropType = array();
$dropType[]="4.7 Java 9 Beta Builds";
$dropType[]="4.7 JUnit 5 Beta Builds";
$dropType[]="Patch Builds";
// the "prefix" array and dropType array must be of same size, defined in right order
for ($i = 0; $i < count($dropType); $i++) {
    $typeToPrefix[$dropType[$i]] = $dropPrefix[$i];
}


