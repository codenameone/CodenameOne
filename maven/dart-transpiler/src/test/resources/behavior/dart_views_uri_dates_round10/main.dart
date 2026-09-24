// getRange is a live view; containsValue and int-keyed lookups use Dart's ==;
// Uri.parse normalizes its components; a throwing catchError test completes the
// returned future; DateTime is proleptic Gregorian.
import 'dart:async';

Future<void> main() async {
  final list = [1, 2, 3];
  final range = list.getRange(0, 2);
  list[0] = 9;
  print(range.toList());
  try {
    list.getRange(1, 5);
  } on RangeError {
    print('range checked eagerly');
  }

  final Map<int, int> ints = {0: 1};
  print(ints.containsValue(1));
  final Map<String, num> nums = {'a': 1};
  print(nums.containsValue(1.0));

  print(Uri.parse('https://x/a b').toString());
  print(Uri.parse('https://x/a b').path);
  print(Uri.parse('https://x/a%2fb%7e%41%zz').toString());
  print(Uri.parse('https://x/p?q=a b#f g').toString());
  print(Uri.parse('HTTPS://X.COM/%7Euser').toString());
  print(Uri.parse('https://x/café').toString());
  print(Uri.parse('https://x/a\\b?c\\d').toString());
  print(Uri.parse('https://x/a b').pathSegments);

  try {
    await Future<int>.error(StateError('op')).catchError((e) => 1,
        test: (e) {
      throw ArgumentError('predicate');
    });
    print('not reached');
  } on ArgumentError {
    print('predicate error completes the future');
  }

  final d = DateTime.utc(1582, 10, 10);
  print('${d.year}-${d.month}-${d.day} ${d.millisecondsSinceEpoch} ${d.weekday}');
  print(DateTime.utc(1582, 10, 4).add(const Duration(days: 1)));
  print(DateTime.utc(1, 1, 1).millisecondsSinceEpoch);
  print(DateTime.utc(2024, 2, 30));
  print(DateTime.utc(2024, 0, 1));
  print(DateTime.utc(-1, 12, 31, 23, 59, 59, 999));
  print(DateTime.fromMillisecondsSinceEpoch(-62135596800001, isUtc: true));
  print(DateTime.utc(2000, 3, 1).weekday);
}
