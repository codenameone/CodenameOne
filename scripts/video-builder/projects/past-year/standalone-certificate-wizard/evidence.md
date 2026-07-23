# Evidence map

Source: `docs/website/content/blog/standalone-certificate-wizard.md`
Canonical: https://www.codenameone.com/blog/standalone-certificate-wizard/

## Thesis

Why App Store Connect API keys remove the brittle Apple ID and 2FA certificate flow

## Supported beats

- **Why The Old Wizard Kept Breaking:** Apple signing needs a pile of interlocking assets: a signing certificate, a bundle ID, registered devices, a provisioning profile that ties the three together, and a push key if you use notifications.
- **The New Approach: A Key, Not A Login:** The new wizard never sees your Apple ID or password. It uses an App Store Connect API key, which is Apple's official machine-to-machine credential: a .p8 private key plus a Key ID and an Issuer ID, created once in App Store Connect under Users and Access, Integrations.
- **Auto Setup:** With the key stored, the toolbar shows Auto Setup. It reads your project's codenameone_settings.properties, takes the package name as the bundle ID, and creates or reuses everything a normal project needs: the bundle ID, development and distribution certificates, development and App Store provisioning profiles, and push enablement.
- **A Standalone Tool, Bound To Your Project:** The wizard used to live inside the Codename One Settings app. It's now its own application, launched from any Codename One Maven project.
- **The Tradeoff:** The cost of the new model: the .p8 key is sent to the Codename One cloud signing service, which performs the Apple API calls on your behalf and doesn't return the key afterward.

## Referenced evidence

- https://github.com/codenameone/CodenameOne/pull/5339

## Independent problem evidence

- App Store Connect API: https://developer.apple.com/help/app-store-connect/get-started/app-store-connect-api — App Store Connect API keys are role-scoped, downloaded once, and revocable from the account.
- Authentication Cheat Sheet: https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html — Modern authentication guidance adds reauthentication and multiple factors to protect human sessions.

## Product proof

- `docs/website/static/blog/standalone-certificate-wizard.jpg`
