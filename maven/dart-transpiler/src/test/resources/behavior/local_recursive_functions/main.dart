// Local functions that call themselves, directly or through a loop, and a pair
// of local functions where the later one is called by the earlier one.
List<List<int>> components(Map<int, List<int>> graph) {
  var index = 0;
  final indices = <int, int>{};
  final low = <int, int>{};
  final stack = <int>[];
  final result = <List<int>>[];
  void connect(int v) {
    indices[v] = index;
    low[v] = index;
    index++;
    stack.add(v);
    for (final w in graph[v]!) {
      if (!indices.containsKey(w)) {
        connect(w);
        low[v] = low[v]! < low[w]! ? low[v]! : low[w]!;
      } else if (stack.contains(w)) {
        low[v] = low[v]! < indices[w]! ? low[v]! : indices[w]!;
      }
    }
    if (low[v] == indices[v]) {
      final c = <int>[];
      int w;
      do {
        w = stack.removeLast();
        c.add(w);
      } while (w != v);
      result.add(c..sort());
    }
  }

  for (final v in graph.keys) {
    if (!indices.containsKey(v)) connect(v);
  }
  return result;
}

int fib(int n) {
  int go(int k) => k < 2 ? k : go(k - 1) + go(k - 2);
  return go(n);
}

String countdown(int n) {
  final out = StringBuffer();
  void tick(int k) {
    out.write(k);
    if (k > 0) {
      out.write(',');
      tick(k - 1);
    }
  }

  tick(n);
  return out.toString();
}

void main() {
  print(components({1: [2], 2: [3], 3: [1], 4: [5], 5: []}));
  print(fib(10));
  print(countdown(4));
}
