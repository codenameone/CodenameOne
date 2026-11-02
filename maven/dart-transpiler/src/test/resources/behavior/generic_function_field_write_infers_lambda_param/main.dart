abstract class Entity { String get id; }
class User implements Entity {
  final String id; final String name;
  User(this.id, this.name);
  String toString() => 'User($id,$name)';
}
class Repo<T extends Entity> {
  void Function(T)? onSave;
  int Function(T, int)? scorer;
  void save(T t) { onSave?.call(t); }
}
void main() {
  final r = Repo<User>();
  r.onSave = (u) => print('saved ${u.name}');
  r.save(User('1', 'ann'));
  r.onSave = (User u) { print('block ${u.name.length}'); };
  r.save(User('2', 'bob'));
  r.scorer = (u, k) => u.name.length * k;
  print(r.scorer!(User('3', 'carla'), 2));
  dynamic dr = r;
  try { (dr as Repo<Entity>).onSave = (Entity e) => print(e); print('setcb ok'); } catch (e) { print('setcb ${e is TypeError}'); }
  Repo<Entity> re = Repo<Entity>();
  re.onSave = (e) => print('entity ${e.id}');
  re.save(User('4', 'dan'));
  try { Repo<Entity> cov = r; cov.onSave!(User('5', 'eve')); print('call ok'); } catch (e) { print('call ${e is TypeError}'); }
}
