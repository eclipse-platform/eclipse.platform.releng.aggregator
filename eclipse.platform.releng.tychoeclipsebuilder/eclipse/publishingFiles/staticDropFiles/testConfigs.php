<?php

// This file is "static" for now, and, at first, before any tests processed. 
// Eventually will be overwritten by "generate indexes" tools. (In theory, 
// The order might change. It is intended a s place holder so that pages look 
// more consistent. May change in future -- say, to have a simple "pending" message.
// Best to have a consistent order, which is currently alphabetical.
// See https://bugs.eclipse.org/bugs/show_bug.cgi?id=490624

include("buildproperties.php");

$expectedTestConfigs = array();
$expectedTestConfigs[]="ep$STREAMMajor$STREAMMinor$TESTED_BUILD_TYPE-unit-mac64-java17_macosx.cocoa.x86_64_17";
$expectedTestConfigs[]="ep$STREAMMajor$STREAMMinor$TESTED_BUILD_TYPE-unit-macM1-java17_macosx.cocoa.aarch64_17";
$expectedTestConfigs[]="ep$STREAMMajor$STREAMMinor$TESTED_BUILD_TYPE-unit-win32-java11_win32.win32.x86_64_11";
$expectedTestConfigs[]="ep$STREAMMajor$STREAMMinor$TESTED_BUILD_TYPE-unit-cen64-gtk3-java11_linux.gtk.x86_64_11";
$expectedTestConfigs[]="ep$STREAMMajor$STREAMMinor$TESTED_BUILD_TYPE-unit-cen64-gtk3-java17_linux.gtk.x86_64_17";
$expectedTestConfigs[]="ep$STREAMMajor$STREAMMinor$TESTED_BUILD_TYPE-unit-cen64-gtk3-java18_linux.gtk.x86_64_18";
