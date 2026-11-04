class A {
  String _n() => 'A._n';
  String get _g => 'A._g';
  String callN() => _n();
  String interp() => 'i:$_g ${_g}';
  dynamic dyn() { dynamic d = this; return d._g; }
}
enum EA { _one, two }
String eaNames() => EA.values.map((e) => e.name).join(',') + ' ' + EA._one.toString();
class NA {
  dynamic noSuchMethod(Invocation i) => 'NA:${i.memberName == #_zz}';
}
dynamic callZZ(dynamic d) { try { return d._zz(); } catch (e) { return 'a-nsm'; } }
mixin MA { String _m() => 'MA._m'; String useM() => _m(); }
extension _E on int { String get tag => 'a$this'; }
String aTag() => 3.tag;
typedef _F = String Function(int);
String aF() { _F f = (i) => 'aF$i'; return f(1); }
