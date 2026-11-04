typedef Id = int;
typedef Fn = int Function(int);
class Base { List<Id> ids() => [1, 2]; Fn op() => (x) => x * 10; }
mixin M on Base { int twice() => ids().length * 2; }
