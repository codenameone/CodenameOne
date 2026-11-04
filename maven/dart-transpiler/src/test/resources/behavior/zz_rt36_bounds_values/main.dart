import 'dart:async';
class F<T extends Comparable<T>> {
  final T v;
  F(this.v);
  String toString() => 'F<$T>($v)';
}

class Contra<T extends void Function(T)> {
  final Object? v;
  Contra(this.v);
  String toString() => 'Contra<$T>($v)';
}

class Dep<T extends Object, U extends T> {
  final int v;
  Dep(this.v);
  String toString() => 'Dep<$T, $U>($v)';
}

class Self<T extends Self<T>> {
  final int v;
  Self(this.v);
  String toString() => 'Self<$T>($v)';
}

class Ret<T extends T Function()> {
  final int v;
  Ret(this.v);
  String toString() => 'Ret<$T>($v)';
}

class Mixed<T extends Map<T, int>> {
  final int v;
  Mixed(this.v);
  String toString() => 'Mixed<$T>($v)';
}

class Obj<T extends Object?> {
  final int v;
  Obj(this.v);
  String toString() => 'Obj<$T>($v)';
}

class Fo<T extends FutureOr<T>> { final int v; Fo(this.v); String toString() => 'Fo<$T>($v)'; }
class Nul<T extends Comparable<T>?> { final int v; Nul(this.v); String toString() => 'Nul<$T>($v)'; }
class Top<T extends Object?, U extends Map<T, U>> { final int v; Top(this.v); String toString() => 'Top<$T,$U>($v)'; }
class Cn<T extends void Function(Object? Function(T))> { final int v; Cn(this.v); String toString() => 'Cn<$T>($v)'; }
class Ne<T extends Never Function(T)> { final int v; Ne(this.v); String toString() => 'Ne<$T>($v)'; }
void attempt(String label, Object? Function() body) {
  try {
    print('$label ${body()}');
  } on TypeError {
    print('$label TypeError');
  }
}

void main() {
  dynamic f = F.new;
  attempt('F', () => f(3));
  dynamic c = Contra.new;
  attempt('Contra', () => c(1));
  dynamic d = Dep.new;
  attempt('Dep', () => d(2));
  dynamic s = Self.new;
  attempt('Self', () => s(4));
  dynamic r = Ret.new;
  attempt('Ret', () => r(5));
  dynamic m = Mixed.new;
  attempt('Mixed', () => m(6));
  dynamic o = Obj.new;
  attempt('Obj', () => o(7));
  attempt('apply', () => Function.apply(Contra.new, [8]));

  for (final f in <dynamic>[Fo.new, Nul.new, Top.new, Cn.new, Ne.new]) {
    try { print((f as dynamic)(1)); } on TypeError { print('TypeError'); }
  }
}
