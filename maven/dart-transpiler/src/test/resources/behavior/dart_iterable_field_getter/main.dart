// A class can satisfy an interface's GETTER with a plain FIELD, and Dart makes
// no distinction between the two — `it.current` reads the same either way.
//
// The Java emitter does distinguish: a stub interface's `E get current` becomes
// the method `current()`, while a program class's field `current` becomes a
// private field with `get$current()` accessors. A class that supplies the
// property as a field therefore never implements the interface method, and Java
// keeps the interface's default. Nothing fails to compile and nothing throws —
// the iteration simply hands back the default forever.
//
// This is exactly how the 2D-transformations demo lost its board: `Board with
// IterableMixin` walked its points correctly and yielded null for every one.

import 'dart:collection' show IterableMixin;

class Cell {
  const Cell(this.label);
  final String label;
}

class _CellIterator implements Iterator<Cell?> {
  _CellIterator(this.cells);

  final List<Cell> cells;
  int? index;

  // A FIELD, not a getter — the whole point of this fixture.
  @override
  Cell? current;

  @override
  bool moveNext() {
    index = index == null ? 0 : index! + 1;
    if (index! >= cells.length) {
      current = null;
      return false;
    }
    current = cells[index!];
    return true;
  }
}

class Grid extends Object with IterableMixin<Cell?> {
  Grid(this._cells);

  final List<Cell> _cells;

  @override
  Iterator<Cell?> get iterator => _CellIterator(_cells);
}

void main() {
  final Grid grid = Grid(<Cell>[const Cell('a'), const Cell('b'), const Cell('c')]);

  // Reached through the interface's own protocol.
  final Iterator<Cell?> it = grid.iterator;
  final List<String> direct = <String>[];
  while (it.moveNext()) {
    direct.add(it.current!.label);
  }
  print('direct ${direct.join(",")}');

  // Reached through IterableMixin, which is what the demo's painter uses: a
  // named function with a declared parameter type, exactly as the board's
  // painter passes `drawBoardPoint` to `board.forEach`.
  final List<String> visited = <String>[];
  void visit(Cell? c) {
    visited.add(c!.label);
  }

  grid.forEach(visit);
  print('forEach ${visited.join(",")}');

  print('length ${grid.length}');
}
