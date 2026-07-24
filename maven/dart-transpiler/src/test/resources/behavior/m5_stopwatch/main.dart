void main() {
  final sw = Stopwatch();
  print(sw.isRunning);
  sw.start();
  print(sw.isRunning);
  int x = 0;
  for (int i = 0; i < 1000; i++) {
    x += i;
  }
  sw.stop();
  print(sw.isRunning);
  print(sw.elapsedMicroseconds >= 0);
  print(x);
}
