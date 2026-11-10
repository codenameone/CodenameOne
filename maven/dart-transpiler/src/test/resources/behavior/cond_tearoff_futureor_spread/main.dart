import 'dart:async';

class Fwd<T> {
  final String name;
  final bool live;
  Fwd(this.name, this.live);
  Future<void>? forward() => live ? Future.value() : null;
}

Future<void>? _forward(Fwd<Object?> f) => f.forward();

class Sink2 {
  final List<String> log = [];
  void completeError(Object e, [StackTrace? s]) => log.add('plain $e');
}

class Runner {
  final FutureOr<Object?> Function()? onCancel;
  final List<Fwd<Object?>>? forwarders;
  Runner(this.onCancel, this.forwarders);

  Future<Object?> invoke() async {
    final FutureOr<Object?> toReturn = onCancel?.call();
    final isFuture = toReturn is Future;
    final futures = <Future<Object?>>[
      if (isFuture) toReturn,
      ...?forwarders?.map(_forward).nonNulls,
    ];
    final results = futures.isNotEmpty ? await Future.wait(futures) : const [];
    return isFuture ? results.first : '${results.length} $toReturn';
  }
}

Future<void> route(Future<int> f, Sink2 sink, bool custom) async {
  await f.then<void>((v) => sink.log.add('value $v'),
      onError: !custom
          ? sink.completeError
          : (Object error, StackTrace stack) async {
              sink.log.add('custom $error');
            });
}

enum Court { a, b }

class Picker {
  Court court = Court.a;
  int total = 0;
  void pick(dynamic v) => court = v != null ? v : court;
  void size(bool last, dynamic chunk) {
    int n = last ? chunk.length : 0;
    total += n;
  }
}

Future<void> main() async {
  final pk = Picker();
  pk.pick(null);
  pk.pick(Court.b);
  pk.size(true, [1, 2, 3]);
  pk.size(false, null);
  print('${pk.court} ${pk.total}');
  final Object o = pk.total > 1 ? 'big' : pk.court;
  print(o);
  print(await Runner(() => 'sync', null).invoke());
  print(await Runner(() => Future.value('async'), null).invoke());
  print(await Runner(null, [Fwd('a', true), Fwd('b', false), Fwd('c', true)]).invoke());
  final s = Sink2();
  await route(Future.error('x'), s, false);
  await route(Future.error('y'), s, true);
  await route(Future.value(3), s, true);
  print(s.log);
}
