class T { int b = 0; }
extension TE on T { int get s => b; set s(int v) { b = v; } }
class H { final String? _f; H(this._f); }
extension NulE on String? { String w() => 'nullable'; }
extension NonE on String { String w() => 'nonnull'; }
extension OE on Object { String o() => 'obj'; }
extension SE on String { String o() => 'str'; }
void main() {
  T? t = T(); t!.s++; print(t.s); T u = T(); u.s++; ++u.s; print(u.s); print(t!.s--); print(t.s);
}
