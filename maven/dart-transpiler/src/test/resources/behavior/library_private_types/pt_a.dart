part 'pt_a_part.dart';

class _State {
  final int n = 1;
  String who() => 'a$n';
}

enum _Mode { alpha, beta }

typedef _Fn = String Function(int);

mixin _Mix {
  String mixed() => 'mixA';
}

class _UsesMix with _Mix {}

extension _Ext on int {
  int twice() => this + 100;
}

class Box<T> {
  final T value;
  Box(this.value);
}

abstract class Labeled {
  String label();
}

class _Impl implements Labeled {
  String label() => 'implA';
}

Object makeA() => _State();
String describeA(Object o) => o is _State ? (o as _State).who() : 'notA';
bool isA(Object o) => o is _State;
Box<_State> boxA() => Box<_State>(_State());
String modeA() => _Mode.values.map((m) => m.name).join(',') + ' ' + _Mode.beta.toString();
String extA(int x) {
  _Fn f = (i) => 'f$i';
  return f(x.twice());
}
String partState() => _PartState().tag();
Labeled publicFromA() => _Impl();
String mixA() => _UsesMix().mixed() + ' ' + (_UsesMix() is _Mix).toString();
