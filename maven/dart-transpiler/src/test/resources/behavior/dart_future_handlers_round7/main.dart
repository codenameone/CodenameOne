// then's named onError handles the source failure; catchError's named test
// decides which failures the handler sees.

Future<int> fails() async {
  throw StateError('source failed');
}

void main() async {
  final handled = await fails().then((v) => 'value', onError: (e) => 'handled');
  print(handled);

  final selective = await fails()
      .catchError((e) => -1, test: (e) => e is ArgumentError)
      .catchError((e) => -2, test: (e) => e is StateError);
  print(selective);

  try {
    await Future.value(1).then((v) {
      throw ArgumentError('from onValue');
    }, onError: (e) => 'not me');
  } catch (e) {
    print('onValue error escaped');
  }
}
