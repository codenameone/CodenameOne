// Codename One Flutter runtime API stubs — "identifiersEnums" category
// (new_gallery, Pass 3).
//
// This file resolves bare unresolved IDENTIFIERS the app references directly:
//   * dart:math top-level constant `pi` (imported unprefixed `import 'dart:math';`)
//   * package:flutter/scheduler top-level `timeDilation` (mutable double, read + write)
//   * package:flutter/foundation top-level getter `defaultTargetPlatform`
//   * enums / const-instance value types used only via `X.constant` — declared as
//     stub enums with EXACT Flutter constant names (params that receive them are
//     declared loosely as `Object?` elsewhere, so an enum shape is sufficient)
//   * dart:core `Uri` and the `Localizations` inherited-widget lookup helpers.
//
// Top-level values map, via @JavaName, to a fully-qualified Java static field
// (StubRegistry.topLevelVars + JavaEmitter.emitIdent). The `= <literal>` on each
// is only a parse anchor — the emitted reference is the @JavaName target, so both
// reads (`pi / 2`) and writes (`timeDilation = 5.0`) go through the Java static.
//
// TargetPlatform itself is contributed by gallery_dartCore.dart; we only add the
// `defaultTargetPlatform` accessor here and reference the existing enum.

// --- dart:math unprefixed top-level constant --------------------------------

@JavaName('dart.math.DartMath.pi')
double pi = 3.141592653589793;

// --- package:flutter/scheduler ---------------------------------------------

@JavaName('com.codename1.flutter.scheduler.SchedulerLib.timeDilation')
double timeDilation = 1.0;

// --- package:flutter/foundation --------------------------------------------

@JavaName('com.codename1.flutter.foundation.FoundationLib.defaultTargetPlatform')
TargetPlatform defaultTargetPlatform = TargetPlatform.android;

// --- services : text-input enums --------------------------------------------

@JavaName('com.codename1.flutter.services.TextInputAction')
enum TextInputAction {
  none, unspecified, done, go, search, send, next, previous,
  continueAction, join, route, emergencyCall, newline
}

@JavaName('com.codename1.flutter.services.TextInputType')
enum TextInputType {
  text, multiline, number, phone, datetime, emailAddress, url,
  visiblePassword, name, streetAddress, none
}

@JavaName('com.codename1.flutter.services.TextCapitalization')
enum TextCapitalization { none, words, sentences, characters }

// --- painting / rendering enums --------------------------------------------

@JavaName('com.codename1.flutter.TextOverflow')
enum TextOverflow { clip, fade, ellipsis, visible }

@JavaName('com.codename1.flutter.rendering.HitTestBehavior')
enum HitTestBehavior { deferToChild, opaque, translucent }

// FloatingActionButtonLocation is a class of static const instances in Flutter;
// modelled here as an enum since the app only ever names a constant and the
// Scaffold slot receives it as `Object?`.
@JavaName('com.codename1.flutter.material.FloatingActionButtonLocation')
enum FloatingActionButtonLocation {
  startTop, miniStartTop, centerTop, miniCenterTop, endTop, miniEndTop,
  startFloat, miniStartFloat, centerFloat, miniCenterFloat, endFloat, miniEndFloat,
  startDocked, miniStartDocked, centerDocked, miniCenterDocked, endDocked, miniEndDocked
}

// --- dart:core Uri ----------------------------------------------------------

@JavaName('dart.core.DartUri')
class Uri {
  external static Uri parse(String uri);
  external static Uri? tryParse(String uri);
  external String get scheme;
  external String get host;
  external int get port;
  external String get path;
  external String get query;
  external String get fragment;
  external List<String> get pathSegments;
  external Map<String, String> get queryParameters;
  external String toString();
}

// --- widgets : Localizations lookup ----------------------------------------
// Localizations.of<T>(context, type) returns the inherited T; localeOf(context)
// returns the ambient Locale. `of` returns an unbound type parameter, so the
// emitter threads the requested type as a trailing Class<T> witness.

@JavaName('com.codename1.flutter.widgets.Localizations')
class Localizations {
  external static T of(BuildContext context, Object type);
  external static Locale localeOf(BuildContext context);
}
