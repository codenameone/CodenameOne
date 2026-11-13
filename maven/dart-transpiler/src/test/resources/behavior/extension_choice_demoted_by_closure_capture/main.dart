class T { int b = 0; }
extension TE on T { int get s => b; set s(int v) { b = v; } }
class H { final String? _f; H(this._f); }
extension NulE on String? { String w() => 'nullable'; }
extension NonE on String { String w() => 'nonnull'; }
extension OE on Object { String o() => 'obj'; }
extension SE on String { String o() => 'str'; }
void main() {
  Object y = 'b'; if (y is String) { var f = () { y = 4; }; print(y.o()); f(); print(y.o()); } print(y);
}
