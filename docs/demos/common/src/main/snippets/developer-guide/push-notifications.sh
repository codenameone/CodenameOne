// Generated from docs/developer-guide source blocks. Edit the guide snippets here, not inline.

// tag::push-notifications-bash-001[]
curl -X POST https://cloud.codenameone.com/api/v3/push/messages \
  -H 'Authorization: Bearer YOUR_SERVER_API_KEY' \
  -H 'Content-Type: application/json' \
  -d '{
    "appId": "APPLICATION_ID",
    "targets": [
      {"provider":"fcm", "token":"OPAQUE_DEVICE_TOKEN"},
      {"provider":"apns", "token":"OPAQUE_DEVICE_TOKEN"}
    ],
    "message": {
      "schema":3,
      "title":"Order shipped",
      "body":"Order 4815 is on its way",
      "data":{"orderId":"4815"}
    }
  }'
// end::push-notifications-bash-001[]
