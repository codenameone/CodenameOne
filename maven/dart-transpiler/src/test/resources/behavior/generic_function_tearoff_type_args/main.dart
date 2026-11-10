T id<T>(T x) => x;
List<T> wrap<T>(T x) => [x];
String tn<T>() => '$T';
class C {
  static String sm<T>(T x) => '$T';
  String im<T>(T x) => '$T';
}
void main() {
  int Function(int) fi = id;
  print(fi(3));
  Object o = fi;
  print(o is int Function(int));
  List<int> Function(int) fw = wrap;
  print(fw(2).runtimeType);
  String Function(int) s1 = C.sm;
  print(s1(1));
  var c = C();
  String Function(String) s2 = c.im;
  print(s2('a'));
  String Function() t = tn;
  print(t());
  String Function() t2 = tn<double>;
  print(t2());
}
