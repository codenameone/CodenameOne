// Generic METHODS whose body uses their own type parameter as a value.

bool isA<T>(Object? x) => x is T;

String nameOf<T>() => '$T';

bool isInt<T>() => T == int;

T? firstOf<T>(List<Object?> xs) {
  for (final x in xs) {
    if (x is T) {
      return x;
    }
  }
  return null;
}

List<T> only<T>(List<Object?> xs) => xs.whereType<T>().toList();

// Hands its T on to a witnessed generic.
int countOf<T>(List<Object?> xs) => only<T>(xs).length;

bool Function(Object?) matcher<T>() => (x) => x is T;

class Adapter<T> {}

class IntAdapter extends Adapter<int> {}

String register<T>(Adapter<T> a) => 'adapter for ${nameOf<T>()}';

class Box<T> {
  final Object? value;
  Box(this.value);
  Box._copy(this.value);
  bool holds() => value is T;
  Box<T> copy() => Box._copy(value);
  static Box<T> make<T>(Object? v) => Box<T>(v);
}

abstract class Registry {
  bool has<T>(Object? v);
}

class RegistryImpl implements Registry {
  @override
  bool has<T>(Object? v) => v is T;
}

class LoudRegistry extends RegistryImpl {
  @override
  bool has<T>(Object? v) => !super.has<T>(v);
}

class Bag<E> {
  final List<Object?> items;
  Bag(this.items);
  List<E> mine() => items.whereType<E>().toList();
}

class Group<T> {
  final List<Object?> _values = [];
  final List<void Function()> _later = [];
  void add(Object? v) {
    _later.add(() {
      _values.add(v);
    });
  }

  List<T> settle() {
    for (final f in _later) {
      f();
    }
    return _values.whereType<T>().toList();
  }

  void settleInto(void Function(List<T>) done) {
    Future.value(0).then((_) {
      done(_values.whereType<T>().toList());
    });
  }
}

// Infers its T from the argument it is handed (through the supertype).
String ignoreAs<T>() => register(Adapter<T>());

class Store<E> extends Box<E> {
  Store(Object? v) : super(v);
}

// Unrelated to Shelf: the same method name must not join its override group.
class OtherShelf {
  Object? shelf<E>(Object? v) => v;
}

abstract class Shelf {
  Box<E> shelf<E>(Object? v);
}

class ShelfImpl implements Shelf {
  @override
  Box<E> shelf<E>(Object? v) => Store<E>(v);

  Future<Box<E>> open<E>(Object? v) async {
    return shelf(v);
  }
}

void main() async {
  print(isA<int>(3));
  print(isA<String>(3));
  print(isA<int?>(null));
  print(nameOf<String>());
  print(isInt<int>());
  print(isInt<double>());
  final int? n = firstOf(['a', 2, 'b', 3]);
  print(n);
  print(firstOf<String>([1, 'x']));
  print(only<String>([1, 'a', 2, 'b']));
  print(countOf<int>([1, 'a', 2, 'b', 3]));
  final m = matcher<String>();
  print(m('s'));
  print(m(1));
  print(register(IntAdapter()));
  print(Box<int>(1).copy().holds());
  print(Box<String>(1).copy().holds());
  print(Box.make<int>(2).holds());
  Registry r = RegistryImpl();
  print(r.has<int>(1));
  print(r.has<String>(1));
  Registry l = LoudRegistry();
  print(l.has<int>(1));
  print(Bag<int>([1, 'a', 2]).mine());
  final g = Group<String>();
  g.add('a');
  g.add(1);
  g.add('b');
  print(g.settle());
  g.settleInto((xs) => print('later $xs'));
  await Future.value(0);
  print(ignoreAs<double>());
  final b = await ShelfImpl().open<int>(5);
  print(b.holds());
  final Box<String> s = await ShelfImpl().open(5);
  print(s.holds());
  print(OtherShelf().shelf(3));
}
