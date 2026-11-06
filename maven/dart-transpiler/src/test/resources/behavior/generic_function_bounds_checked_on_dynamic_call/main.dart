void tryIt(String label, void Function() f) {
  try { f(); print('$label ok'); } catch (e) { print('$label ${e is TypeError ? "TypeError" : e.runtimeType}'); }
}
T pickNum<T extends num>(T x) => x;
T pickCmp<T extends Comparable<T>>(T x) => x;
class Holder {
  T keep<T extends num>(T x) => x;
  static T keepStatic<T extends num>(T x) => x;
}
void main() {
  dynamic f1 = pickNum;
  tryIt('topLevelTearOffRejectsString', () => print(f1('s')));
  tryIt('topLevelTearOffAcceptsInt', () => print(f1(4)));
  dynamic f2 = pickCmp;
  for (final v in <Object>[3, 'a', Object()]) {
    tryIt('selfBoundTearOff ${v.runtimeType}', () => print(f2(v)));
  }
  dynamic h = Holder();
  tryIt('dynamicMethodRejectsString', () => print(h.keep('s')));
  tryIt('dynamicMethodAcceptsDouble', () => print(h.keep(1.5)));
  dynamic mt = Holder().keep;
  tryIt('methodTearOffRejectsString', () => print(mt('s')));
  dynamic st = Holder.keepStatic;
  tryIt('staticTearOffRejectsString', () => print(st('s')));
  var c = <T extends Comparable<T>>(T x) => x;
  dynamic d = c;
  for (final v in <Object>[3, 'a', Object()]) {
    tryIt('selfBoundLambda ${v.runtimeType}', () => print(d(v)));
  }
  tryIt('selfBoundExplicitString', () => print(pickCmp<String>('z')));
}
