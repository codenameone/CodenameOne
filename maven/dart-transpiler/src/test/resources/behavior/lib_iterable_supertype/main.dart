import 'dart:convert';

class Countdown extends Iterable<int> {
  final int from;
  Countdown(this.from);

  @override
  Iterator<int> get iterator => List<int>.generate(from, (i) => from - i).iterator;
}

class Summing extends ByteConversionSink {
  int total = 0;
  int chunks = 0;
  bool closed = false;

  @override
  void add(List<int> chunk) {
    chunks++;
    for (final b in chunk) {
      total += b;
    }
  }

  @override
  void addSlice(List<int> chunk, int start, int end, bool isLast) {
    add(chunk.sublist(start, end));
    if (isLast) close();
  }

  @override
  void close() {
    closed = true;
  }
}

void main() {
  final c = Countdown(4);
  print(c.toList());
  print(c.length);
  print(c.map((x) => x * 10).join(','));
  print(c.where((x) => x != 3).toList());
  print(c.where((x) => x.isEven).toList());
  print(c.first);
  print(c.contains(3));
  final s = Summing();
  s.add([1, 2, 3]);
  s.addSlice([10, 20, 30, 40], 1, 3, true);
  print('${s.total} ${s.chunks} ${s.closed}');
}
