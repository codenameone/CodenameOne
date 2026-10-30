// Generated from docs/developer-guide source blocks. Edit the guide snippets here, not inline.

// tag::crypto-wallets-bash-001[]
scripts/crypto-bench/jvm-run.sh            # JVM, against BouncyCastle and the JDK
scripts/crypto-bench/native/run.sh         # C, against libsecp256k1 and OpenSSL
scripts/crypto-bench/android-run.sh        # an attached device or emulator
scripts/crypto-gpu/batch-check.sh          # the GPU split, with its crossover
scripts/crypto-gpu/js-batch-check.sh       # WebGPU in headless Chromium
python3 scripts/crypto-bench/gate.py jvm <output>   # hold a run to its floors
// end::crypto-wallets-bash-001[]
