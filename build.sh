#!/bin/sh

cd eclipse.jdt.core
patch -N -p1 < ../apgas.compiler/R4_6.patch
cd ../apgas.parent
mvn -P build-individual-bundles package
cd ../eclipse.jdt.core
cd ..
cp apgas/apgas.zip .
cp apgas.site/target/site_assembly.zip apgas-update-site.zip
