// Generated from docs/developer-guide source blocks. Edit the guide snippets here, not inline.

// tag::backend-security-bash-key[]
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out signing.pem
// end::backend-security-bash-key[]

// tag::backend-security-bash-mfa-key[]
openssl rand -base64 32
// end::backend-security-bash-mfa-key[]

// tag::backend-security-bash-apikey[]
curl -H "X-API-Key: cn1_your-key" https://api.example.com/api/deploy/status
curl -H "Authorization: Bearer cn1_your-key" https://api.example.com/api/deploy/status
// end::backend-security-bash-apikey[]

// tag::backend-security-bash-client-credentials[]
curl -u nightly-reports:your-client-secret \
     -d grant_type=client_credentials -d scope=orders:read \
     https://id.example.com/oauth2/token
// end::backend-security-bash-client-credentials[]

// tag::backend-security-bash-device[]
curl -d client_id=acme-app -d "scope=openid orders:read" \
     https://id.example.com/oauth2/device_authorization

curl -d grant_type=urn:ietf:params:oauth:grant-type:device_code \
     -d client_id=acme-app -d device_code=the-device-code \
     https://id.example.com/oauth2/token
// end::backend-security-bash-device[]

// tag::backend-security-bash-apk-hash[]
keytool -exportcert -alias upload -keystore release.keystore \
    | openssl sha256 -binary | openssl base64 | tr '+/' '-_' | tr -d '='
// end::backend-security-bash-apk-hash[]
