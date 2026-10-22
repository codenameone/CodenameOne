// PopScope, WillPopScope and Dismissible compile with typed callbacks: a callback
// parameter typed dynamic could not reach direction.name at run time.
import 'package:flutter/material.dart';

void main() {
  final log = <String>[];
  final page = PopScope<String>(
    canPop: false,
    onPopInvokedWithResult: (didPop, result) => log.add('pop $didPop $result'),
    child: WillPopScope(
      onWillPop: () async => log.isEmpty,
      child: Dismissible(
        key: const ValueKey('row'),
        onDismissed: (direction) => log.add('gone ${direction.name}'),
        confirmDismiss: (direction) async => direction == DismissDirection.endToStart,
        child: const Text('row'),
      ),
    ),
  );
  print(page is PopScope);
  print(log.length);
}
