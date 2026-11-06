class Part { final String id; const Part(this.id); }
class Box { final List<Part> items; final Part? main; const Box(this.items, [this.main]); }
extension PartX on Part { String describe() => 'part<$id>'; }
String look(Object o) => switch (o) {
  Box(items: [var only]) => 'one ${only.describe()}',
  Box(main: Part m) => 'main ${m.describe()}',
  Box(items: [var first, ...]) => 'first ${first.describe()}',
  _ => 'other',
};
class Stack<T> { final List<T> xs = []; }
class Pair<A, B> { final A a; final B b; Pair(this.a, this.b); }
void main() {
  print(look(Box([Part('a')])));
  print(look(Box([Part('b'), Part('c')], Part('m'))));
  print(look(Box([Part('d'), Part('e')])));
  final rec = (Part('r'), [Part('s')]);
  final (p, [q]) = rec;
  print(p.describe() + q.describe());
  if (rec case (var x, [var y])) print('${x.describe()} ${y.describe()}');
  print(Stack<int>().runtimeType);
  print(Pair<String, int>('a', 1).runtimeType);
  print(Stack<String>().toString().startsWith("Instance of 'Stack<String>'"));
}
