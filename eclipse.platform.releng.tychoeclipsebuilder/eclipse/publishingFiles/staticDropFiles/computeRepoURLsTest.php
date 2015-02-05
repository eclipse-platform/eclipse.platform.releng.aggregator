<?

include "computeRepoURLs.php";

function displayresults() {
global $BUILD_ID, $BUILD_TYPE, $STREAM_MAJOR, $STREAM_MINOR, $TIMESTAMP;
echo "BUILD_ID:  $BUILD_ID\n";
echo "BUILD_TYPE:  $BUILD_TYPE\n";
echo "STREAM_MAJOR:  $STREAM_MAJOR\n";
echo "STREAM_MINOR:  $STREAM_MINOR\n";

echo "STREAM_REPO_URL:  ".computeSTREAM_REPO_URL() ."\n";
echo "BUILD_REPO_URL:  ".computeBUILD_REPO_URL()."\n";
}


$BUILD_ID="4.4.1RC3";
$BUILD_TYPE="M";
$STREAM_MAJOR=4;
$STREAM_MINOR=4;
$TIMESTAMP="20150202-1200";

displayResults();

$BUILD_ID="4.4RC3";

displayResults();

$BUILD_ID="M20150202-0800";
displayResults();