# Desktop port report

What it takes for this application to build and run as a Codename One application, from its compiled classes.

## Summary

- 3 application classes from 2 source files.
- 7 of 7 call sites into Swing, AWT and JavaFX are supported (100%).
- 2 source files have no finding; 0 have at least one.
- The compliance check passes: every API the application and its libraries use is available on a device.

## What works unchanged

2 source files use only API that is available on a device:

- `com/acme/port/Clean.java`
- `com/acme/port/Main.java`

## What to change

Nothing in the application's own sources.

## Dependencies

| Dependency | What it is | Classes shipped | Verdict |
|---|---|---:|---|
| fancy-lib-1.0.jar | UI library | 1 of 1 | Works: bundled whole and relocated with the application. |
| plain-lib-1.0.jar | pure Java | 1 of 1 | Works: the classes the application uses are relocated and pass the check. |
| unused-lib-1.0.jar | pure Java | 0 of 1 | Not used by the application's classes; left out. |

## What cannot run on a device

Nothing was found that has no counterpart on a device.
