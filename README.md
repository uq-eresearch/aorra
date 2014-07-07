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



[play]: https://playframework.com/
[activator]: https://typesafe.com/activator
