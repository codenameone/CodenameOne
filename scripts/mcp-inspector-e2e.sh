#!/bin/bash
#
# MCP spec conformance end-to-end check.
#
# Builds a minimal headless Codename One app (scripts/mcp/McpStdioDemo.java) that
# exposes itself over the MCP stdio transport, then drives it with the reference MCP
# Inspector CLI (npx @modelcontextprotocol/inspector). A green initialize handshake
# plus successful tools/list and tools/call is the conformance gate.
#
# Requirements: JDK 8 (JAVA_HOME), Maven, Node/npx with network access.
#
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$REPO_ROOT/maven"

: "${JAVA_HOME:?Set JAVA_HOME to a JDK 8 install}"
JAVAC="$JAVA_HOME/bin/javac"
JAVA="$JAVA_HOME/bin/java"

echo "==> Building core + javase and compiling core test classes"
mvn -q -o -pl core install -DskipTests
mvn -q -o -pl javase -Plocal-dev-javase install -DskipTests
mvn -q -o -pl core-unittests test-compile

echo "==> Resolving classpath"
mvn -q -o -pl javase -Plocal-dev-javase dependency:build-classpath -Dmdep.outputFile=/tmp/mcp-e2e-cp.txt
# Freshly built classes come first so they shadow any older snapshot jar the
# dependency graph may resolve; the JavaSE port supplies MCPStdioTransport and the
# test-classes supply the headless TestCodenameOneImplementation.
CP="$REPO_ROOT/maven/core/target/classes:$REPO_ROOT/maven/javase/target/classes:$REPO_ROOT/maven/core-unittests/target/test-classes:$(cat /tmp/mcp-e2e-cp.txt)"

OUT="$(mktemp -d)"
echo "==> Compiling demo server into $OUT"
"$JAVAC" -cp "$CP" -d "$OUT" "$REPO_ROOT/scripts/mcp/McpStdioDemo.java"

cat > "$OUT/server.sh" <<EOF
#!/bin/bash
exec "$JAVA" -cp "$CP:$OUT" McpStdioDemo
EOF
chmod +x "$OUT/server.sh"

echo "==> Inspector: tools/list"
npx -y @modelcontextprotocol/inspector --cli "$OUT/server.sh" --method tools/list

echo "==> Inspector: tools/call ui_snapshot"
npx -y @modelcontextprotocol/inspector --cli "$OUT/server.sh" \
    --method tools/call --tool-name ui_snapshot

echo "==> Inspector: tools/call ui_set_text (drives the TextField)"
npx -y @modelcontextprotocol/inspector --cli "$OUT/server.sh" \
    --method tools/call --tool-name ui_set_text --tool-arg nodeId=6 --tool-arg text=driven

echo "==> MCP conformance E2E passed"
