# Matter bridge compile stubs

Minimal declarations of the Android and Play services types
`MatterCommissioningBridge.javas` uses, so `MatterCommissioningBridgeCompilesTest`
can compile that injected source without an Android SDK on the classpath.

They carry no copyright header and are named `.javas` rather than `.java` for
the same reason: they are not Codename One source. Each one mimics the shape of
somebody else's published API -- a method signature and a return of the right
type, nothing more -- and stamping our licence header on a facsimile of
`android.app.Activity` would be a claim we have no business making. The
extension also keeps them out of every gate that walks Java sources, none of
which has anything useful to say about a four-line stub.

They declare only what the bridge actually uses. A member that goes missing is
a compile error naming it, which is the right outcome: it means the bridge
started using something new, and the stub has to say so.
