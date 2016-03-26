<?php

// This file is "static" for now, but eventually may compute and upload
// this file more dynamically.
// BUT, for the moment, the order is important. We expect the order here
// to be the same order as displayed on test results summary page. (And,
// while not sure how that can be controlled, eventually, in either case
// we'd probably want a consistent ordering.

$expectedTestConfigs = array();
$expectedTestConfigs[]="ep46${BUILD_TYPE}-unit-lin64_linux.gtk.x86_64_8.0";
$expectedTestConfigs[]="ep46${BUILD_TYPE}-unit-mac64_macosx.cocoa.x86_64_8.0";
$expectedTestConfigs[]="ep46${BUILD_TYPE}-unit-win32_win32.win32.x86_8.0";
$expectedTestConfigs[]="ep46${BUILD_TYPE}-unit-cen64_linux.gtk.x86_64_8.0";
