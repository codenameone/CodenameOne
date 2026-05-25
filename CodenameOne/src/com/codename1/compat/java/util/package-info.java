/// Compatibility shims for `java.util` APIs that are not available on every
/// Codename One target.
///
/// On platforms that lack the standard JDK class (notably older Android
/// runtimes via the build server), the Codename One build pipeline remaps
/// references from the standard package to the implementation here.
/// Application code should keep using the standard `java.util` types --
/// these classes exist only so the remap has a target to point at.
package com.codename1.compat.java.util;
