import 'pt_a.dart';
import 'pt_b.dart';

class _State {
  String who() => 'main';
}

enum _Mode { on }

typedef _Fn = int Function();

extension _Ext on int {
  int twice() => this * 2;
}

void main() {
  final s = _State();
  print(s.who());
  print(s.runtimeType);
  print(_Mode.on);
  _Fn f = () => 3;
  print(f());
  print(4.twice());
  final a = makeA();
  final b = makeB();
  print(describeA(a));
  print(describeB(b));
  print(a.runtimeType);
  print(b.runtimeType);
  Object o = a;
  print(o is _State);
  print(isA(o));
  print(isB(o));
  print(isB(b));
  print(boxA().value.runtimeType);
  print(boxB().length);
  print(modeA());
  print(modeB());
  print(extA(5));
  print(extB(5));
  print(partState());
  print(publicFromA().label());
  print(mixA());
  print(mixB());
}
