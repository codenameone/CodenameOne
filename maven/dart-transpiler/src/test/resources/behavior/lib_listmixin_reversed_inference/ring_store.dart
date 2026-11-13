class Store<E> {
  List<E?> _slots = <E?>[];
  int get size => _slots.length;
  void put(E v) => _slots.add(v);
  E? at(int i) => _slots[i];
  bool sameSlots(Store<E> other) => identical(_slots, other._slots);
}
