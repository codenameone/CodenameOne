class Thread<T> { final T t; Thread(this.t); T get() => t; String toString() => 'Thread($t)'; }
class Dyn { String toString() => 'userDyn'; }
class Funcs { static String make() => 'userFuncs'; }
class DartList { int get length => 77; }
mixin Runnable { static int x(int a) => a + 1; }
extension Integer on int { int twice() => this * 2; }
