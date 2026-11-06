class Box<T> { T v; Box(this.v); void set(T x) { v = x; } }
class A { void m(num x) { print('A $x'); } }
class B extends A { @override void m(covariant int x) { print('B $x'); } }
class MyList<E> { final List<E> inner = <E>[]; void add(E e) => inner.add(e); }
List<T> mk<T>() => <T>[];
T Function(T) g<T>() => (T x) => x;
List<T> wrap<T>(T x) => [x];
void main() {
  print('L1 ${mk<int>() is List<String>}');
  print('L2 ${g<int>() is String Function(String)}');
  print('L3 ${g<int>() is int Function(int)}');
  print('L4 ${wrap(1) is List<int>}');
  print('L5 ${wrap<num>(1) is List<int>}');
  print('L6 ${List.generate(2, (i) => i) is List<int>}');
  print('L7 ${List.filled(2, 0) is List<String>}');
  print('L8 ${{for (var i in [1]) i: '$i'} is Map<int, String>}');
  print('L9 ${[for (var i in [1]) i] is List<int>}');
  print('L10 ${[1].map<String>((e) => '$e').toList() is List<String>}');
  print('L11 ${[1].map((e) => '$e').toList() is List<int>}');
  print('L12 ${Iterable<int>.empty().toList() is List<int>}');
  print('L13 ${<int>[1].followedBy([2]).toList() is List<int>}');
  print('L14 ${[1, 2].reversed.toList() is List<int>}');
  print('L15 ${{'a': 1}.map((k, v) => MapEntry(v, k)) is Map<int, String>}');
  print('L16 ${{'a': 1}.entries.toList() is List<MapEntry<String, int>>}');
  List<dynamic> dl = [1];
  print('L17 ${[...dl] is List<int>}');
  print('L18 ${List<int>.from(dl) is List<int>}');
  print('L19 ${List.of(<int>[1]) is List<int>}');
  print('L20 ${List.unmodifiable(<int>[1]) is List<String>}');
  Object f = (int x, {required String s}) => x;
  print('L21 ${f is int Function(int, {required String s})}');
  print('L22 ${f is int Function(int, {String s})}');
  print('L23 ${f is int Function(int)}');
  Object h = <T>(T x) => x;
  print('L24 ${h is int Function(int)}');
  print('L25 ${h is T Function<T>(T)}');
  Object v = (Object? x) {};
  print('L26 ${v is void Function(int)}');
  print('L27 ${v is Null Function(int)}');
}
