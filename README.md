# AORRA

[![Build Status](https://travis-ci.org/uq-eresearch/aorra.png?branch=master)](https://travis-ci.org/uq-eresearch/aorra)


AORRA is an application to assist creating reef health report cards for the Great Barrier Reef.

## Building

AORRA is a [Play Framework][play] 2.2.3 application. The easiest way to build AORRA is with [Typesafe Activator][activator]. As AORRA does not use the latest bundled library versions, it's perfectly fine to use the mini-package with no bundled dependencies.

Once you have `activator` on your path, enter the directory containing the AORRA source and run:

```shell
# Fetch dependencies (which will take quite some time) and run tests
activator test
# Build distributable archive
activator dist
```

Alternately you can run AORRA in place with `activator run`.

### Container images

The scripts `build_docker.sh` and `build_aci.sh` can be used to build a container images for AORRA. You will need Docker installed for both, and [docker2aci][docker2aci] for the latter.

## Running

AORRA runs on port 9000 by default, and exposes a local admin console over telnet on port 5000.

Ensure the `SESSION_SECRET` environment variable is set in order to properly protect cookie-based sessions.


[play]: https://playframework.com/
[activator]: https://typesafe.com/activator
[docker2aci]: https://github.com/appc/docker2aci
