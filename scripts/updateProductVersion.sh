#Before running this script configure the source and target POM versions correctly
cd ..
find `pwd` -name pom.xml -exec sed -i 's/4.11.0/4.12.0/g' {} \;
find `pwd` -name MANIFEST.MF -exec sed -i 's/4.11.0/4.12.0/g' {} \;
find `pwd` -name *.product -exec sed -i 's/4.11.0/4.12.0/g' {} \;
find `pwd` -name feature.xml -exec sed -i 's/4.11.0/4.12.0/g' {} \;
