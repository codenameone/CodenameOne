#!/usr/bin/env bash
set -euo pipefail

scenario_file="${1:-tests/push-e2e/scenarios.json}"
for required in PUSH_API_BASE PUSH_API_KEY PUSH_APP_ID PUSH_TARGETS_FILE PUSH_RECEIPT_BASE PUSH_RECEIPT_TOKEN; do
    if [[ -z "${!required:-}" ]]; then
        echo "Missing required environment variable: ${required}" >&2
        exit 2
    fi
done
command -v jq >/dev/null || { echo "jq is required" >&2; exit 2; }
command -v curl >/dev/null || { echo "curl is required" >&2; exit 2; }

failures=0
while IFS= read -r scenario; do
    scenario_id="$(jq -r '.id' <<<"${scenario}")"
    correlation="${scenario_id}-$(date +%s)-${RANDOM}"
    message="$(jq -c --arg correlation "${correlation}" '.message + {id:$correlation}' <<<"${scenario}")"
    required_events="$(jq -c '.requiredEvents' <<<"${scenario}")"
    while IFS= read -r provider; do
        target="$(jq -r --arg provider "${provider}" '.[$provider]' "${PUSH_TARGETS_FILE}")"
        request="$(jq -n --arg app "${PUSH_APP_ID}" --arg provider "${provider}" \
            --arg target "${target}" --argjson message "${message}" \
            '{appId:$app,targets:[{provider:$provider,token:$target}],message:$message}')"
        status="$(curl --silent --show-error --output "/tmp/cn1-push-${correlation}.json" \
            --write-out '%{http_code}' --request POST "${PUSH_API_BASE}/api/v3/push/messages" \
            --header "Authorization: Bearer ${PUSH_API_KEY}" \
            --header 'Content-Type: application/json' --data "${request}")"
        if [[ "${status}" != "202" ]]; then
            echo "FAIL ${scenario_id}/${provider}: admission HTTP ${status}" >&2
            failures=$((failures + 1))
            continue
        fi
        complete=false
        for attempt in $(seq 1 60); do
            receipt="$(curl --silent --show-error \
                --header "Authorization: Bearer ${PUSH_RECEIPT_TOKEN}" \
                "${PUSH_RECEIPT_BASE}/receipts/${correlation}/${provider}" || true)"
            if jq -e --argjson required "${required_events}" \
                '([.events[].type] as $actual | all($required[]; . as $event | $actual | index($event)))' \
                <<<"${receipt}" >/dev/null 2>&1; then
                complete=true
                break
            fi
            sleep 5
        done
        if [[ "${complete}" == true ]]; then
            echo "PASS ${scenario_id}/${provider}"
        else
            echo "FAIL ${scenario_id}/${provider}: required receipt events not observed" >&2
            failures=$((failures + 1))
        fi
    done < <(jq -r 'keys[]' "${PUSH_TARGETS_FILE}")
done < <(jq -c '.[]' "${scenario_file}")

exit "${failures}"
