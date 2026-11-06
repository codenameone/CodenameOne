import 'dart:collection';

String group(int n) => n.toString().replaceAllMapped(
    RegExp(r'(\d)(?=(\d{3})+$)'), (m) => '${m[1]},');

void main() {
  print(group(1234567));
  print(group(999));
  print(group(1000));
  final re = RegExp(r'a(?=(bc)+d)');
  print(re.hasMatch('abcbcd'));
  print(re.hasMatch('abcd'));
  print(re.hasMatch('abd'));
  print('quq'.replaceAll(RegExp(r'q(?!u)'), 'Q'));

  final list = [1, 2, 3];
  try {
    list[5];
  } on RangeError catch (e) {
    print(e.message);
  }
  try {
    list[-1] = 0;
  } on RangeError catch (e) {
    print(e.message);
    print(e);
  }
  try {
    list.removeAt(7);
  } on RangeError catch (e) {
    print(e.message);
  }

  print(Queue.of([0, 1, 2]));
  print(Queue<int>());
  print(ListQueue.of(['a', 'b']));
  print(DoubleLinkedQueue.of([3, 4]));
}
