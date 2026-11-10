int counter = 1;
Stream<String> agen() async* { var f = counter > 0 ? (int x) => 'p$x' : (int x) => 'n$x'; yield f(1); yield await Future.value(f(2)); }
Iterable<String> sgen() sync* { String Function(int) f = counter > 0 ? (x) => 'p$x' : (x) => 'n$x'; yield f(1); yield f(2); }
Future<String> af() async { var f = counter > 0 ? (int x) => 'p$x' : (int x) => 'n$x'; await null; return f(3); }
void main() async {
  print(await agen().toList()); print(sgen().toList()); print(await af());
  var l = [1];
  print(l.add == l.add); print(identical(l.add, l.add));
  Object o = l.add; print(o == l.add);
}
