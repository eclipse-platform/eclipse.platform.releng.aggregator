#!/usr/bin/env bash

source ./syncUpdateUtils.shsource

function testConvert ()
{
  testcase=$1
  testnum=$2
  num=$(convertToZeroPaddedMillisecs $testnum)
  printf "Case %2d. In: %s \t Out: %s\n" $testcase $testnum $num
}

testConvert 1 "11.65"
testConvert 2 "011.6"
testConvert 3 "11.650"
testConvert 4 "00011.650"
testConvert 5 "011.6503333"
testConvert 6 "011.6506666"
testConvert 7 ".650"
testConvert 8 "00.650"
testConvert 9 "00011"
testConvert 10 "11.650.3333"
testConvert 11 "1.1.6503333"
testConvert 12 ".11.6503333"
testConvert 13 "1.1.6503333"
testConvert 14 "19778728"
