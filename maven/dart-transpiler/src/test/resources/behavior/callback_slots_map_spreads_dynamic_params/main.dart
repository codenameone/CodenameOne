// Callbacks in declared slots: a void closure with named parameters returned as a
// void typedef, a map literal built from a spread indexed with `!`, a map of void
// callbacks, and untyped closure parameters taking a dynamic callback's argument.

typedef Frame = void Function(Map<String, String>);
typedef Stop = void Function({Map<String, String>? extra});
typedef AnyCb = void Function(dynamic);

enum Court { indoor, outdoor }

class Hub {
  final _watchers = <String, Frame>{};
  final List<String> log = [];
  bool open = true;
  int _next = 0;

  Stop watch({required String topic, required Frame callback, Map<String, String>? headers}) {
    final h = {
      ...?headers,
      'topic': topic,
    };
    if (!h.containsKey('id')) {
      h['id'] = 'w${_next++}';
    }
    _watchers[h['id']!] = callback;
    return ({Map<String, String>? extra}) {
      if (!open) return;
      final x = {...?extra};
      if (!x.containsKey('id')) {
        x['id'] = h['id']!;
      }
      _watchers.remove(x['id']);
      log.add('stopped ${x['id']}');
    };
  }

  void fire(String id, Map<String, String> m) {
    _watchers[id]!(m);
  }
}

class Picker {
  Court _court = Court.indoor;
  Set<Court> _courts = {Court.outdoor};
  void run(void Function(void Function()) setState, void Function(AnyCb, AnyCb) bind) {
    bind((v) => setState(() => _court = v ?? _court), (s) => setState(() => _courts = s));
  }
}

void main() {
  final hub = Hub();
  final stop = hub.watch(topic: 't', callback: (m) => hub.log.add('got ${m['k']}'), headers: {'a': 'b'});
  hub.fire('w0', {'k': 'v'});
  stop();
  hub.log.add('left ${hub._watchers.length}');
  print(hub.log.join('\n'));
  final p = Picker();
  p.run((f) => f(), (a, b) {
    a(null);
    print(p._court);
    a(Court.outdoor);
    print(p._court);
    b(<Court>{Court.indoor});
    print(p._courts);
  });
}
