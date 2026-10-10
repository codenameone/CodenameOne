// An int against a double: ==, <, >, min and max convert the int (only compareTo is
// exact), as the Dart VM does. lastIndexWhere/lastIndexOf honour their start;
// removeWhere asks its predicate front to back; a collection that contains itself
// prints a cycle marker.
import 'dart:math';

void main() {
  final int big = int.parse('9007199254740993');
  final double near = double.parse('9007199254740992');
  print(big == near);
  print(big > near);
  print(near < big);
  Object a = big;
  Object b = near;
  print(a == b);
  dynamic da = big;
  dynamic db = near;
  print(da > db);
  print(big.compareTo(near));
  print(min(big, near));
  print(max(near, big));

  print([1, 2, 1].lastIndexWhere((x) => x == 1, 1));
  print([1, 2, 1].lastIndexWhere((x) => x == 1));
  final List<num> nums = [1, 2.0, 1];
  print(nums.lastIndexOf(2));
  print(nums.lastIndexOf(1, 1));

  var flip = false;
  final order = <int>[];
  final l = [1, 2, 3, 4];
  l.removeWhere((x) {
    order.add(x);
    flip = !flip;
    return flip;
  });
  print('$order $l');
  final kept = [1, 2, 3, 4];
  kept.retainWhere((x) => x.isEven);
  print(kept);

  final self = <dynamic>[1];
  self.add(self);
  print(self);
  final m = <dynamic, dynamic>{};
  m['k'] = m;
  print(m);
}
