#!/usr/bin/env bash
set -eu

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
INITIALIZR_CN1LIBS_DIR="$ROOT_DIR/../initializr/cn1libs"
TARGET_CN1LIBS_DIR="$ROOT_DIR/cn1libs"
SOURCE_ZIP="$INITIALIZR_CN1LIBS_DIR/ZipSupport/jars/main.zip"
TARGET_ZIP="$TARGET_CN1LIBS_DIR/ZipSupport/jars/main.zip"

if [ ! -f "$SOURCE_ZIP" ]; then
  echo "Missing source file: $SOURCE_ZIP" >&2
  exit 1
fi

mkdir -p "$TARGET_CN1LIBS_DIR/ZipSupport/jars"

python3 - "$SOURCE_ZIP" "$TARGET_ZIP" <<'PY'
import os
import tempfile
import zipfile
import sys

source_zip = sys.argv[1]
target_zip = sys.argv[2]

old_prefix = "META-INF/codenameone/com.codename1.initializr/initializr-ZipSupport/"
new_prefix = "META-INF/codenameone/com.codename1.tools.skindesigner/skindesigner-ZipSupport/"

with tempfile.NamedTemporaryFile(delete=False) as tmp:
    tmp_path = tmp.name

try:
    with zipfile.ZipFile(source_zip, "r") as src, zipfile.ZipFile(tmp_path, "w") as dst:
        for info in src.infolist():
            data = src.read(info.filename)
            filename = info.filename

            if filename.startswith(old_prefix):
                filename = new_prefix + filename[len(old_prefix):]

            if filename.endswith("codenameone_library_required.properties"):
                text = data.decode("utf-8")
                filtered = []
                for line in text.splitlines():
                    if line.startswith("codename1.arg.java.version="):
                        continue
                    filtered.append(line)
                data = ("\n".join(filtered) + "\n").encode("utf-8")

            out = zipfile.ZipInfo(filename)
            out.date_time = info.date_time
            out.compress_type = info.compress_type
            out.external_attr = info.external_attr
            out.comment = info.comment
            out.extra = info.extra
            out.create_system = info.create_system
            out.create_version = info.create_version
            out.extract_version = info.extract_version
            out.flag_bits = info.flag_bits
            out.internal_attr = info.internal_attr
            dst.writestr(out, data)

    os.replace(tmp_path, target_zip)
finally:
    if os.path.exists(tmp_path):
        os.unlink(tmp_path)
PY

echo "Prepared skindesigner ZipSupport jars/main.zip from scripts/initializr/cn1libs with skindesigner metadata"
