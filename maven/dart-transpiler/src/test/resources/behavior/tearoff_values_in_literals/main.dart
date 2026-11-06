typedef IntFn = int Function(int);
typedef Scaler = int Function(int, {int scale});

class M {
  final int k;
  M(this.k);
  int twice(int x) => x * 2 * k;
  String? tag(String? s) => s == null ? null : '$s$k';
  void note(Object o) {}
  int scaled(int x, {int scale = 1}) => x * scale + k;
}

final M shared = M(1);
M pick() => shared;

void main() {
  final s = {pick().twice, pick().twice};
  print(s.length);
  final tags = {pick().tag, pick().tag};
  print(tags.length);
  final notes = <void Function(Object)>{pick().note, pick().note, M(3).note};
  print(notes.length);
  final t = <int Function(int)>{pick().twice, pick().twice};
  print(t.length);
  Set<IntFn> u = {pick().twice, shared.twice};
  print(u.length);
  final v = <IntFn>[pick().twice];
  print(v.contains(shared.twice));
  final w = <Object>{pick().twice, pick().twice};
  print(w.length);
  final x = <IntFn, int>{pick().twice: 1};
  print(x[shared.twice]);
  final l = [pick().twice, M(1).twice];
  print(l[0] == l[1]);
  print(l[0] == pick().twice);
  final m = {'a': pick().twice};
  print(m['a']!(5));
  for (final f in s) {
    print(f(3));
    print(f is int Function(int));
    print(f is String Function(int));
  }
  Scaler a = shared.scaled;
  Scaler b = shared.scaled;
  print(a == b);
  print(a(2, scale: 3));
  print(a(2));
  print(a is Scaler);
  print((a as Object) is int Function(int));
  final other = M(2);
  Scaler c = other.scaled;
  print(a == c);
  print({a, b, c}.length);
}
