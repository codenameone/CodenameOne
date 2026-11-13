String h({int? class_, int? $x, int? int2, String? new_ = 'n', int? a$b}) => '$class_ ${$x} $int2 $new_ ${a$b}';
String k({required int default_, int? $}) => '$default_ ${$}';
class C {
  final int $v;
  final int? native;
  C({required this.$v, this.native});
  String toString() => 'C(${$v}, $native)';
}
void main() {
  print(h(class_: 1, $x: 2, int2: 3, a$b: 4));
  print(Function.apply(h, [], {#class_: 5, #$x: 6, #new_: 'q', #a$b: 8}));
  print(k(default_: 1, $: 2));
  print(Function.apply(k, [], {#default_: 3, #$: 4}));
  print(C($v: 1, native: 2));
  print(Function.apply(C.new, [], {#$v: 9, #native: 8}));
  dynamic d = h;
  print(d(class_: 7, $x: 8));
  dynamic cc = C.new;
  print(cc($v: 3));
}
