/// Near Field Communication (NFC) API: read and write NDEF messages,
/// exchange APDUs with contactless smart cards, and host-emulate as a
/// card.
///
/// `Nfc.getInstance()` returns the platform implementation; supporting
/// classes cover NDEF (`NdefMessage`, `NdefRecord`), tag discovery
/// (`Tag`, `TagType`, `TagTechnology`, `NfcListener`,
/// `NfcReadOptions`), specific tag technologies (`IsoDep`,
/// `MifareClassic`, `MifareUltralight`, `NfcA`, `NfcB`, `NfcF`,
/// `NfcV`), APDU exchange (`ApduResponse`) and host card emulation
/// (`HostCardEmulationService`). Failures surface through `NfcError`
/// and `NfcException`.
package com.codename1.nfc;
