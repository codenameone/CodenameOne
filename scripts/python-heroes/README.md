# Python hero ports

`llm` and Microsoft MarkItDown, installed unmodified from PyPI and built as
Codename One applications. The developer guide's "Case study: Porting llm and
MarkItDown" explains the results.

| Directory | What it is |
|---|---|
| `llm-mobile`, `markitdown-mobile` | The applications: a `requirements.txt` and a new user interface in `src/main.py` |
| `bench/run_heroes.py` | Builds each workload with the production build steps, runs it on the JVM and under CPython, checks both give the same output, and records time, memory and shipped size |
| `bench/workloads` | The two workloads, plain Python that runs unchanged on both sides |
| `bench/harness` | The JVM entry point that boots the headless runtime before a workload |
| `bench/baseline/ratios.json` | The JVM/CPython ratios `--check` holds a run to |
| `tests` | Runners for each project's own test suite, transpiled and under CPython's pytest |

## Run it

```bash
python3 scripts/python-heroes/bench/run_heroes.py \
    --java-home "$JAVA17_HOME" --build-cp <build-engine classpath> \
    --runtime-jar <python-runtime jar> --core-jar <core jar> --run-cp <test classpath> \
    --markitdown-tests <markitdown checkout>/packages/markitdown/tests \
    --llm-tests <llm checkout>/tests --python python3 --check
```

`.github/workflows/python-heroes.yml` shows how each class path is put
together. Add `--workload <name>` to run one workload and `--json <file>` to
write the report. With `MARKITDOWN_DUMP=<dir>` set, the MarkItDown workload
writes each document's Markdown there; on a digest mismatch the driver does
this on both sides and names the files that differ.
