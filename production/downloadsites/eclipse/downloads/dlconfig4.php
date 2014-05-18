<?php
$dropPrefix = array();
$dropPrefix[]="R";
$dropPrefix[]="S";
$dropPrefix[]="I";
$dropPrefix[]="M";
$dropPrefix[]="N";
$dropPrefix[]="P";
$dropType = array();
$dropType[]="Latest Release";
$dropType[]="4.4 Stable Build";
$dropType[]="4.4 Integration Build";
$dropType[]="4.3 Maintenance Build";
$dropType[]="4.4 Nightly Build";
$dropType[]="Patch Builds";
// the "prefix" array and dropType array must be of same size, defined in right order
for ($i = 0; $i < count($dropType); $i++) {
  $typeToPrefix[$dropType[$i]] = $dropPrefix[$i];
}

?>
