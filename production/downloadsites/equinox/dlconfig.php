<?php
$dropPrefix = array();
$dropPrefix[]="R";
$dropPrefix[]="S";
$dropPrefix[]="I";
// patch builds not expected in equinox
// $dropPrefix[]="P";
$dropType = array();
$dropType[]="Latest Release";
$dropType[]="Stable Builds";
$dropType[]="Integration Builds";
// patch builds not expected in equinox
// $dropType[]="Patch Builds";

// the "prefix" array and dropType array must be of same size, defined in right order
for ($i = 0; $i < count($dropType); $i++) {
    $typeToPrefix[$dropType[$i]] = $dropPrefix[$i];
}
// intentionally left unclosed, since 'included' in PHP section

