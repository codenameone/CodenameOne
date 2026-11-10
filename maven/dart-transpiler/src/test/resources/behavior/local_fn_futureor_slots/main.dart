import 'dart:async';

class Holder<R> {
  final FutureOr Function()? onCancel;
  final List<String> log = [];
  Holder({this.onCancel});
  void complete(R value) => log.add('value $value');
  void fail(Object e, StackTrace s) => log.add('error $e');
  Future<void> cancel() async {
    final f = onCancel;
    if (f != null) await f();
    log.add('cancelled');
  }
}

class Job<T> {
  final T seed;
  Job(this.seed);

  Holder<R> thenJob<R>(FutureOr<void> Function(T, Holder<R>) onValue,
      {FutureOr<void> Function(Object, StackTrace, Holder<R>)? onError}) {
    final h = Holder<R>();
    onValue(seed, h);
    if (onError != null) onError('nope', StackTrace.current, h);
    return h;
  }

  Holder<R> then<R>(FutureOr<R> Function(T) onValue,
          {FutureOr<R> Function(Object, StackTrace)? onError}) =>
      thenJob<R>((value, holder) {
        holder.complete(onValue(value) as R);
      },
          onError: onError == null
              ? null
              : (error, stackTrace, holder) {
                  holder.complete(onError(error, stackTrace) as R);
                });
}

Future<void> main() async {
  var done = false;
  Future<void> stopAll() {
    done = true;
    return Future.value();
  }

  final holder = Holder<int>(onCancel: stopAll);
  await holder.cancel();
  print('done $done ${holder.log}');

  final results = <String>[];
  stopAll().whenComplete(() => results.add('completed'));
  await Future.delayed(Duration.zero);
  print(results);

  final job = Job<int>(20);
  print(job.then((v) => v + 1).log);
  print(job.then<String>((v) => 'v$v', onError: (e, s) => 'handled $e').log);
}
