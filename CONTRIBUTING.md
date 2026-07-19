# Help Improve Codename One

Codename One accepts outside testing, issue reports, benchmark counterexamples,
documentation feedback, and design discussion. External pull requests are
disabled.

A framework change can affect the JavaSE simulator, Android, iOS, JavaScript,
desktop ports, bytecode translation, and screenshot baselines at the same time.
The repository has a large CI matrix to keep those targets aligned. Reviewing
and repairing outside patches across that matrix costs more maintainer time than
implementing the confirmed change directly, so maintainers land the code.

That does not make the repository read-only. A small reproducer or a precise
platform report often finds a problem no maintainer environment could expose.

## Choose the Right Channel

- Use [GitHub Discussions](https://github.com/codenameone/CodenameOne/discussions)
  for usage questions, API design, feature ideas, and proposals that still need
  their scope defined.
- Use [GitHub Issues](https://github.com/codenameone/CodenameOne/issues/new/choose)
  for a reproducible defect, performance counterexample, toolchain or OS
  compatibility problem, and a specific documentation failure.
- Use the [security policy](SECURITY.md) for vulnerabilities. Do not publish a
  security report in an issue or discussion.

## Make a Report Actionable

Include the evidence another developer needs to reproduce the result:

1. The smallest project or code sample that still fails.
2. The Codename One version and JDK version.
3. The affected target, OS or SDK version, and device or simulator.
4. Exact steps, expected behavior, and observed behavior.
5. Complete logs or stack traces as text. Remove credentials and private data.
6. Screenshots or recordings when the problem is visual or timing-dependent.
7. The last known working version when reporting a regression.

Do not attach a full proprietary application when a small project can reproduce
the failure. If the problem cannot be shared publicly, describe the smallest
observable case first and wait for a maintainer to suggest a private support
path.

## Performance Reports

A performance claim needs a workload and a baseline. Include warmup, run count,
hardware, input data, timing method, and the raw results. When comparing two
runtimes or ports, verify that both produce the same output.

Published Codename One benchmarks are open to challenge. A case that produces a
different result is useful even when it disproves a claim.

## Documentation Reports

Link the page or source section and describe what you tried, what you understood
it to mean, and where the instructions stopped matching the current toolchain.
The failure path matters more than a proposed rewrite because it shows which
assumption the documentation left unstated.

## What Happens Next

Maintainers normally classify a new report within three business days. Triage
confirms the channel, requests missing evidence, and assigns a category. It is
not a commitment to a fix date.

Once the behavior is reproducible, a maintainer decides whether it belongs in
the framework, a port, tooling, documentation, or a separate library. The
maintainer implements the change and runs the relevant CI and native test
matrix. Reports that lead to a fix or documented decision receive credit in the
issue and, when the story is useful to other developers, in the release notes or
technical write-up.
