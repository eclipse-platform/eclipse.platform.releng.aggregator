<?php
$dropPrefix = array();
$dropPrefix[]="R";
$dropPrefix[]="S";
$dropPrefix[]="I";
$dropPrefix[]="M";
$dropPrefix[]="Y";
$dropPrefix[]="N";
$dropPrefix[]="P";
$dropType = array();
$dropType[]="Latest Release";
$dropType[]="4.7 Stable Builds";
$dropType[]="4.7 Integration Builds";
$dropType[]="4.6 Maintenance Builds";
$dropType[]="4.7 Java 9 Beta Builds";
$dropType[]="4.7 Nightly Builds";
$dropType[]="Patch Builds";
// the "prefix" array and dropType array must be of same size, defined in right order
for ($i = 0; $i < count($dropType); $i++) {
    $typeToPrefix[$dropType[$i]] = $dropPrefix[$i];
}


