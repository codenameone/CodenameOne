# Desktop port report

What it takes for this application to build and run as a Codename One application, from its compiled classes.

## Summary

- 5 application classes from 4 source files.
- 7 of 11 call sites into Swing, AWT and JavaFX are supported (63%).
- 2 source files have no finding; 2 have at least one.
- The compliance check fails: 13 references to 8 APIs a device does not have (11 in the application, 2 in its libraries).

| In the application | Missing APIs | Call sites |
|---|---:|---:|
| Swing, AWT and JavaFX API the compatibility layers lack | 2 | 2 |
| JDK API a device lacks | 6 | 9 |

## What works unchanged

2 source files use only API that is available on a device:

- `com/acme/port/Clean.java`
- `com/acme/port/Main.java`

## What to change

### `com/acme/port/Git.java`

- A device cannot start a process. To open a URL, a file or another application use CN.execute(url), after CN.canExecute(url); anything else belongs behind an interface with a desktop-only implementation.
  - lines 4, 7: `java.lang.ProcessBuilder`
  - lines 4, 7: `java.lang.ProcessBuilder.start()`
  - lines 4, 7: `new java.lang.ProcessBuilder(java.lang.String[])`
- Use com.codename1.io.Socket.connect(host, port, SocketConnection), or WebSocket for a message stream to a server.
  - line 13: `java.net.Socket`
  - line 13: `new java.net.Socket(java.lang.String, int)`
- No JDBC on a device. Use the device's SQLite through Display.getInstance().openOrCreate(name), which answers a com.codename1.db.Database; a server database belongs behind a web service.
  - line 10: `java.sql.DriverManager.getConnection(java.lang.String)`

### `com/acme/port/Tray.java`

- No equivalent in an application: a device does not let one synthesize input or read the screen. In a test use com.codename1.testing.TestUtils.
  - line 5: `java.awt.Robot`
- A device has no tray. What a tray icon announces is a LocalNotification (Display.scheduleLocalNotification).
  - line 4: `java.awt.SystemTray`

## Dependencies

| Dependency | What it is | Classes shipped | Verdict |
|---|---|---:|---|
| fancy-lib-1.0.jar | UI library | 1 of 1 | 1 of 1 shipped classes use API a device lacks: processes (2). Needs a device replacement, or an interface with a device implementation. |
| plain-lib-1.0.jar | pure Java | 1 of 1 | Works: the classes the application uses are relocated and pass the check. |
| unused-lib-1.0.jar | pure Java | 0 of 1 | Not used by the application's classes; left out. |

## What cannot run on a device

- `java.awt.Robot` (1 use in `com/acme/port/Tray.java`). No equivalent in an application: a device does not let one synthesize input or read the screen. In a test use com.codename1.testing.TestUtils.
- `java.lang.ProcessBuilder`, `java.lang.ProcessBuilder.start()`, `new java.lang.ProcessBuilder(java.lang.String[])` (6 uses in `com/acme/port/Git.java`). A device cannot start a process. To open a URL, a file or another application use CN.execute(url), after CN.canExecute(url); anything else belongs behind an interface with a desktop-only implementation.
- `java.sql.DriverManager.getConnection(java.lang.String)` (1 use in `com/acme/port/Git.java`). No JDBC on a device. Use the device's SQLite through Display.getInstance().openOrCreate(name), which answers a com.codename1.db.Database; a server database belongs behind a web service.
