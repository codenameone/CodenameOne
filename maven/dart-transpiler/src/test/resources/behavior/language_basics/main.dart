int addAll(List<int> values) {
  int sum = 0;
  for (int i = 0; i < values.length; i++) {
    sum += values[i];
  }
  return sum;
}

String describe(String name, int count) {
  if (count > 3) {
    return '$name has many (${count * 2})';
  } else if (count == 0) {
    return name.isEmpty ? 'nothing' : name;
  }
  return name + ' x' + count.toString();
}

void main() {
  List<int> nums = [1, 2, 3, 4];
  print(addAll(nums));
  print(describe('widgets', 4));
  print(describe('', 0));
  print(describe('gear', 2));
  Map<String, int> ages = {'z': 1, 'a': 2};
  print(ages['z']);
  print(ages);
  print(7 ~/ 2);
  print(-7 % 3);
  print(1 / 2);
  double d = 3.0;
  print(d);
  bool flag = nums.isNotEmpty && ages.length == 2;
  print(flag);
  var doubled = nums.map((n) => n * 2).toList();
  print(doubled);
  int counter = 0;
  var inc = () {
    counter++;
  };
  inc();
  inc();
  print(counter);
  String s = 'hello world';
  print(s.toUpperCase());
  print(s.split(' '));
  print(s.contains('wor'));
  print(s.substring(6));
  while (counter < 5) {
    counter = counter + 1;
  }
  print(counter);
  for (int v in nums) {
    if (v == 3) {
      continue;
    }
    if (v > 3) {
      break;
    }
    print(v);
  }
}
