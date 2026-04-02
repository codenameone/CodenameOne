# JavaScript Screenshot Baselines

This directory is reserved for JavaScript-port CN1SS screenshot baselines.

The intent is to reuse the same `scripts/hellocodenameone` screenshot suite
that currently feeds the iOS and Android runners, but compare the browser
runtime output against JavaScript-specific reference PNGs here once the
browser-driven execution harness is in place.

Current status:

- The decoding/comparison entrypoint exists in
  `/Users/shai/dev/cn1/scripts/run-javascript-screenshot-tests.sh`
- Baseline PNGs have not been checked in yet
- The browser execution half of the JavaScript screenshot pipeline is still in
  progress
