void describe(Object f) => print(f.runtimeType);

void main() {
  // A cast map checks each key it hands out, as it checks values.
  final Map<Object, Object> raw = {1: 'one', 'two': 'two'};
  final cast = raw.cast<String, String>();
  print(cast.length);
  try {
    for (final k in cast.keys) {
      print(k);
    }
  } on TypeError {
    print('key TypeError');
  }
  try {
    cast.forEach((k, v) => print('$k=$v'));
  } on TypeError {
    print('forEach TypeError');
  }

  // cast<num> and cast<bool> check each element read.
  final List<Object> mixed = [1, 'x'];
  final nums = mixed.cast<num>();
  print(nums[0]);
  try {
    print(nums[1]);
  } on TypeError {
    print('num TypeError');
  }
  final List<Object> flags = [true, 3];
  final bools = flags.cast<bool>();
  print(bools.first);
  try {
    print(bools[1]);
  } on TypeError {
    print('bool TypeError');
  }

  // Named parameters compare as a set, not by declaration order.
  void ab({int a = 0, int b = 0}) {}
  void ba({int b = 0, int a = 0}) {}
  print(ab.runtimeType == ba.runtimeType);
  describe(ab);
  describe(ba);
  print(ba is void Function({int a, int b}));
}
