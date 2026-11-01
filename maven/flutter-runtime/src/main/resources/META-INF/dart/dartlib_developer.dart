// Codename One Dart runtime API stubs -- dart:developer.
//
// A dartlib_<library>.dart file is reached only through an import of
// dart:<library>: dart:developer's log is not dart:math's, and the stubs are
// otherwise one global namespace. Backed by dart.developer (maven/dart-runtime):
// log goes to Codename One's Log; the timeline and the service protocol have
// nothing to talk to and do nothing.

@JavaName('dart.developer.Developer.log')
external void log(String message, {DateTime? time, int? sequenceNumber, int level = 0, String name = '',
    Object? zone, Object? error, StackTrace? stackTrace});

@JavaName('dart.developer.Developer.debugger')
external bool debugger({bool when = true, String? message});

@JavaName('dart.developer.Developer.inspect')
external Object? inspect(Object? object);

@JavaName('dart.developer.Developer.postEvent')
external void postEvent(String eventKind, Map eventData, {String stream = 'Extension'});

@JavaName('dart.developer.Developer.registerExtension')
external void registerExtension(String method, Object handler);

@JavaName('dart.developer.Timeline')
abstract class Timeline {
  external static void startSync(String name, {Map? arguments, Object? flow});
  external static void finishSync();
  external static void instantSync(String name, {Map? arguments});
  external static T timeSync<T>(String name, T Function() function, {Map? arguments, Object? flow});
}

@JavaName('dart.developer.TimelineTask')
class TimelineTask {
  external TimelineTask({TimelineTask? parent, String? filterKey});
  external void start(String name, {Map? arguments});
  external void instant(String name, {Map? arguments});
  external void finish({Map? arguments});
  external TimelineTask pass();
}
