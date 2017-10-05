#!/bin/bash
set -e

source ./build_docker.sh
sudo docker save aorra > aorra.docker
docker2aci aorra.docker
mv library-aorra-latest.aci aorra.aci
