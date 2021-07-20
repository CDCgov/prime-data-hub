#!/usr/bin/env bash
IMG=${IMG:-prime-fe}
TAG=${TAG:-local}

pushd "$(dirname "${0}")" 1>/dev/null 2>&1

echo "Image: ${IMG?}:${TAG?}"
docker build -t "${IMG?}:${TAG?}" .
docker run -it -p 3000:3000 "${IMG?}:${TAG?}"

popd 1>/dev/null 2>&1m
