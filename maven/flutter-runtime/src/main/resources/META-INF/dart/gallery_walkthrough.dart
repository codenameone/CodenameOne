// Pointer plumbing, so ONE walkthrough script can drive both stacks.
//
// A demo written against these runs unchanged on the native app, where they are
// Flutter's own, and on the transpiled one, where they reach the Codename One
// runtime and become real pointer events. The alternative -- two drivers, one
// per stack -- is what produced videos whose panes did different things.

@JavaName('com.codename1.flutter.gestures.PointerEvent')
abstract class PointerEvent {
  external Offset get position;
}

@JavaName('com.codename1.flutter.gestures.PointerDownEvent')
class PointerDownEvent extends PointerEvent {
  external PointerDownEvent({Offset? position, int? pointer});
}

@JavaName('com.codename1.flutter.gestures.PointerMoveEvent')
class PointerMoveEvent extends PointerEvent {
  external PointerMoveEvent({Offset? position, int? pointer, Offset? delta});
}

@JavaName('com.codename1.flutter.gestures.PointerUpEvent')
class PointerUpEvent extends PointerEvent {
  external PointerUpEvent({Offset? position, int? pointer});
}

@JavaName('com.codename1.flutter.gestures.GestureBinding')
class GestureBinding {
  external static GestureBinding get instance;
  external void handlePointerEvent(PointerEvent event);
}
