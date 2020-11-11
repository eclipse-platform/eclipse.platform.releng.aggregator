<?php
$dropPrefix = array();
$dropPrefix[]="R";
$dropPrefix[]="S";
$dropPrefix[]="I";
$dropPrefix[]="Y";
$dropPrefix[]="P";
$dropPrefix[]="X";
$dropType = array();
$dropType[]="Latest Release";
$dropType[]="Stable Builds";
$dropType[]="Integration Builds";
$dropType[]="Java15 branch Builds";
$dropType[]="Java15 patch Builds";
$dropType[]="Experimental Builds built on JIRO";
// the "prefix" array and dropType array must be of same size, defined in right order
for ($i = 0; $i < count($dropType); $i++) {
    $typeToPrefix[$dropType[$i]] = $dropPrefix[$i];
}


