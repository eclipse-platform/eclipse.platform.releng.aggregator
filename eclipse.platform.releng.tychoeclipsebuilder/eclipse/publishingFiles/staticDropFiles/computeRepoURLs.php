<?php

// This snippet must some before the "repository URLs" as is it used to
// "compute" the proper URLs for the various streams and types of builds.

// It requires, for following variables to have already been defined in outter script:
// $BUILD_ID, $STREAM_MAJOR, $STREAM_MINOR, $BUILD_TYPE, $TIMESTAMP

// It "outputs" two values:
// $STREAM_REPO_URL
// $$BUILD_REPO_URL

// "RCs" have complicated rules, but in short:
//   RC's for an initial release (in June) go from 4.x-M-build repos to 4xmilestone repos
//   RC's for SRs "stay" in the 4.x-M-build repo. And, no easy way to tell if service release,
//   from the build id alone. The hard way is to part the first "digit groups" of build id, and,
//   if service field is 0 (or does not exist) then it is not a service release.


// checking for existence is especially important for BUILD_REPOs, since they might have been removed.
// but we do it also for sanity check.
// relative patch can be either 3 or 4 "up", depending on if on downloads, or build machine.
// either can be used to "prove existence".
// We define them here, but used in "main", outter script.
$relativePath4="../../../..";
$relativePath3="../../..";


function isSRRC ( $buildid ) {
  $SRPATTERN="/([0-9]+)\\.([0-9]+)\\.([1-9]+)RC[1-4]/";
  return (preg_match($SRPATTERN, $buildid));
}

function isRRC ( $buildid ) {
  $RPATTERN="/([0-9]+)\\.([0-9]+)((\\.0)?)RC[1-4]/";
  return (preg_match($RPATTERN, $buildid));
}

function computeSTREAM_REPO_NAME () {
  global $BUILD_ID, $BUILD_TYPE, $STREAM_MAJOR, $STREAM_MINOR;
    // We expect $BUILD_ID to be defined in buildproperties.php
    // But it can be defined several times in reference URI, such as once in directory name,
    // and once in filename. We want the directory-like part.
    // And to complicate things, in S and R builds, the segment is no longer BUILD_ID,
    // but a more complicated concatenation.
    if ($BUILD_TYPE === "M" && isSRRC($BUILD_ID)) {
      $STREAM_REPO_NAME=$STREAM_MAJOR.".".$STREAM_MINOR."-"."$BUILD_TYPE"."-builds";
    } else {
      if ($BUILD_TYPE === "M" && isRRC($BUILD_ID)) {
        $STREAM_REPO_NAME=$STREAM_MAJOR.".".$STREAM_MINOR."milestones";
      } else {
        if ($BUILD_TYPE === "N" || $BUILD_TYPE === "I" || $BUILD_TYPE === "M" || $BUILD_TYPE === "P" || $BUILD_TYPE === "X" || $BUILD_TYPE === "Y") {
          $STREAM_REPO_NAME=$STREAM_MAJOR.".".$STREAM_MINOR."-".$BUILD_TYPE."-"."builds";
        } else {
          if ($BUILD_TYPE === "S") {
            $STREAM_REPO_NAME=$STREAM_MAJOR.".".$STREAM_MINOR."milestones";
          } else {
            if ($BUILD_TYPE === "R") {
              $STREAM_REPO_NAME=$STREAM_MAJOR.".".$STREAM_MINOR;
            }
            else {
              echo "Unexpected value of BUILD_TYPE: $BUILD_TYPE. <br />\n";
              // We will make an assumption that might work.
              $STREAM_REPO_NAME=$STREAM_MAJOR.".".$STREAM_MINOR."-".$BUILD_TYPE."-"."builds";
            }
          }
        }
      }
    }
  return $STREAM_REPO_NAME;
}

function computeSTREAM_REPO_URL () {
  $STREAM_REPO_URL="http://download.eclipse.org/eclipse/updates/".computeSTREAM_REPO_NAME ();
  return $STREAM_REPO_URL;
}

function computeBUILD_REPO_URL () {
  global $BUILD_ID, $BUILD_TYPE, $TIMESTAMP ; 
  // There are two types of M builds, some RCs, some not.
  if ($BUILD_TYPE === "N" || $BUILD_TYPE === "I" || $BUILD_TYPE === "P" || $BUILD_TYPE === "X" || $BUILD_TYPE === "Y" || $BUILD_TYPE === "M" && ! ((isSRRC($BUILD_ID)) || isRRC($BUILD_ID))) {
    $BUILD_REPO_NAME=computeSTREAM_REPO_NAME ()."/".$BUILD_ID;
  } else {
    $timestamp = str_replace('-', '', $TIMESTAMP);
    if ($BUILD_TYPE === "S" || $BUILD_TYPE === "R" || ($BUILD_TYPE === "M" && (isSRRC($BUILD_ID) || isRRC($BUILD_ID)))) {
      $BUILD_REPO_NAME=computeSTREAM_REPO_NAME ()."/".$BUILD_TYPE."-".$BUILD_ID."-"."$timestamp";
    } else {
      echo "Unexpected value of BUILD_TYPE: $BUILD_TYPE. <br />\n";
      // We will make an assumption that might work.
      $BUILD_REPO_NAME=computeSTREAM_REPO_NAME ()."/".$BUILD_ID;
    }
  }

  $BUILD_REPO_URL="http://download.eclipse.org/eclipse/updates/".$BUILD_REPO_NAME;
  return $BUILD_REPO_URL;
}

