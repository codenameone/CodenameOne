extension ON on String? {
  set s(int v) { print('snullable $v'); }
  String get g => 'gnullable';
}
extension NN on String {
  set s(int v) { print('snonnull $v'); }
  String get g => 'gnonnull';
}
String? mk(bool b) => b ? 'v' : null;

class H {
  final String? _h;
  H(this._h);
  void run(H o) {
    if (_h != null) { _h.s = 1; print(_h.g); }
    if (this._h != null) { this._h.s = 2; print(this._h.g); }
    if (o._h != null) { o._h.s = 3; print(o._h.g); }
  }
}

void main() {
  String? x = mk(true);
  x.s = 4;
  print(x.g);
  if (x != null) { x.s = 5; print(x.g); }
  String? y = mk(false);
  y.s = 6;
  print(y.g);
  H('h').run(H('o'));
}
