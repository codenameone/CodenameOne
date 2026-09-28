# Wear surface stubs

Minimal stand-ins for the Android, AndroidX Wear and Guava types the two injected Wear surface
services use, so `WearGlueCompilesTest` can compile them without an Android SDK or the
`androidx.wear` artifacts.

They deliberately declare only what the services actually touch. A member that goes missing is a
compile error naming it, which is the right outcome: it means a service started using something
new and this tree has to say so. That makes the tree an executable record of exactly how much of
the AndroidX Wear API surface Codename One depends on.

Files carry the `.javas` extension so this module's own compilation ignores them; the test copies
and renames them.
