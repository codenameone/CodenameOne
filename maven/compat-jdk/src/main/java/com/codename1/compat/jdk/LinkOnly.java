package com.codename1.compat.jdk;

/// Marks a class of a compatibility layer that exists so that a library
/// naming it LINKS, and that does nothing useful when it is reached.
///
/// A third-party library written against a desktop toolkit is bundled with
/// the application whole, and almost every one of them has corners no device
/// can serve: a layout manager that can save itself through
/// `java.beans.XMLEncoder`, which is reflection from end to end, or that asks
/// whether a component is one of AWT's heavyweight widgets. The library's
/// classes name those types, so the types have to exist or nothing that
/// translates the application ahead of time can build it -- although the
/// application never takes those paths.
///
/// What the build does with the mark (`BytecodeCompliance`): a reference from
/// a bundled library is accepted, and a reference from the application's own
/// classes is reported exactly as if the class did not exist. The layer's
/// contract with a developer is therefore unchanged -- API they call is API
/// that works -- while a library gets to link.
///
/// A marked class says in its own documentation what each member does when it
/// is reached after all; where that cannot be an honest answer it throws
/// `UnsupportedOperationException` naming the API.
///
/// Retained in the class file and no further: the build reads it there, and
/// nothing reads it on a device.
public @interface LinkOnly {
}
