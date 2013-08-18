The files in this "downloadsites" directory are
not used during the build, but are releated. This files make up
the "main" download pages, that list each of the builds.

On the download server, they reside under
/home/data/httpd/download.eclipse.org/eclipse

The are stored here in this repository just to have a safe record
of them, make them easier to compare/change etc., but there is
nothing "automatic" about making them current or in synch with what
is on the downlaod server, so its recommended, before making changes,
to get a copy, manually compare with that's here, to see if things
have changed but not be put into the repository.

Something similar to following is a good way to get all the interesting files for /eclipse/downloadsites, 
while avoiding the large download drops. index.html and eclipse3x.html are the 
files created when new drops are uploaded, via "updateIndexes.sh" in sdk directory, 
so are excluded, since would often be "out of date" and 
should not be replaced on downloads (via rsync) but instead recreated.

rsync -aP --delete-excluded \
  --exclude '/TIME' --exclude '**/ztime/' --exclude '**/pde/' --exclude '**/equinox/' --exclude '**/eclipse.org-common/' \
  --exclude '**/e4/' --exclude '**/updates/' \
  --include '/index.html' --include '**/drops/index.html'  --include '**/drops4/index.html'  \
  --exclude '**/drops/**' --exclude '**/drops4/**' --exclude 'downloads/index.html' --exclude '**/downloads/eclipse3x.html' \
  build:/home/data/httpd/download.eclipse.org/eclipse/ . 
  
To upload whole directory, do not include --delete-excluded (Could likely not do with out any 
of the excludes/includes, if working with "clean" directory). 

rsync -aP \
  . build:/home/data/httpd/download.eclipse.org/eclipse/ 

Similar for equinox

rsync -aP --delete-excluded \
  --exclude '**/devicekit/' --exclude '**/drops/' --exclude '**/R-3.7.2-201202080800/' \
  --exclude '**/S-3.7RC4-201106030909/' --exclude '**/testweb/' --exclude '**/.*.swp/' \
  build:/home/data/httpd/download.eclipse.org/equinox/ .


