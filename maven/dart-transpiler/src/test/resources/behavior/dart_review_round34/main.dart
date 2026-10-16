// Two imported libraries declare the same class, enum and function. The FIRST
// import hides them and the second shows them, so Dart resolves each name to
// the second library. Resolution walked the import uris in order and ignored
// show/hide, binding every name to the hidden first library instead.
import 'r34_a.dart' hide Foo, Mode, label;
import 'r34_b.dart' show Foo, Mode, label;

void main() {
  print(Foo().name);
  print(Mode.only.toString());
  print(label());
  print(onlyInA());
}
