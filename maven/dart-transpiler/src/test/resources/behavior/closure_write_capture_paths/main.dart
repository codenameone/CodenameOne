class Node {
  final String? label;
  final Node? next;
  Node(this.label, [this.next]);
}

String describe(Node n) {
  var cur = n;
  if (cur.label != null && cur.next?.label != null) {
    final before = cur.label!.length;
    void hop() {
      cur = cur.next ?? cur;
    }
    hop();
    final l = cur.label;
    return '$before ${l == null ? 'none' : l.toUpperCase()}';
  }
  return 'skip';
}

String chain(Node n) {
  Node? p = n;
  if (p != null && p.label != null) {
    final f = () {
      p = p?.next;
    };
    f();
    final lab = p?.label;
    return lab ?? 'null-label';
  }
  return 'none';
}

void main() {
  print(describe(Node('ab', Node('cd'))));
  print(describe(Node('ab', Node(null))));
  print(chain(Node('x', Node(null))));
  print(chain(Node('x', Node('y'))));
  print(chain(Node('x')));
}
