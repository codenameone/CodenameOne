// A top-level `_v` beside a member `_v`: only the member references are renamed, by
// lexical resolution.
int _v = 7;
int _w = 8;
int _tick() => 9;

class LB {
  int _v = 700;
  int _w = 800;
  int _tick() => 900;

  List<int> scopes(bool flag) {
    final out = <int>[];
    final early = () => _v;          // the member: the local below is not in scope here
    out.add(_v);                     // the member
    if (flag) {
      var _v = 5;                    // a local, in this block only
      out.add(_v);
      out.add(this._v);
    } else {
      out.add(_v);                   // the member: a sibling block
    }
    {
      out.add(_v);                   // the member again
      int _w = 6;
      out.add(_w + _v);
    }
    out.add(_w);                     // the member
    for (var _v = 0; _v < 2; _v++) {
      out.add(_v);                   // the loop variable
    }
    out.add(_v);                     // the member
    try {
      throw 4;
    } catch (_v) {
      out.add(_v as int);            // the catch variable
    }
    out.add(early());
    int _tick() => 1;                // a local function
    out.add(_tick());
    out.add(this._tick());
    final late = (int _v) => _v * 2; // a closure parameter
    out.add(late(21));
    out.add(_v);
    return out;
  }
}

Symbol symbolB() => #_v;
Symbol symbolDollar() => #_name$p3;
int topLevel() => _v + _w + _tick();

void spyB(dynamic s) {
  s._v;
  s._v = 1;
  s._tick();
  s._name$p3;
  s._name$p3 = 2;
}
