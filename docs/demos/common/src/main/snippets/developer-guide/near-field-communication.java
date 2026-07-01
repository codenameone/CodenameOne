// Generated from docs/developer-guide source blocks. Edit the guide snippets here, not inline.

// tag::near-field-communication-java-001[]
import com.codename1.nfc.Nfc;
import com.codename1.nfc.NfcReadOptions;
import com.codename1.nfc.NdefMessage;

Nfc nfc = Nfc.getInstance();
if (!nfc.canRead()) {
    // no NFC hardware, or it is disabled in system settings
    return;
}
nfc.readNdef(new NfcReadOptions()
        .setNdefOnly(true)
        .setAlertMessage("Hold near the poster"))
   .onResult((NdefMessage msg, Throwable err) -> {
        if (err != null) {
            return;
        }
        String url = msg.getFirstRecord().getUriPayload();
        Display.getInstance().execute(url);
   });
// end::near-field-communication-java-001[]

// tag::near-field-communication-java-002[]
import com.codename1.nfc.NdefMessage;
import com.codename1.nfc.NdefRecord;

NdefMessage msg = new NdefMessage(
        NdefRecord.createUri("https://codenameone.com"),
        NdefRecord.createText("en", "Codename One"));
Nfc.getInstance().writeNdef(
        new NfcReadOptions().setAlertMessage("Tap a writable tag"),
        msg)
   .onResult((Boolean ok, Throwable err) -> {
       if (err != null) {
           NfcError code = ((NfcException) err).getError();
           // READ_ONLY, CAPACITY_EXCEEDED, INVALID_NDEF, TAG_LOST, ...
       }
   });
// end::near-field-communication-java-002[]

// tag::near-field-communication-java-003[]
nfc.readTag(new NfcReadOptions()
        .setTechFilter(TagType.ISO_DEP)
        .setIsoSelectAids(myAid))
   .onResult((Tag tag, Throwable err) -> {
        if (err != null) return;
        IsoDep iso = tag.getIsoDep();
        if (iso == null) return;
        iso.transceive(myCommandApdu).onResult((byte[] resp, Throwable e) -> {
            if (ApduResponse.isSuccess(resp)) {
                byte[] body = ApduResponse.body(resp);
                // application-specific parsing
            }
        });
   });
// end::near-field-communication-java-003[]

// tag::near-field-communication-java-004[]
new NfcReadOptions()
    .setTechFilter(TagType.NFC_F)
    .setFelicaSystemCodes("0003", "8008");
// end::near-field-communication-java-004[]

// tag::near-field-communication-java-005[]
class MyService extends HostCardEmulationService {
    @Override
    public String[] getAids() {
        return new String[] { "F0010203040506" };
    }
    @Override
    public byte[] processCommand(byte[] apdu) {
        if (apdu.length > 1 && apdu[1] == (byte) 0xA4) {
            // SELECT -- terminal has just routed an APDU to our AID
            return ApduResponse.withStatus(
                    new byte[] { 'O', 'K' },
                    ApduResponse.swSuccess());
        }
        return ApduResponse.swInsNotSupported();
    }
}

Nfc.getInstance().registerHostCardEmulationService(new MyService());
// end::near-field-communication-java-005[]
