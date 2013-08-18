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

Something similar to following is a good way to get all the interesting files, while avoiding the large download drops

rsync -aP --delete-excluded  --include '**/drops/index.html'  --include '**/drops4/index.html' --exclude '**/drops/**' --exclude '**/drops4/**'  build:/home/data/httpd/download.eclipse.org/eclipse/downloads/ .