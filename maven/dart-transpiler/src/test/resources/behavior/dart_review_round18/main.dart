// Set.retainWhere/removeWhere, String.lastIndexOf with a start, compound and
// ++/-- assignment through an app getter/setter pair, `is` against a generic and a
// nullable type, and a failed `as` that must be Dart's TypeError.
class Counter {
  int _v = 0;
  int get value => _v;
  set value(int v) {
    _v = v;
    print('set $v');
  }
}

int calls = 0;
Counter make(Counter c) {
  calls++;
  return c;
}

Object? pick(Object? v) {
  calls++;
  return v;
}

void main() {
  final s = <int>{1, 2, 3, 4, 5, 6};
  s.retainWhere((e) => e.isEven);
  print(s.toList());
  s.removeWhere((e) => e > 4);
  print(s.toList());

  final str = 'abcabc';
  print(str.lastIndexOf('b'));
  print(str.lastIndexOf('b', 3));
  print(str.lastIndexOf('c', 1));
  try {
    str.lastIndexOf('b', 99);
  } on RangeError {
    print('RangeError');
  }

  final counter = Counter();
  counter.value += 5;
  counter.value -= 2;
  counter.value++;
  counter.value--;
  print(counter.value);
  calls = 0;
  make(counter).value += 10;
  print('${counter.value} calls=$calls');

  Object value = <String>['a'];
  print(value is List<String>);
  print(value is! List<String>);
  final described = switch (value) {
    List<String> l => 'list ${l.length}',
    _ => 'other',
  };
  print(described);
  Object? none;
  print(none is int?);
  print(none is int);
  print(none is! int?);
  Object five = 5;
  print(five is int?);
  print(five is String?);
  calls = 0;
  print(pick(null) is int?);
  print('calls=$calls');

  Object text = 'x';
  try {
    print(text as int);
  } on TypeError {
    print('TypeError');
  }
  try {
    print(none as int);
  } on TypeError {
    print('TypeError for null');
  }
  print(none as int?);
  print((five as int) + 1);
}
