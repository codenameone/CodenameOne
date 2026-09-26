Future<int> compute() async {
  await Future.delayed(Duration(milliseconds: 40));
  return 41;
}

Future<String> fetchName() async => 'dart';

void main() async {
  print('start');
  int v = await compute();
  print(v + 1);
  String n = await fetchName();
  print(n.toUpperCase());
  try {
    throw FormatException('bad');
  } on FormatException catch (e) {
    print('caught: $e');
  } catch (e) {
    print('other');
  } finally {
    print('done');
  }
  try {
    throw Exception('boom');
  } on FormatException catch (e) {
    print('wrong');
  } catch (e) {
    print('generic: $e');
  }
  var results = await Future.wait([compute(), compute()]);
  print(results);
}
