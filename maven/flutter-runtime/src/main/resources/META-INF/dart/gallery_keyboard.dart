// Codename One Flutter runtime API stubs — KEYBOARD / services category.
//
// Signature-only declarations for the hardware-keyboard surface new_gallery
// touches through Focus.onKeyEvent / KeyboardListener: the KeyEvent hierarchy,
// the LogicalKeyboardKey constants compared against event.logicalKey, and the
// KeyEventResult returned from key handlers. Each @JavaName points at the
// hand-written Java runtime under com.codename1.flutter.services. Conventions
// match flutter_material.dart.

// --- key-handler result ------------------------------------------------

// What a key handler reports back to the focus system — Flutter's
// `KeyEventResult`. highlight_focus / rally login return handled / ignored.
@JavaName('com.codename1.flutter.services.KeyEventResult')
enum KeyEventResult { handled, ignored, skipRemainingHandlers }

// --- key events --------------------------------------------------------

// Base class for a keyboard event in the modern (HardwareKeyboard) API —
// Flutter's `KeyEvent`. The onKeyEvent callbacks receive one of the concrete
// subclasses; code switches on `event is KeyDownEvent` and reads `logicalKey`.
@JavaName('com.codename1.flutter.services.KeyEvent')
abstract class KeyEvent {
  external LogicalKeyboardKey get logicalKey;
  external PhysicalKeyboardKey get physicalKey;
  external String? get character;
  external Duration get timeStamp;
}

@JavaName('com.codename1.flutter.services.KeyDownEvent')
class KeyDownEvent extends KeyEvent {
  external KeyDownEvent({required LogicalKeyboardKey logicalKey, required PhysicalKeyboardKey physicalKey, String? character});
}

@JavaName('com.codename1.flutter.services.KeyUpEvent')
class KeyUpEvent extends KeyEvent {
  external KeyUpEvent({required LogicalKeyboardKey logicalKey, required PhysicalKeyboardKey physicalKey});
}

@JavaName('com.codename1.flutter.services.KeyRepeatEvent')
class KeyRepeatEvent extends KeyEvent {
  external KeyRepeatEvent({required LogicalKeyboardKey logicalKey, required PhysicalKeyboardKey physicalKey, String? character});
}

// A physical (scan-code) key — Flutter's `PhysicalKeyboardKey`. Present so the
// KeyEvent.physicalKey getter resolves; new_gallery does not compare against it.
@JavaName('com.codename1.flutter.services.PhysicalKeyboardKey')
class PhysicalKeyboardKey {
  external int get usbHidUsage;
  external String? get debugName;
}

// --- logical keys ------------------------------------------------------

// A logical (layout-dependent) key — Flutter's `LogicalKeyboardKey`. The static
// constants are singletons compared with `==` against `event.logicalKey`.
// new_gallery uses enter / numpadEnter / space / escape; the rest round out the
// navigation and control keys so the class matches the real Flutter shape.
@JavaName('com.codename1.flutter.services.LogicalKeyboardKey')
class LogicalKeyboardKey {
  external int get keyId;
  external String? get keyLabel;
  external String? get debugName;

  external static LogicalKeyboardKey get arrowUp;
  external static LogicalKeyboardKey get arrowDown;
  external static LogicalKeyboardKey get arrowLeft;
  external static LogicalKeyboardKey get arrowRight;
  external static LogicalKeyboardKey get enter;
  external static LogicalKeyboardKey get numpadEnter;
  external static LogicalKeyboardKey get escape;
  external static LogicalKeyboardKey get tab;
  external static LogicalKeyboardKey get space;
  external static LogicalKeyboardKey get backspace;
  external static LogicalKeyboardKey get delete;
  external static LogicalKeyboardKey get home;
  external static LogicalKeyboardKey get end;
  external static LogicalKeyboardKey get pageUp;
  external static LogicalKeyboardKey get pageDown;
  external static LogicalKeyboardKey get shift;
  external static LogicalKeyboardKey get control;
  external static LogicalKeyboardKey get meta;
  external static LogicalKeyboardKey get alt;
}
