import 'ps_a.dart';
import 'ps_b.dart';

// This library has `_v` as a top-level, a member of two classes and a symbol.
int _v = 10;
int _tick() => 1;

class Holder {
  int _v = 20;
  int _tick() => 2;
  int own() => _v + _tick();
}

class Plain {
  // Declares no `_v`: a bare `_v` here is the top-level one.
  int read() => _v + _tick();
}

class Spy {
  dynamic noSuchMethod(Invocation i) {
    print('nsm ${i.memberName} ${i.isGetter} ${i.isSetter}');
    return null;
  }
}

void main() {
  print(#_v);
  print((#_v).toString());
  print(#_v == #_v);
  print(#_v == symbolA());
  print(#_v == symbolB());
  print(symbolA() == symbolB());
  print(symbolA());
  print(symbolB());
  final h = Holder();
  print(h.own());
  print(Plain().read());
  print(_v);
  _v = 11;
  print(_v);
  print(h._v);
  print(readA());
  print(readB());
  print(memberA(PA()));
  print(memberB(PB()));
  dynamic s = Spy();
  s._v;
  s._v = 3;
  s._tick(1);
  spyA(s);
  spyB(s);
  dynamic pa = PA();
  print(memberB2(pa));
}
