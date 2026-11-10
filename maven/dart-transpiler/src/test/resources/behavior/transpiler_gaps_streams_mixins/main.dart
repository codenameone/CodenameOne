import 'dart:async';
import 'dart:collection';

class Tripled extends StreamView<int> {
  Tripled(Stream<int> s) : super(s.map((x) => x * 3));
}

Future<void> main() async {
  final mapped = [1, 2].map((x) => x + 1).toList();
  print('mapped $mapped');
  final direct = await Stream.fromIterable([7, 8]).toList();
  print('direct $direct');
  final all = await Tripled(Stream.fromIterable([1, 2, 3])).toList();
  print('tripled $all');
  final first = await Tripled(Stream.fromIterable([4, 5])).first;
  print('first $first');
  print('done');
}
