// Covariant-override bridges against the runtime's own base classes. The bridge
// is only emitted for a base parameter of a concrete type: a RestorableProperty<T>
// subclass overriding initWithValue(T) as initWithValue(Set<String>) is already a
// Java override by generic substitution, and a bridge taking Object failed to
// compile (two methods with the same erasure) -- the gallery's
// _RestorableCheckedValues. A narrowed updateShouldNotify must be what the base
// signature runs.
import 'package:flutter/material.dart';

class _RestorableNames extends RestorableProperty<Set<String>> {
  Set<String> _names = <String>{};

  @override
  Set<String> createDefaultValue() => <String>{'default'};

  @override
  void initWithValue(Set<String> value) {
    _names = value;
  }

  @override
  Set<String> fromPrimitives(Object? data) => <String>{'restored'};

  @override
  Object toPrimitives() => _names.length;
}

class PageStatus extends InheritedWidget {
  final int v;
  PageStatus({required this.v, required Widget child}) : super(child: child);

  @override
  bool updateShouldNotify(PageStatus old) => old.v != v;
}

void main() {
  RestorableProperty<Set<String>> p = _RestorableNames();
  p.initWithValue(<String>{'a', 'b'});
  print(p.toPrimitives());
  InheritedWidget a = PageStatus(v: 1, child: const SizedBox());
  InheritedWidget b = PageStatus(v: 1, child: const SizedBox());
  InheritedWidget c = PageStatus(v: 2, child: const SizedBox());
  print(a.updateShouldNotify(b));
  print(c.updateShouldNotify(a));
}
