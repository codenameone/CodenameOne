import 'lib_a.dart' as a;
import 'lib_b.dart' as b;

class LocalA extends a.Base with a.M {
  @override
  String who() => 'LocalA>' + super.who();
}

class LocalB extends b.Base with b.M {
  @override
  String who() => 'LocalB>' + super.who() + ' ' + only();
}

class DeepB extends b.FromB {
  String all() => '${who()} ${tag()} ${weight()}';
}

void main() {
  final fa = a.FromA();
  final fb = b.FromB();
  print(fa.who());
  print(fa.tag());
  print(fa.depth);
  print(fb.who());
  print(fb.tag());
  print(fb.weight());
  print(fb.only());
  final la = LocalA();
  final lb = LocalB();
  print(la.who());
  print(la.tag());
  print(lb.who());
  print(lb.tag());
  print(lb.weight());
  print(DeepB().all());
  print(fa is a.Base);
  print(fa is a.M);
  Object o = fb;
  print(o is b.Base);
  print(o is b.M);
  print(o is a.Base);
  print(o is a.M);
  print(la is b.M);
  final List<a.Base> as = [fa, la];
  print(as.map((x) => x.depth).toList());
}
