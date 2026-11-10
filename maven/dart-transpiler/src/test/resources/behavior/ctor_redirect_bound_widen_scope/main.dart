import 'ctx_lib.dart' as ctx;
import 'span_lib.dart';

class Base {
  final int code;
  final int? length;
  final String? note;
  Base(this.code, {this.length, this.note = 'base-default'});
}

// An unnamed constructor redirecting to a named one that forwards super parameters.
class Reply extends Base {
  final String body;
  Reply(String body, int code, {String? note})
      : this.raw(body.codeUnits, code, note: note);
  Reply.raw(List<int> units, super.code, {super.note})
      : body = String.fromCharCodes(units),
        super(length: units.length);
}

class Plain {
  final int a;
  final int b;
  Plain(int x) : this.pair(x, x * 2);
  Plain.pair(this.a, this.b);
}

abstract class Writer<T> {
  String write(String tag, T value);
}

class Stamp {
  final int ms;
  Stamp(this.ms);
}

class LateStamp extends Stamp {
  LateStamp(super.ms);
}

// The override widens T to its bound.
class StampWriter<T extends Stamp> extends Writer<T> {
  @override
  String write(String tag, Stamp value) => '$tag:${value.ms}';
}

void main() {
  final r = Reply('hey', 201, note: 'n');
  print('${r.body} ${r.code} ${r.length} ${r.note}');
  final r2 = Reply('abcd', 404);
  print('${r2.body} ${r2.code} ${r2.length} ${r2.note}');
  final p = Plain(3);
  print('${p.a} ${p.b}');
  Writer<LateStamp> w = StampWriter<LateStamp>();
  print(w.write('t', LateStamp(42)));
  print(StampWriter<Stamp>().write('s', Stamp(7)));
  final s = Span('abc', 'own');
  print(s.upper().describe());
  print(ctx.label);
}
