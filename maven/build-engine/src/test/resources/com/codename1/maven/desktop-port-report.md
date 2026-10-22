# Desktop port report

What it takes for this application to build and run as a Codename One application, from its compiled classes.

## Summary

- 6 application classes from 6 source files.
- 8 of 12 call sites into Swing, AWT and JavaFX are supported (66%).
- 3 source files have no finding; 3 have at least one.
- The compliance check fails: 10 references to 7 APIs a device does not have.

| In the application | Missing APIs | Call sites |
|---|---:|---:|
| Swing, AWT and JavaFX API the compatibility layers lack | 2 | 2 |
| JDK API a device lacks | 5 | 8 |

## What works unchanged

3 source files use only API that is available on a device:

- `com/acme/port/Clean.java`
- `com/acme/port/Main.java`
- `org/plain/Words.java`

## What to change

### `com/acme/port/Git.java`

- No equivalent: a device has no JMX.
  - lines 4, 7: `java.lang.management.ManagementFactory.getRuntimeMXBean()`
  - lines 4, 7: `java.lang.management.RuntimeMXBean.getName()`
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

### `org/fancy/FancyPanel.java`

- No equivalent: a device has no JMX.
  - line 3: `java.lang.management.ManagementFactory.getRuntimeMXBean()`

## What cannot run on a device

- `java.awt.Robot` (1 use in `com/acme/port/Tray.java`). No equivalent in an application: a device does not let one synthesize input or read the screen. In a test use com.codename1.testing.TestUtils.
- `java.lang.management.ManagementFactory.getRuntimeMXBean()`, `java.lang.management.RuntimeMXBean.getName()` (5 uses in `com/acme/port/Git.java`, `org/fancy/FancyPanel.java`). No equivalent: a device has no JMX.
- `java.sql.DriverManager.getConnection(java.lang.String)` (1 use in `com/acme/port/Git.java`). No JDBC on a device. Use the device's SQLite through Display.getInstance().openOrCreate(name), which answers a com.codename1.db.Database; a server database belongs behind a web service.
