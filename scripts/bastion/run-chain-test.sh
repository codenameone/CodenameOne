#!/usr/bin/env bash
#
# Real Ether through the bank: starts a development Ethereum node (geth
# --dev, in a container), points the server at it and runs
# backend/.../chain/ChainIntegrationTest -- a deposit credited from the chain,
# swept into the hot wallet, a withdrawal signed here, broadcast and
# confirmed, the books checked against the chain, and a withdrawal held when
# the ledger owes more than the chain holds. The offline known-answer tests
# (ChainVectorTest) run with it.
#
#   run-chain-test.sh
#
# Needs docker or podman, JAVA_HOME on JDK 17 or newer and the Codename One
# artifacts in the local Maven repository (MAVEN_OPTS reaches the build, for
# -Dmaven.repo.local). The node is removed on exit.
set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
PORT="${BASTION_CHAIN_PORT:-18545}"
IMAGE="${BASTION_GETH_IMAGE:-docker.io/ethereum/client-go:v1.14.12}"
NAME="bastion-chain-test-$$"
ENGINE="$(command -v docker || command -v podman || true)"
[ -n "$ENGINE" ] || { echo "run-chain-test: docker or podman is needed" >&2; exit 1; }

cleanup() { "$ENGINE" rm -f "$NAME" >/dev/null 2>&1 || true; }
trap cleanup EXIT

"$ENGINE" run -d --name "$NAME" -p "127.0.0.1:$PORT:8545" "$IMAGE" \
    --dev --dev.period 1 --http --http.addr 0.0.0.0 --http.port 8545 \
    --http.api eth,net,web3 >/dev/null
for _ in $(seq 1 60); do
  if curl -s -X POST -H 'Content-Type: application/json' \
      --data '{"jsonrpc":"2.0","id":1,"method":"eth_chainId","params":[]}' \
      "http://127.0.0.1:$PORT" | grep -q result; then
    break
  fi
  sleep 1
done

# A throwaway seed, new every run: this chain and its coins vanish on exit.
export BASTION_CHAIN_RPC_URL="http://127.0.0.1:$PORT"
export BASTION_CHAIN_ID=1337
export BASTION_CHAIN_CONFIRMATIONS=2
export BASTION_CHAIN_SEED="$(od -An -tx1 -N32 /dev/urandom | tr -d ' \n')"
export BASTION_CHAIN_PERIOD_MS=3600000

cd "$HERE"
mvn -B -q -Dcodename1.platform=backend -pl backend -am test \
    -Dtest='ChainVectorTest,ChainIntegrationTest' -Dsurefire.failIfNoSpecifiedTests=false
echo "run-chain-test: passed"
