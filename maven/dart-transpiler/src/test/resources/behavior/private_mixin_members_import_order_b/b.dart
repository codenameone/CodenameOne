import 'a.dart';
class B extends A {
  String _n() => 'B._n';
  String get _g => 'B._g';
  String callB() => _n();
  dynamic dynB(dynamic d) { try { return d._g; } catch (e) { return 'b-nsm'; } }
  String toString() => 'B($_g) ${_g.length}';
}
enum EB { _one, three }
String ebNames() => EB.values.map((e) => e.name).join(',') + ' ' + EB._one.toString() + ' ' + EB._one.name;
class NB {
  String _zz() => 'NB._zz';
  dynamic noSuchMethod(Invocation i) => 'NB:${i.memberName == #_zz}';
}
dynamic callZZb(dynamic d) { try { return d._zz(); } catch (e) { return 'b-nsm'; } }
class C with MA { String _m() => 'C._m'; String own() => _m(); }
extension _E on int { String get tag => 'b$this'; }
String bTag() => 4.tag;
typedef _F = int Function(String);
String bF() { _F f = (s) => s.length; return 'bF${f("xy")}'; }
