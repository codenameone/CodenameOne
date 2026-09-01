# Device runtime probes

Programs to push at a device runtime to find out what it cannot do yet.

Each one is small, prints a single `PROBE <name>: ...` line whose contents are
checkable by eye, and shows a form so the device is visibly doing something.
`notes-app/` and `resource-app/` are not probes but ordinary applications --
the first is four files in three packages entered through a `Lifecycle` rather
than a `main`, the second ships its own `.res` and a plain file alongside its
source. The shape of a real application is itself a thing that has to work.

Run them against a device runtime with the app already installed:

```bash
scripts/run-device-runtime-android.sh                 # build, install, launch
for p in scripts/devruntime-probes/*.java; do
    scripts/cn1-push.sh "$p" 18234
done
scripts/cn1-push.sh scripts/devruntime-probes/notes-app 18234

scripts/run-device-runtime-ios.sh                     # same, for the simulator
scripts/run-device-runtime-ios.sh --skip-build scripts/devruntime-probes/EnumProbe.java
```

`NetProbe` needs an endpoint on the host, reached over loopback (`adb reverse
tcp:18080 tcp:18080` on Android; the simulator shares the host's loopback):

```bash
mkdir -p /tmp/www && echo hello-from-host > /tmp/www/hello.txt
(cd /tmp/www && python3 -m http.server 18080)
```

Two probes fail on purpose: `TraceProbe` throws, to show that the reported
stack names your source and your line numbers, and `LoopProbe` spins forever,
to show that the watchdog stops it rather than freezing the app.

## Why these exist

Every one of them was written because something plausible turned out not to
work, and each covers a defect that was invisible to everything written before
it. They are kept because that is the pattern: what breaks a device runtime is
never the thing being tested at the time.

| Probe | The defect it was written for |
|---|---|
| `LambdaProbe` | `invokedynamic` was rejected outright -- no lambdas or method references |
| `EnumProbe` | `java.lang.Enum` cannot be shimmed, so enums did not run at all |
| `CollectionProbe` | iOS dispatched on the call site's declared type, so `List.add` reached `AbstractList.add`, which throws. Probes that declared `ArrayList` could not see it |
| `LateCallbackProbe` | the event-thread budget was measured per session, so every callback later than two seconds failed having run nothing |
| `NativeProbe` | a `cn1lib`'s native half has to degrade to `isSupported() == false`, not fail |
| `TraceProbe` | a host exception thrown by pushed code carried the interpreter's stack, not the program's |
| `NetProbe` | the default network-error handler answers a failure with another blocking request, and wedges the event thread |
| `SyncProbe` | `monitorenter`/`monitorexit` were accepted and ignored, so `synchronized` guaranteed nothing |
| `WaitNotifyProbe` | why the monitors are the objects' own: a `ReentrantLock` per object gives mutual exclusion and nothing else, and `wait()` would throw `IllegalMonitorStateException` |
| `resource-app` | a pushed program had no way to bring its own `theme.res`, so it wore the host's design |
| `notes-app` | `ireturn` was boxed as `Integer`, so every `boolean` method crashed; peers were never mapped back to their interpreted objects, so `Collections.sort` failed; shims did not override `Object`'s methods, so model objects printed as `Interp_I_java_lang_Comparable@df828bb` |
