// Two name-resolution gaps.
//
// A prefixed call `a.Foo()` / `a.make()` was lowered to a bare `Foo()` /
// `make()`, losing the prefix, so `a.Foo()` and `b.Foo()` both followed import
// order to the same declaration.
//
// A barrel library that re-exports a declaration did not count as providing it:
// only the imported library's OWN declarations matched, and `Bar` fell back to
// the same-named class of an unrelated library main.dart never imports.
import 'r35_a.dart' as a;
import 'r35_b.dart' as b;
import 'r35_barrel.dart';

void main() {
  print(a.Foo().name);
  print(b.Foo().name);
  print(a.make());
  print(b.make());
  print(Bar().name);
  print(Kind.values.first.name);
  print(where());
}
