// int against double compares exactly (statically, through num and through
// dynamic); toRadixString refuses a radix outside 2..36; RegExp honours
// dotAll; min/max/pow keep ints ints; whenComplete waits for an async cleanup
// and surfaces its error.
import 'dart:math';

final List<String> cleanupLog = <String>[];

Future<void> asyncCleanup() async {
  await Future.delayed(const Duration(milliseconds: 10));
  cleanupLog.add('cleaned');
}

Future<void> failingCleanup() async {
  await Future.delayed(const Duration(milliseconds: 1));
  throw StateError('late cleanup failed');
}

Future<void> main() async {
  int big = 9007199254740993;
  double near = 9007199254740992.0;
  print(big.compareTo(near));
  print(near.compareTo(big));
  print(big > near);
  print(near < big);
  num a = big;
  num b = near;
  print(a.compareTo(b));
  print(a > b);
  dynamic da = big;
  dynamic db = near;
  print(da > db);
  final nums = <num>[near, big, 1.5, 1];
  nums.sort();
  print(nums);
  print(0.compareTo(-0.0));
  print(1.compareTo(double.nan));

  try {
    print(255.toRadixString(1));
  } on RangeError {
    print('radix 1 refused');
  }
  try {
    print(255.toRadixString(37));
  } on RangeError {
    print('radix 37 refused');
  }
  print(255.toRadixString(16));

  print(RegExp('a.b', dotAll: true).hasMatch('a\nb'));
  print(RegExp('a.b').hasMatch('a\nb'));

  print(min(1, 2));
  print(max(3, 2) is int);
  print(max(1, 2.5));
  print(pow(2, 3));
  print(pow(2, 3) is int);
  print(pow(2, -1));
  print(pow(2.0, 3));
  int m = min(4, 7);
  print(m + 1);

  final log = <String>[];
  final v = await Future.value(1).whenComplete(() async {
    await Future.delayed(const Duration(milliseconds: 10));
    log.add('cleanup');
  });
  print('$v $log');
  try {
    await Future.value(1).whenComplete(() async {
      throw StateError('cleanup failed');
    });
    print('not caught');
  } on StateError {
    print('cleanup failed');
  }
  final w = await Future.value(2).whenComplete(() => asyncCleanup());
  print('$w $cleanupLog');
  final t = await Future.value(3).whenComplete(asyncCleanup);
  print('$t $cleanupLog');
  try {
    await Future.value(4).whenComplete(() => failingCleanup());
    print('not caught');
  } on StateError {
    print('late cleanup failed');
  }
  try {
    await Future<int>.error(ArgumentError('op failed')).whenComplete(() async {});
  } on ArgumentError {
    print('op failed');
  }
}
