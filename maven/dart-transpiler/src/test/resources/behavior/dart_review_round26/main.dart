// emitLazyTopLevel used to mark a lazy top-level/static variable "ready" BEFORE
// running its initialiser, so a recursive read during initialisation silently
// read the Java zero value instead of Dart's real behaviour:
//  - non-late: reading a top-level/static variable while its own initialiser is
//    still running is a cyclic-initialisation error.
//  - late (with an initialiser): a re-entrant read re-runs the initialiser
//    instead of throwing, and `late final` additionally rejects a second
//    completed store.
// An initialiser that throws for an unrelated reason must still leave the
// variable unbound so the next read tries again -- that part was already
// correct and must stay correct.

int a = b + 1;
int b = a + 1;

void cyclicTopLevel() {
  try {
    print(a);
  } catch (e) {
    print('caught1: $e');
  }
  try {
    print(a);
  } catch (e) {
    print('caught2: $e');
  }
}

int counter = 0;
late int i = counter < 10 ? (++counter + i) : 1;

void lateReentrant() {
  print('i=$i counter=$counter');
}

int c2 = 0;
int touchJ() {
  c2++;
  return j;
}

late final int j = c2 < 1 ? touchJ() : 99;

void lateFinalReentrant() {
  try {
    print(j);
  } catch (e) {
    print('caught: $e');
  }
  print('j=$j c2=$c2');
}

int flakyAttempts = 0;
int flaky() {
  flakyAttempts++;
  if (flakyAttempts == 1) {
    throw StateError('boom');
  }
  return 7 * flakyAttempts;
}

final int retried = flaky();

void throwingInitRetries() {
  try {
    print(retried);
  } catch (e) {
    print('first read: $e');
  }
  print(retried);
  print('flakyAttempts=$flakyAttempts');
}

void main() {
  cyclicTopLevel();
  lateReentrant();
  lateFinalReentrant();
  throwingInitRetries();
}
