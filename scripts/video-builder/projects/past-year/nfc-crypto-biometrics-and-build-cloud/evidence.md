# Evidence map

Source: `docs/website/content/blog/nfc-crypto-biometrics-and-build-cloud.md`
Canonical: https://www.codenameone.com/blog/nfc-crypto-biometrics-and-build-cloud/

## Thesis

Making NFC, cryptography, and biometrics first-class framework capabilities

## Supported beats

- **A new Build Cloud UI — preview:** The single most visible change this week sits behind the Build Cloud login. The console we have been serving for years is being replaced. The new UI is live now here, alongside the current console you can still find here.
- **Device APIs become first-class:** The bigger structural change this week is that three new APIs that used to live in cn1libs or weren't available at all are now built into the framework core: biometrics, cryptography, and NFC.
- **Biometrics — PR #4987:** Touch ID, Face ID, and Android BiometricPrompt are now in com.codename1.security.Biometrics. The API uses simpler semantics compared to the original fingerprint API (that predated face scanning but didn't rename the API).
- **Cryptography — PR #4994:** Routine cryptography (hashing, MAC, symmetric and asymmetric encryption, signing, JWT, OTP) is now in com.codename1.security and ships with the framework. The pure-Java algorithms (Hash, Hmac, Base32, the JWT and OTP machinery) produce identical output on every supported platform.
- **NFC — PR #4996:** com.codename1.nfc is the third addition. A single Nfc entry point, an NdefMessage / NdefRecord pair with typed factories (createUri, createText, createMime, createExternal, createApplicationRecord), per-technology Tag subclasses (IsoDep, MifareClassic, MifareUltralight, NfcA, NfcB, NfcF, NfcV), and a HostCardEmulationService base class for emulating a contactless card.
- **cn1libs can now own simulator menus — and that changes Bluetooth:** PR #4988 is one of those small-looking changes that opens up a whole category of UX. The Java SE simulator now scans every jar on its classpath for META-INF/codenameone/simulator-hooks.properties and lets any cn1lib contribute its own menu items.

## Referenced evidence

- https://cloud.codenameone.com/console/index.html
- https://cloud.codenameone.com/secure/index.html
- https://github.com/codenameone/CodenameOne/pull/4987
- https://github.com/codenameone/CodenameOne/pull/4994
- https://github.com/codenameone/CodenameOne/pull/4996
- https://github.com/codenameone/CodenameOne/pull/4988
- https://github.com/codenameone/bluetoothle-codenameone/
- https://github.com/codenameone/CodenameOne/pull/4990
- https://github.com/codenameone/CodenameOne/pull/4989
- https://github.com/codenameone/CodenameOne/pull/4980
- https://github.com/codenameone/CodenameOne/issues/3136
- https://github.com/codenameone/CodenameOne/pull/4985
