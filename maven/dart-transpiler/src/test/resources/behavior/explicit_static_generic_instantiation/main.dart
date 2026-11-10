class K { static String s<T>(T x) => '$T:$x'; }
class G<A> { static String s<T>(T x, [T? y]) => '$T:$x:$y'; }
void main() {
  var a = K.s<int?>;
  print(a(null));
  print(a == K.s<int?>);
  print(a == K.s<int>);
  var b = G.s<String>;
  print(b('q'));
  print(b == G.s<String>);
  String Function(String, [String?]) c = G.s;
  print(c == b);
  var l = [K.s<int>, K.s<int>];
  print(l[0] == l[1]);
  print(l.map((f) => f(5)).toList());
}
