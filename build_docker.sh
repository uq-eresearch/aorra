#!/bin/bash
set -e

rm -rf target/universal target/docker
activator dist
mkdir -p target/docker
unzip -d target/docker target/universal/*.zip
mv target/docker/aorra* target/docker/aorra

sudo docker build -t aorra .
