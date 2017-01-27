<?php

// This file is "static" for now, and, at first, before any tests processed. 
// Eventually will be overwritten by "generate indexes" tools. (In theory, 
// The order might change. It is intended a s place holder so that pages look 
// more consistent. May change in future -- say, to have a simple "pending" message.
// Best to have a consistent order, which is currently alphabetical.
// See https://bugs.eclipse.org/bugs/show_bug.cgi?id=490624

include("buildproperties.php");

$expectedTestConfigs = array();
$expectedTestConfigs[]="ep46$TESTED_BUILD_TYPE-unit-cen64-gtk3_linux.gtk.x86_64_8.0";
$expectedTestConfigs[]="ep46$TESTED_BUILD_TYPE-unit-cen64-gtk2_linux.gtk.x86_64_8.0";
$expectedTestConfigs[]="ep46$TESTED_BUILD_TYPE-unit-mac64_macosx.cocoa.x86_64_8.0";
$expectedTestConfigs[]="ep46$TESTED_BUILD_TYPE-unit-win32_win32.win32.x86_8.0";

