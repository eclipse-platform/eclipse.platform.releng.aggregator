
# Maven needs a local cache to store its files.
# If you use Maven for other projects, you probably don't want to pollute the default repository
# Use this variable to tell the build script where to create the Eclipse CBI Maven cache
m2repo="$BASE/TMP/m2repo"

# If you use your own Maven proxy, the build will probably fail.
# Copy your settings.xml to this location to override the defaults.
m2settings="$BASE/settings.xml"
