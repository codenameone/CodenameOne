# Python vs Codename One benchmark

The kernels under `kernels/` run twice: by CPython, and transpiled to Java by
the Codename One Python transpiler and run against `codenameone-python-runtime`.

```
kernels/*.py      one workload each; _harness.py times it and prints the lines below
run_bench.py      builds, measures, renders and gates
baseline/         the gate's ratios, seeded from CI (absent until the first fold)
```

Each kernel prints `RESULT <name> <value>` and `BENCH <name> <microseconds>`.
The RESULT must be identical on both sides or the run fails: a faster
program that computes something else is a bug, not a speed-up. BENCH is the
fastest of several rounds timed inside the program, so it excludes start-up;
the harness separately records the whole process's wall time and its peak
resident memory.

The kernels are our own implementations of the classic algorithms (n-body,
spectral norm, fannkuch, the Richards scheduler) plus a text-processing
workload. They avoid generators and other constructs the transpiler does not
support yet.

## Running it

```bash
source tools/env.sh
cd maven
mvn -Dmaven.repo.local=$(pwd)/../.m2-repo -pl core,python-transpiler -am install -DskipTests
JAVA_HOME=$JAVA17_HOME mvn -Dmaven.repo.local=$(pwd)/../.m2-repo -pl python-runtime,python-transpiler install
cd ..
python3 scripts/python-bench/run_bench.py --java-home "$JAVA17_HOME" \
    --runtime-jar maven/python-runtime/target/codenameone-python-runtime-8.0-SNAPSHOT.jar \
    --core-jar .m2-repo/com/codenameone/codenameone-core/8.0-SNAPSHOT/codenameone-core-8.0-SNAPSHOT.jar \
    --transpiler-cp maven/python-transpiler/target/classes
```

`--kernel nbody` runs one kernel; `--rounds` sets the rounds per side.

## The gate

`--check` compares each kernel's ratio ours/CPython -- compute time, process
wall time, peak memory -- with `baseline/ratios.json` and fails when one grows
past its tolerance. Ratios depend on the machine and the interpreter, so the
baseline comes from the CI runner, never a laptop: download the
`python-bench-report` artifact and run

```bash
python3 scripts/python-bench/run_bench.py --fold python-bench.json
```

Until that file exists `--check` only reports. CPython is pinned in the
workflow (`CPYTHON_VERSION`); moving it is a deliberate re-baseline.

## What is measured today

Only the JVM arm, which is what the simulator and the desktop targets run.
Its memory figure is the JVM's own footprint, which dominates a small kernel,
so read it as a ceiling rather than a device number. The ParparVM, Android
and JavaScript arms join as the Python support reaches them.
