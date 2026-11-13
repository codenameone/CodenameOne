class _State {
  final String s = 'b';
  String who() => 'b:$s';
}

enum _Mode { gamma }

mixin _Mix {
  String mixed() => 'mixB';
}

class _UsesMix with _Mix {}

extension _Ext on int {
  int twice() => this * 10;
}

Object makeB() => _State();
String describeB(Object o) => o is _State ? (o as _State).who() : 'notB';
bool isB(Object o) => o is _State;
List<_State> boxB() => <_State>[_State(), _State()];
String modeB() => _Mode.values.length.toString() + ' ' + _Mode.gamma.toString();
String extB(int x) => x.twice().toString();
String mixB() {
  Object m = _UsesMix();
  return (m as _Mix).mixed() + ' ' + (m is _Mix).toString() + ' ' + <_State>[].runtimeType.toString();
}
