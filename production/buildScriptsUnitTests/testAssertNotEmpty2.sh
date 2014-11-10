#!/usr/bin/env bash

source bashUtilities.shsource

T1="some value"
assertNotEmpty T1

echo "Should see this line"

function testDiagMsg
{
  assertNotEmpty T2
  echo "Should NOT see this line"
}

testDiagMsg

