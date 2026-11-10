void sh<T>(T x) { print('$T'); }

extension ON on String? { String tag() => 'nullable'; String get g => 'gnullable'; }
extension NN on String { String tag() => 'nonnull'; String get g => 'gnonnull'; }
extension OO on Object? { String t2() => 'obj?'; }
extension OS on Object { String t2() => 'obj'; }
extension LN on List<int>? { String t3() => 'list?'; }
extension LI on Iterable<int> { String t3() => 'iter'; }

class K { final String? _s; K(this._s);
  String run() => _s != null ? _s.tag() : 'none'; }

String? mk(bool b) => b ? 'v' : null;
class Box { String? v; Box? next; }

void main() {
  String? x = 'a';
  print(x.tag());
  print(x.g);
  String? y = mk(true);
  print(y.tag());
  print(y.g);
  if (y != null) { print(y.tag()); print(y.g); print(y.t2()); }
  Object? o = 1;
  print(o.t2());
  Object? p = mk(false);
  print(p.t2());
  List<int>? l2 = mk(false) == null ? null : [1];
  print(l2.t3());
  print(K('k').run());
  // assignment promotions with && / ||
  String? z;
  if ((z = mk(true)) != null && z == 'v') sh(z);
  String? w;
  if ((w = mk(false)) == null || w == 'q') sh(w);
  Box b = Box()..next = Box();
  String? q;
  if ((q = b.next?.v) != null && (q = b.next?.next?.v) == null) sh(q);
  String? r;
  if ((r = b.next?.v ?? 'd') != null || r!.isEmpty) sh(r);
  String? s2 = mk(true);
  if (s2 != null || (s2 = 'x') != null) sh(s2);
  String? s3 = mk(false);
  if (s3 == null && (s3 = mk(true)) != null) sh(s3);
  sh(s3);
  String? s4 = mk(true);
  final t = s4 != null && (s4 = null) == null ? s4 : 'k';
  sh(t);
}
