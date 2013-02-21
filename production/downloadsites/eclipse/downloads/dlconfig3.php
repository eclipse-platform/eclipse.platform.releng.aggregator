<?php
$dropPrefix = array();
$dropPrefix[]="R";
$dropPrefix[]="S";
$dropPrefix[]="I";
$dropPrefix[]="M";
$dropPrefix[]="N";
$dropType = array();
$dropType[]="Latest Release";
$dropType[]="3.8 Stable Build";
$dropType[]="3.8 Integration Build";
$dropType[]="3.8 Maintenance Build";
$dropType[]="3.8 Nightly Build";
// the "prefix" array and dropType array must be of same size, defined in right order
for ($i = 0; $i < count($dropType); $i++) {
    $typeToPrefix[$dropType[$i]] = $dropPrefix[$i];
}

?>
