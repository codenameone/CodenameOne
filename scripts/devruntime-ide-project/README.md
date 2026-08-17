# Run a Codename One app on your phone, from your IDE

Open this project, edit `MyApp.java`, run the push. The app appears on a phone
that is already holding the runtime, in seconds. Nothing is compiled for the
phone and nothing is installed: your classes are bundled and interpreted by the
runtime app.

## 1. Install the runtime on the phone

```
~/cn1-device-runtime.apk
```

Copy it to the phone and open it; Android will ask you to allow installs from
this source. It is debug-signed, which is what makes it installable without a Play listing
and also what makes it unfit for anything but development.

It is 11MB and contains no native libraries at all, so it runs on any phone
regardless of architecture. It is built from `scripts/cn1-device-runtime/`,
whose only application code is the runtime itself.

## 2. Nothing to configure

Put the phone on the same network as your machine and open the app. There is no
address to type and no port to set.

The phone looks for you: the computer it last spoke to, then loopback (which is
a USB session, where `adb reverse tcp:18234 tcp:18234` maps your machine onto
the phone's own address), then every address on its own subnet. The push tool
answers with a frame that identifies itself, so the search and the push are the
same connection.

There is no UDP in the Codename One API and therefore no broadcast to announce
with, which is why this is a sweep rather than the discovery protocol you might
expect. It costs one TCP attempt per address, in batches, and the address that
answers is remembered -- so it happens once, not every couple of seconds.

**Look for my computer** on the runtime screen forgets that address, which is
what you want after moving to a different network.

## 3. Push

```bash
mvn -Ppush package        # USB
mvn -Ppush-lan package    # Wi-Fi
```

In an IDE, add those as run configurations, or right-click the profile in the
Maven panel. Every run recompiles, rebundles and pushes, so the loop is: edit,
run, look at the phone.

Over Wi-Fi the first push pairs. The terminal shows a six-digit code and the
phone asks for it. The phone stores your computer only if the code matches, and
every later connection is still approved on the phone unless you choose
*Always*. **Forget paired computers** on the runtime screen undoes it.

Pairing is not optional off loopback, and the runtime enforces that rather than
trusting the caller: over USB the connection can only come from a machine you
have authorised with a cable, while on a network anything can answer, and the
bundle carries your program's whole source.

## What you can write

Ordinary Codename One. The entry point is a `Lifecycle` subclass, as in any
application -- `MyApp` is one -- and a `main(String[])` works too if you would
rather have one. Lambdas, method references, enums, inner classes, generics,
collections, threads, `synchronized`, networking, `Storage`, `Preferences`, and
subclassing framework classes such as `Form` all work.

Put `theme.res`, CSS and images under `src/main/resources`; they travel with the
bundle and the framework loads them the usual way, so your app wears its own
design rather than the runtime host's.

## What you cannot

- **Native code.** A `cn1lib`'s Java half is interpreted like the rest of your
  program; its native half reports `isSupported() == false` rather than failing.
- **The app's identity.** Bundle id, icon, permissions, push certificates and
  URL schemes belong to the runtime app and are fixed at its build.
- **Performance conclusions.** Your code is interpreted and the runtime app is
  built with the optimizer off. Tight loops and per-pixel work in `paint` say
  nothing about a real build.
- **A program that never yields.** The event thread has a budget; a runaway loop
  is stopped and reported rather than freezing the phone.

## When something fails

The phone reports back through the terminal, with your file names and line
numbers rather than the interpreter's:

```
FAILED: java.lang.IllegalStateException: from depth 0
	at TraceProbe.deep(TraceProbe.java:3)
	at TraceProbe.main(TraceProbe.java:5)
```

If a framework method threw, the report names it: `(thrown by
java.util.List.add(Ljava/lang/Object;)Z)`.

*"the device never connected"* means the phone is not dialling this machine:
either the app is not running, or `adb reverse` is not set (USB), or the address
under **Desktop** is not this computer (Wi-Fi).
