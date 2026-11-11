// Generated from docs/developer-guide source blocks. Edit the guide snippets here, not inline.

// tag::commerce-bash-001[]
curl -X POST \
  -H "Authorization: Bearer sk_your_secret_key" \
  -H "Content-Type: application/json" \
  -d '{"expiresDate": 1798761600000, "reason": "support ticket 4411"}' \
  https://cloud.codenameone.com/api/v2/commerce/v1/subscribers/account-1234/entitlements/pro/grant
// end::commerce-bash-001[]
