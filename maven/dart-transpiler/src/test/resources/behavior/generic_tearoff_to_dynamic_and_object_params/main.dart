String g<T>(T x) => '$T';
class C { static String s<T>(T x) => '$T'; String m<T>(T x) => '$T'; }
void main() {
  String Function(dynamic) a = g;
  String Function(Object?) b = g;
  print(a == b);
  print(a(1)); print(b(1));
  String Function(dynamic) c = C.s;
  String Function(Object?) d = C.s;
  print(c == d);
  var o = C();
  String Function(dynamic) e = o.m;
  String Function(Object?) f = o.m;
  print(e == f);
  String Function(List<dynamic>) h = g;
  String Function(List<Object?>) i = g;
  print(h == i);
  print(h(const []));
}
