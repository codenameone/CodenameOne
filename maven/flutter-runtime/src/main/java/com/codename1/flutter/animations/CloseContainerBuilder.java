package com.codename1.flutter.animations;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Widget;

import dart.runtime.Funcs;

/**
 * Signature for the {@code closedBuilder} of an {@link OpenContainer} — the
 * animations package's {@code CloseContainerBuilder}
 * ({@code Widget Function(BuildContext, VoidCallback openContainer)}). The
 * second argument is the callback that triggers the open transition. A
 * single-abstract-method interface so transpiled Dart closures bind as Java
 * lambdas.
 */
public interface CloseContainerBuilder {
    Widget call(BuildContext context, Funcs.VoidFunc0 openContainer);
}
