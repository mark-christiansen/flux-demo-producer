#!/bin/bash
IMAGE="machrist/flux-demo-producer"
VERSION="0.0.1"
docker build . -t $IMAGE:$VERSION
kind load docker-image $IMAGE:$VERSION