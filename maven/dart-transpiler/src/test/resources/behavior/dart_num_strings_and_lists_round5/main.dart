// num arithmetic keeps an int an int and compares exactly; String * int in typed
// code; List.from(growable: false); List.indexOf with a start.

void main() {
  num n = 1;
  print(n + 1);
  print((n + 1) is int);
  num big = 9007199254740993;
  print(big > 9007199254740992);
  num half = 1.5;
  print(half * 2);
  n += 2;
  print(n);
  print(n is int);
  print(n / 2);

  String s = 'ab';
  print(s * 3);
  s *= 2;
  print(s);

  final fixed = List<int>.from([1, 2], growable: false);
  try {
    fixed.add(3);
    print('grew');
  } on UnsupportedError {
    print('fixed length');
  }
  final names = List<String>.from(['a'], growable: false);
  try {
    names.add('b');
    print('grew');
  } on UnsupportedError {
    print('fixed length');
  }

  print([1, 2, 1].indexOf(1, 1));
}
