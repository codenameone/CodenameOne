// A raw generic class used as a type argument is instantiated to its bounds.
class Relay<T> {
  final T value;
  Relay(this.value);
  Future<String> fire() async => 'relay $value';
}

Future<String>? _fireOne(Relay relay) => relay.value == null ? null : relay.fire();

class Hub {
  List<Relay>? relays;
  Map<String, Relay> named = {};

  Future<List<String>> fireAll() {
    final out = <Future<String>>[];
    final found = relays?.map(_fireOne).nonNulls;
    if (found != null) {
      out.addAll(found);
    }
    return Future.wait(out);
  }
}

Future<void> main() async {
  final h = Hub();
  print(await h.fireAll());
  h.relays = [Relay<int>(1), Relay<String>('b'), Relay<Object?>(null)];
  print(await h.fireAll());
  h.named['x'] = Relay<double>(2.5);
  print(h.named.values.map((r) => r.value).toList());
  print(h.relays!.length);
}
