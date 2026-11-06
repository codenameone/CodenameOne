T maxOf<T extends Comparable<T>>(T a, T b) => a.compareTo(b) >= 0 ? a : b;
class Box { T pick<T extends Comparable<T>>(T a, T b) => a.compareTo(b) <= 0 ? a : b; }
void main() {
  dynamic f = maxOf;
  try { print(f(1, 2)); } catch (e) { print('err0 ${e is TypeError}'); }
  try { print(f<num>(1, 2)); } catch (e) { print('err1 ${e is TypeError}'); }
  try { print(f<Object>(1, 2)); } catch (e) { print('err1b ${e is TypeError}'); }
  dynamic m = Box().pick;
  try { print(m(3, 2)); } catch (e) { print('err2 ${e is TypeError}'); }
  try { print(m<num>(3, 2)); } catch (e) { print('err3 ${e is TypeError}'); }
  print(maxOf<num>(1, 5));
}
