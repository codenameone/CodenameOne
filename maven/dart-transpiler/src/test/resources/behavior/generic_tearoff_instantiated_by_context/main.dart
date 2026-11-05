String u<T>() => '$T';
String bnd<T extends num>() => '$T';
String w<T>(int x) => '$T:$x';
class G<E> {
  String Function() get t => u;
  String Function(int) get t2 => w;
  String inner<T>() { String Function() f = u; return f(); }
}
void main() {
  String Function() a = u;
  String Function() b = bnd;
  print(a()); print(b());
  print(a == u<dynamic>);
  print(b == bnd<num>);
  String Function(int) c = w;
  print(c(3));
  print(G<String>().t());
  print(G<String>().t2(4));
  print(G<String>().t == G<int>().t);
  print(G<int>().inner<String>());
  List<String Function()> l = [u, bnd];
  for (var f in l) print(f());
}
