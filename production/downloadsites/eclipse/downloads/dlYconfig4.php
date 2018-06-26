<?php
$dropPrefix = array();
$dropPrefix[]="Y";
$dropPrefix[]="P";
$dropType = array();
$dropType[]="4.9 Java 11 Beta Builds";
$dropType[]="Patch Builds";
// the "prefix" array and dropType array must be of same size, defined in right order
for ($i = 0; $i < count($dropType); $i++) {
    $typeToPrefix[$dropType[$i]] = $dropPrefix[$i];
}


