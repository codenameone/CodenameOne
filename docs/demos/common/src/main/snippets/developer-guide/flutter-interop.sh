// Generated from docs/developer-guide source blocks. Edit the guide snippets here, not inline.

// tag::flutter-interop-bash-001[]
python3 scripts/flutter-bench/run_bench.py --list

python3 scripts/flutter-bench/run_bench.py --platform macos \
    --cn1-app     /path/to/Bench.app \
    --flutter-app /path/to/gallery.app \
    --json out/macos.json --markdown out/macos.md
// end::flutter-interop-bash-001[]
