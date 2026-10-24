# Desktop port report

What it takes for this application to build and run as a Codename One application, from its compiled classes.

## Summary

- 4 application classes from 4 source files.
- 8 of 8 call sites into Swing, AWT and JavaFX are supported (100%).
- 4 source files have no finding; 0 have at least one.
- The compliance check passes: every API the application and its libraries use is available on a device.

## What works unchanged

4 source files use only API that is available on a device:

- `com/acme/port/Clean.java`
- `com/acme/port/Main.java`
- `org/fancy/FancyPanel.java`
- `org/plain/Words.java`

## What to change

Nothing in the application's own sources.

## What cannot run on a device

Nothing was found that has no counterpart on a device.
