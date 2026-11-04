class T { int b = 0; }
extension TE on T { int get s => b; set s(int v) { b = v; } }
class H { final String? _f; H(this._f); }
extension NulE on String? { String w() => 'nullable'; }
extension NonE on String { String w() => 'nonnull'; }
extension OE on Object { String o() => 'obj'; }
extension SE on String { String o() => 'str'; }
void main() {
  Object x = 'a'; if (x is String) { for (int i = 0; i < 2; i++) { print(x.o()); x = 3; } } print(x);
  Object w = 'a'; if (w is String) { while (w is! int) { print(w.o()); w = 3; } }
}
