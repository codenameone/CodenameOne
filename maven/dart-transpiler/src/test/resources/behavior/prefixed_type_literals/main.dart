import 'lib.dart' as p;
void main() {
  print(p.Thing);
  print(p.Thing == p.Thing);
  print(p.Thing().runtimeType == p.Thing);
  Type t = p.Col;
  print(t);
  print(p.Gen<int>().runtimeType);
  print(p.Alias == p.Thing);
  var m = {p.Thing: 1, p.Col: 2};
  print(m[p.Thing]);
  print(<Type>[p.Thing, p.Col].contains(p.Col));
}
