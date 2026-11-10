String p4({int? a, int? b, int? c, int? d}) => '$a$b$c$d';
String p5({int? a, int? b, int? c, int? d, int? e}) => '$a$b$c$d$e';
String p6(int z, {int? a, int? b, int? c, int? d, int? e}) => '$z$a$b$c$d$e';
void t(String l, Object? Function() f) {
  try { print('$l ${f()}'); } catch (e) { print('$l threw ${e.runtimeType}'); }
}
void main() {
  t('p4', () => Function.apply(p4, [], {#a: 1}));
  t('p5', () => Function.apply(p5, [], {#a: 1}));
  t('p6', () => Function.apply(p6, [0], {#e: 1}));
  dynamic d5 = p5;
  t('dyn p5', () => d5(b: 2));
  t('direct', () => p5(e: 3));
}
