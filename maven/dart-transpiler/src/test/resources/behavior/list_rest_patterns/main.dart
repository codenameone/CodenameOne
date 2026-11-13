Future<List<int>> load() async => [4, 5, 6, 7];

String describe(Object o) {
  switch (o) {
    case [int first, ...var middle, int last]:
      return 'ends $first..$last middle $middle';
    case [...]:
      return 'empty-ish list';
    default:
      return 'other';
  }
}

Future<void> run() async {
  final [h, ...tail] = await load();
  print('async head $h tail $tail');
  var [..., y, z] = await load();
  print('async last two $y $z');
}

void main() async {
  var list = [1, 2, 3];
  var [a, ...rest] = list;
  print('$a $rest');
  final [x, ..., w] = list;
  print('$x $w');
  var [...all] = list;
  all.add(9);
  print('$all $list');
  print(describe([1, 2, 3, 4]));
  print(describe([1, 2]));
  print(describe([]));
  print(describe('s'));
  if (list case [_, ...var r] when r.length == 2) {
    print('if-case rest $r');
  }
  await run();
}
