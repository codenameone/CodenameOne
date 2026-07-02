// Generated from docs/developer-guide source blocks. Edit the guide snippets here, not inline.

// tag::apple-wallet-extension-java-001[]
import com.codename1.payment.WalletExtension;
import com.codename1.payment.WalletPassEntry;

if (WalletExtension.isSupported()) {
    WalletExtension.setPassEntries(new WalletPassEntry[] {
        new WalletPassEntry()
            .identifier(card.getPrimaryAccountIdentifier())
            .title("My Bank Debit Card")
            .cardholderName(user.getFullName())
            .primaryAccountSuffix(card.getLast4())
            .paymentNetwork("Visa")
            .localizedDescription("My Bank Debit Card")
            .artPng(cardArt.getImageData())
    });
    WalletExtension.setRemotePassEntries(sameEntries); // Apple Watch list
    WalletExtension.setAuthToken(session.getToken());
    WalletExtension.setRequiresAuthentication(false);
}
// end::apple-wallet-extension-java-001[]
