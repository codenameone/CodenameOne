#!/bin/bash
# Proves the local Java SE runtime and the native binary answer the same.
#
# The shared runtime (src/) is one copy of the protocol logic, so it cannot drift
# on its own -- but the per-target impl/ classes underneath it can, and a dev loop
# that behaves differently from production is worse than no dev loop. This runs the
# SAME request script against both and diffs the answers.
#
# Both are exercised over a real socket, not in-process, so what is compared is
# what a client sees: status lines, headers that matter, and bodies.
set -e
cd "$(dirname "$0")"
PORT_JVM="${CN1_PARITY_PORT_JVM:-8471}"
PORT_NATIVE="${CN1_PARITY_PORT_NATIVE:-8472}"
OUT="target/parity"
rm -rf "$OUT"; mkdir -p "$OUT"

# Every response goes through this so the parts that are ALLOWED to differ do not
# register as drift: a JWT carries an issued-at and a random-per-process signing
# secret, and Date is a wall clock.
normalize() {
    sed -E -e 's/eyJ[A-Za-z0-9_-]+\.eyJ[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+/<TOKEN>/g' \
           -e 's/^Date:.*/Date: <MASKED>/' \
           -e 's/"uptimeSeconds":[0-9]+/"uptimeSeconds":<MASKED>/' \
           -e 's/^Server:.*/Server: <MASKED>/' \
           -e 's/\r$//'
}

# One request per line, run in order against whichever server is up. Bodies that
# depend on a token use $TOK, which the script fills in from /login.
probe() {
    local base="$1" out="$2"
    local tok
    tok="$(curl -sS --max-time 10 -X POST "$base/login" -H 'Content-Type: application/json' \
        -d '{"username":"shai","password":"hunter2"}' | tr -d '"')"
    {
        echo "### greet";        curl -sS --max-time 10 "$base/greet/Shai"; echo
        echo "### greet loud";   curl -sS --max-time 10 "$base/greet/Shai?loud=yes"; echo
        echo "### whoami";       curl -sS --max-time 10 "$base/whoami" -H 'X-User: shai' \
                                     -H 'Cookie: session=abc123'; echo
        echo "### login bad";    curl -sS --max-time 10 -X POST "$base/login" \
                                     -H 'Content-Type: application/json' \
                                     -d '{"username":"shai","password":"wrong"}'; echo
        echo "### login shape";  echo "$tok" | cut -c1-3; echo
        echo "### addPet";       curl -sS --max-time 10 -X POST "$base/pet" \
                                     -H 'Content-Type: application/json' \
                                     -d '{"name":"Rex","species":"dog","weight":12.5,"good":true}'; echo
        echo "### getPet";       curl -sS --max-time 10 "$base/pet/1"; echo
        echo "### getPet 404";   curl -sS --max-time 10 -o /dev/null -w '%{http_code}\n' "$base/pet/999"
        echo "### bulk";         curl -sS --max-time 10 -X POST "$base/pets/bulk" \
                                     -H "Authorization: Bearer $tok" \
                                     -H 'Content-Type: application/json' \
                                     -d '[{"name":"Mia","species":"cat"},{"name":"Bo","species":"dog"}]'; echo
        echo "### bulk no auth"; curl -sS --max-time 10 -X POST "$base/pets/bulk" \
                                     -H 'Content-Type: application/json' -d '[]'; echo
        echo "### bulk bad tok"; curl -sS --max-time 10 -X POST "$base/pets/bulk" \
                                     -H "Authorization: Bearer ${tok%?}X" \
                                     -H 'Content-Type: application/json' -d '[]'; echo
        echo "### listPets";     curl -sS --max-time 10 "$base/pets?species=dog"; echo
        echo "### echo nested";  curl -sS --max-time 10 -X POST "$base/echo" \
                                     -H 'Content-Type: application/json' \
                                     -d '{"name":"Rex","species":"dog","weight":1.5,"good":true,"tags":[{"label":"friendly","weight":3},{"label":"loud","weight":1}]}'; echo
        echo "### echo bad nested"; curl -sS --max-time 10 -X POST "$base/echo" \
                                     -H 'Content-Type: application/json' \
                                     -d '{"name":"Rex","tags":["not-an-object",7]}'; echo
        echo "### echo array body"; curl -sS --max-time 10 -X POST "$base/echo" \
                                     -H 'Content-Type: application/json' -d '[1,2,3]'; echo
        echo "### bulk not array"; curl -sS --max-time 10 -X POST "$base/pets/bulk" \
                                     -H "Authorization: Bearer $tok" \
                                     -H 'Content-Type: application/json' -d '{"name":"x"}'; echo
        echo "### photo set";    curl -sS --max-time 10 -X POST "$base/pet/1/photo" \
                                     --data-binary 'aGVsbG8='; echo
        echo "### photo get";    curl -sS --max-time 10 "$base/pet/1/photo"; echo
        echo "### delete";       curl -sS --max-time 10 -X DELETE "$base/pet/2" \
                                     -H "Authorization: Bearer $tok"; echo
        echo "### list after";   curl -sS --max-time 10 "$base/pets"; echo
        echo "### healthz";      curl -sS --max-time 10 "$base/healthz"; echo
        echo "### unknown";      curl -sS --max-time 10 -o /dev/null -w '%{http_code}\n' "$base/nope"
        echo "### bad method";   curl -sS --max-time 10 -o /dev/null -w '%{http_code}\n' \
                                     -X PUT "$base/pet/1"
        echo "### bad json";     curl -sS --max-time 10 -X POST "$base/pet" \
                                     -H 'Content-Type: application/json' -d '{not json'; echo
        echo "### keepalive";    curl -sS --max-time 10 "$base/greet/a" "$base/greet/b"; echo
        echo "### headers";      curl -sS --max-time 10 -D - -o /dev/null "$base/pet/1"
    } 2>&1 | normalize > "$out"
}

wait_for() {
    local base="$1" tries=0
    while [ "$tries" -lt 100 ]; do
        if curl -sS --max-time 2 -o /dev/null "$base/pets" 2>/dev/null; then return 0; fi
        tries=$((tries + 1))
        sleep 0.2
    done
    echo "server never came up at $base"; return 1
}

DB_JVM="$(mktemp "${TMPDIR:-/tmp}/cn1parity-jvm.XXXXXX")"
DB_NATIVE="$(mktemp "${TMPDIR:-/tmp}/cn1parity-native.XXXXXX")"
rm -f "$DB_JVM" "$DB_NATIVE"

CN1_BACKEND_DEMO=demo/petserver CN1_PORT="$PORT_JVM" CN1_DB_PATH="$DB_JVM" \
    ./run-javase.sh com.demo.PetServer > "$OUT/jvm.log" 2>&1 &
JVM_PID=$!
trap 'kill $JVM_PID 2>/dev/null; kill $NATIVE_PID 2>/dev/null' EXIT

# Rebuilt every run by default. A binary left over from an earlier tree would
# make this compare today's Java SE runtime against last week's native one and
# call the agreement proof of anything. CN1_PARITY_REUSE_BINARY=1 keeps it while
# iterating on the Java SE side.
if [ "${CN1_PARITY_REUSE_BINARY:-0}" != "1" ] || [ ! -x target/petserver-native ]; then
    CN1_BACKEND_DEMO=demo/petserver ./build.sh PetServer com.demo target/petserver-native
fi
CN1_PORT="$PORT_NATIVE" CN1_DB_PATH="$DB_NATIVE" \
    ./target/petserver-native > "$OUT/native.log" 2>&1 &
NATIVE_PID=$!

wait_for "http://127.0.0.1:$PORT_JVM"
wait_for "http://127.0.0.1:$PORT_NATIVE"
probe "http://127.0.0.1:$PORT_JVM" "$OUT/jvm.txt"
probe "http://127.0.0.1:$PORT_NATIVE" "$OUT/native.txt"

if diff -u "$OUT/native.txt" "$OUT/jvm.txt" > "$OUT/diff.txt"; then
    echo "PARITY OK -- $(grep -c '^###' "$OUT/jvm.txt") probes identical on both runtimes"
else
    echo "PARITY FAILED -- the two runtimes answered differently:"
    cat "$OUT/diff.txt"
    exit 1
fi
