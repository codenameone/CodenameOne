class Box<T> {
  T v;
  Box(this.v);
}

List<T> makeEmpty<T>() => <T>[];
List<T> makeEmptyCtor<T>() => List<T>.empty(growable: true);
Set<T> makeSet<T>() => <T>{};
Map<K, V> makeMap<K, V>() => <K, V>{};

void main() {
  // A list literal takes its element type from the context type.
  print(Box<List<int>>([]).runtimeType);
  print(Box<List<int>>([]).v.runtimeType);
  print(Box<List<String>>([]).v is List<String>);

  // A type-variable element type carries the caller's argument.
  print(makeEmpty<int>() is List<int>);
  print(makeEmpty<int>() is List<String>);
  print(makeEmptyCtor<int>() is List<String>);
  print(makeEmptyCtor<String>() is List<String>);
  print(makeSet<int>() is Set<String>);
  print(makeSet<int>() is Set<num>);
  print(makeMap<String, int>() is Map<String, String>);
  print(makeMap<String, int>() is Map<String, num>);

  // runtimeType names the collection type with its arguments.
  print(<int>[1, 2].runtimeType);
  print(<double>[1.5].runtimeType);
  print(<String>['a'].runtimeType);
  print(<List<int>>[].runtimeType);
  print(<String, int>{}.runtimeType);
  print(<int>{}.runtimeType);

  // Covariant writes are checked against the reified element type.
  List<num> ints = <int>[1];
  try {
    ints.add(2.5);
    print('added');
  } catch (e) {
    print('TypeError ${e is TypeError}');
  }
  List<Object> strs = <String>['a'];
  try {
    strs.add(3);
    print('added');
  } catch (e) {
    print('TypeError ${e is TypeError}');
  }
  try {
    strs[0] = 4;
    print('stored');
  } catch (e) {
    print('TypeError ${e is TypeError}');
  }
  List<num> nums = <num>[1];
  nums.add(2.5);
  print(nums);

  // is/as on collections read the reified arguments.
  Object m = <String, int>{};
  print(m is Map<String, num>);
  print(m is Map<String, String>);
  print(m is Map<Object, int>);
  Object s = <int>{1};
  print(s is Set<int>);
  print(s is Set<String>);
  print(s is Iterable<num>);
  print(s is Iterable<String>);
  Object l = <String>['x'];
  print(l is Iterable<String>);
  print(l is Iterable<int>);
  print(l is List<Object>);
  try {
    (m as Map<String, String>);
    print('cast ok');
  } catch (e) {
    print('cast fail');
  }

  // Function type tests compare the reified signature.
  var f = (int x) => x;
  print(f is Function(int));
  print(f is int Function(int));
  print(f is int Function(Object));
  print(f is Object Function(int));
  print(f is String Function(int));
  Object g = (Object o) => 'a';
  print(g is String Function(int));
  print(g is String Function(Object));
  print(g is int Function(Object));
}
