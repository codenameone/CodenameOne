import 'dart:async';

void Function(int, String)? sink;

void register(void Function(int)? onChanged) {
  if (onChanged != null) {
    onChanged(3);
    onChanged(7);
  }
}

void report(int i, String tag) {
  print('report $i $tag');
}

Future<void> main() async {
  sink = report;
  register((index) {
    final tag = index > 5 ? 'big' : 'small';
    return sink!(index, tag);
  });

  final doubler = StreamTransformer<int, int>((stream, cancelOnError) {
    final controller = StreamController<int>();
    controller.onListen = () {
      stream.listen((v) => controller.add(v * 2), onDone: controller.close);
    };
    return controller.stream.listen(null);
  });
  final out = await Stream.fromIterable([1, 2, 3]).transform(doubler).toList();
  print('doubled $out');
}
