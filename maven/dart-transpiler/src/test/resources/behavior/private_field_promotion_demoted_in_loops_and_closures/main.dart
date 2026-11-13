class T { int b = 0; }
extension TE on T { int get s => b; set s(int v) { b = v; } }
class H { final String? _f; H(this._f); }
extension NulE on String? { String w() => 'nullable'; }
extension NonE on String { String w() => 'nonnull'; }
extension OE on Object { String o() => 'obj'; }
extension SE on String { String o() => 'str'; }
void main() {
  var q = H('s'); if (q._f != null) { for (int i = 0; i < 2; i++) { print(q._f.w()); q = H(null); } }
  var r = H('s'); if (r._f != null) { print(r._f.w()); void g() { r = H(null); } g(); print(r._f.w()); }
  var z = H('z'); if (z._f != null) { print(z._f.w()); z = H('again'); print(z._f.w()); }
}
