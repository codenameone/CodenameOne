// Generated from docs/developer-guide source blocks. Edit the guide snippets here, not inline.

// tag::monetization-java-001[]
public static final String SKU_WORLD = "com.codename1.world";
// end::monetization-java-001[]

// tag::monetization-java-002[]
public class HelloWorldIAP implements PurchaseCallback {
    ....

 @Override
 public void itemPurchased(String sku) {
...
 }

 @Override
 public void itemPurchaseError(String sku, String errorMessage) {
...
 }

 @Override
 public void paymentFailed(String paymentCode, String failureReason) {
...
 }

 @Override
 public void paymentSucceeded(String paymentCode, double amount, String currency) {
...
 }

}
// end::monetization-java-002[]

// tag::monetization-java-003[]
 public void start() {
 if(current!= null){
 current.show();
 return;
 }
 Form hi = new Form("Hi World");
 Button buyWorld = new Button("Buy World");
 buyWorld.addActionListener(e->{
 if (Purchase.getInAppPurchase().wasPurchased(SKU_WORLD)) {
 Dialog.show("can't Buy It", "You already Own It", "OK", null);
 } else {
 Purchase.getInAppPurchase().purchase(SKU_WORLD);
 }
 });

 hi.addComponent(buyWorld);
 hi.show();
 }
// end::monetization-java-003[]

// tag::monetization-java-004[]
 @Override
 public void itemPurchased(String sku) {
 ToastBar.showMessage("Thanks. You now own the world", FontImage.MATERIAL_THUMB_UP);
 }

 @Override
 public void itemPurchaseError(String sku, String errorMessage) {
 ToastBar.showErrorMessage("Failure occurred: "+errorMessage);
 }
// end::monetization-java-004[]

// tag::monetization-java-005[]
private static final String NUM_WORLDS_KEY = "NUM_WORLDS.dat";
public int getNumWorlds() {
 synchronized (NUM_WORLDS_KEY) {
 Storage s = Storage.getInstance();
 if (s.exists(NUM_WORLDS_KEY)) {
 return (Integer)s.readObject(NUM_WORLDS_KEY);
 } else {
 return 0;
 }
 }
}

public void addWorld() {
 synchronized (NUM_WORLDS_KEY) {
 Storage s = Storage.getInstance();
 int count = 0;
 if (s.exists(NUM_WORLDS_KEY)) {
 count = (Integer)s.readObject(NUM_WORLDS_KEY);
 }
 count++;
 s.writeObject(NUM_WORLDS_KEY, new Integer(count));
 }
}
// end::monetization-java-005[]

// tag::monetization-java-006[]
buyWorld.addActionListener(e->{
 if (Dialog.show("Confirm", "You own "+getNumWorlds()+
 " worlds. Do you want to buy another one?", "Yes", "No")) {
 Purchase.getInAppPurchase().purchase(SKU_WORLD);
 }
});
// end::monetization-java-006[]

// tag::monetization-java-007[]
@Override
public void itemPurchased(String sku) {
 addWorld();
 ToastBar.showMessage("Thanks. You now own "+getNumWorlds()+" worlds", FontImage.MATERIAL_THUMB_UP);
}
// end::monetization-java-007[]

// tag::monetization-java-008[]
ApplePromotionalOffer offer = new ApplePromotionalOffer();
offer.setOfferIdentifier("my-intro-offer");
offer.setKeyIdentifier("A1B2C3D4");
offer.setNonce(UUID.randomUUID().toString());
offer.setSignature(signatureFromYourServer);
offer.setTimestamp(timestampFromYourServer);

Purchase purchase = Purchase.getInAppPurchase();
purchase.subscribe(SKU_WORLD_MONTHLY, offer);
// end::monetization-java-008[]

// tag::monetization-java-009[]
public static final String SKU_WORLD_1_MONTH = "com.codename1.world.subscribe.1month";
public static final String SKU_WORLD_1_YEAR = "com.codename1.world.subscribe.1year";

public static final String[] PRODUCTS = {
 SKU_WORLD_1_MONTH,
 SKU_WORLD_1_YEAR
};
// end::monetization-java-009[]

// tag::monetization-java-010[]
public void init(Object context) {
...

 Purchase.getInAppPurchase().setReceiptStore(new ReceiptStore() {

 @Override
 public void fetchReceipts(SuccessCallback<Receipt[]> callback) {
 // Fetch receipts from storage and pass them to the callback
 }

 @Override
 public void submitReceipt(Receipt receipt, SuccessCallback<Boolean> callback) {
 // Save a receipt to storage. Make sure to call callback when done.
 }
 });
}
// end::monetization-java-010[]

// tag::monetization-java-011[]
// static declarations used by receipt store

// Storage key where list of receipts are stored
private static final String RECEIPTS_KEY = "RECEIPTS.dat";

@Override
public void fetchReceipts(SuccessCallback<Receipt[]> callback) {
 Storage s = Storage.getInstance();
 Receipt[] found;
 synchronized(RECEIPTS_KEY) {
 if (s.exists(RECEIPTS_KEY)) {
 List<Receipt> receipts = (List<Receipt>)s.readObject(RECEIPTS_KEY);
 found = receipts.toArray(new Receipt[receipts.size()]);
 } else {
 found = new Receipt[0];
 }
 }
 // Make sure this is outside the synchronized block
 callback.onSucess(found);
}
// end::monetization-java-011[]

// tag::monetization-java-012[]
@Override
public void submitReceipt(Receipt receipt, SuccessCallback<Boolean> callback) {
 Storage s = Storage.getInstance();
 synchronized(RECEIPTS_KEY) {
 List<Receipt> receipts;
 if (s.exists(RECEIPTS_KEY)) {
 receipts = (List<Receipt>)s.readObject(RECEIPTS_KEY);
 } else {
 receipts = new ArrayList<Receipt>();
 }
 // Check to see if this receipt already exists
 // This probably won't ever happen (that you will be asked to submit an
 // existing receipt, but better safe than sorry
 for (Receipt r : receipts) {
 if (r.getStoreCode().equals(receipt.getStoreCode()) &&
 r.getTransactionId().equals(receipt.getTransactionId())) {
 // If you've already got this receipt, you will this submission.
 return;
 }
 }

 // Now try to find the current expiry date
 Date currExpiry = new Date();
 List<String> lProducts = Arrays.asList(PRODUCTS);
 for (Receipt r : receipts) {
 if (!lProducts.contains(receipt.getSku())) {
 continue;
 }
 if (r.getCancellationDate()!= null) {
 continue;
 }
 if (r.getExpiryDate() == null) {
 continue;
 }
 if (r.getExpiryDate().getTime() > currExpiry.getTime()) {
 currExpiry = r.getExpiryDate();
 }
 }

 // Now set the appropriate expiry date by adding time onto
 // the end of the current expiry date
 Calendar cal = Calendar.getInstance();
 cal.setTime(currExpiry);
 switch (receipt.getSku()) {
 case SKU_WORLD_1_MONTH:
 cal.add(Calendar.MONTH, 1);
 break;
 case SKU_WORLD_1_YEAR:
 cal.add(Calendar.YEAR, 1);
 }
 Date newExpiry = cal.getTime();

 receipt.setExpiryDate(newExpiry);
 receipts.add(receipt);
 s.writeObject(RECEIPTS_KEY, receipts);

 }
 // Make sure this is outside the synchronized block
 callback.onSucess(Boolean.TRUE);
}
// end::monetization-java-012[]

// tag::monetization-java-013[]
public void start() {

...

 // Now synchronize the receipts
 iap.synchronizeReceipts(0, res->{
 // Update the UI as necessary to reflect

 });
}
// end::monetization-java-013[]

// tag::monetization-java-014[]
Button syncReceipts = new Button("Synchronize Receipts");

syncReceipts.addActionListener(e->{

 iap.synchronizeReceipts(0, res->{
 // Update the UI
 });
});
// end::monetization-java-014[]

// tag::monetization-java-015[]
//...

SpanLabel rentalStatus = new SpanLabel("Loading rental details...");
Button syncReceipts = new Button("Synchronize Receipts");

syncReceipts.addActionListener(e->{

 iap.synchronizeReceipts(0, res->{
 if (iap.isSubscribed(PRODUCTS)) {
 rentalStatus.setText("World rental expires "+iap.getExpiryDate(PRODUCTS));
 } else {
 rentalStatus.setText("You don't currently have a subscription to the world");
 }
 hi.revalidate();
 });
});
// end::monetization-java-015[]

// tag::monetization-java-016[]
Purchase iap = Purchase.getInAppPurchase();
//...
Button rentWorld1M = new Button("Rent World 1 Month");
rentWorld1M.addActionListener(e->{
 String msg = null;
 if (iap.isSubscribed(PRODUCTS)) { // <1>
 msg = "you're already renting the world until "
 +iap.getExpiryDate(PRODUCTS) // <2>
 +". Extend it for one more month?";
 } else {
 msg = "Rent the world for 1 month?";
 }
 if (Dialog.show("Confirm", msg, "Yes", "No")) {
 Purchase.getInAppPurchase().purchase(SKU_WORLD_1_MONTH); // <3>
 // Note: since this is a non-renewable subscription it's a regular
 // product in the play store - therefore you use the purchase() method.
 // If it were a "subscription" product in the play store, then you
 // would use subscribe() instead.
 }
});

Button rentWorld1Y = new Button("Rent World 1 Year");
rentWorld1Y.addActionListener(e->{
 String msg = null;
 if (iap.isSubscribed(PRODUCTS)) {
 msg = "you're already renting the world until "+
 iap.getExpiryDate(PRODUCTS)+
 ". Extend it for one more year?";
 } else {
 msg = "Rent the world for 1 year?";
 }
 if (Dialog.show("Confirm", msg, "Yes", "No")) {
 Purchase.getInAppPurchase().purchase(SKU_WORLD_1_YEAR);
 // Note: since this is a non-renewable subscription it's a regular
 // product in the play store - therefore you use the purchase() method.
 // If it were a "subscription" product in the play store, then you
 // would use subscribe() instead.
 }
});
// end::monetization-java-016[]

// tag::monetization-java-017[]
@Override
public void itemPurchased(String sku) {
 Purchase iap = Purchase.getInAppPurchase();

 // Force you to reload the receipts from the store.
 iap.synchronizeReceiptsSync(0);
 ToastBar.showMessage("Your subscription has been extended to "+iap.getExpiryDate(PRODUCTS), FontImage.MATERIAL_THUMB_UP);
}

@Override
public void itemPurchaseError(String sku, String errorMessage) {
 ToastBar.showErrorMessage("Failure occurred: "+errorMessage);
}
// end::monetization-java-017[]

// tag::monetization-java-018[]
private static final String localHost = "http://10.0.1.32";
// end::monetization-java-018[]

// tag::monetization-java-019[]
private ReceiptStore createReceiptStore() {
 return new ReceiptStore() {

 RESTfulWebServiceClient client = createRESTClient(receiptsEndpoint);

 @Override
 public void fetchReceipts(SuccessCallback<Receipt[]> callback) {
 RESTfulWebServiceClient.Query query = new RESTfulWebServiceClient.Query() {

 @Override
 protected void setupConnectionRequest(RESTfulWebServiceClient client, ConnectionRequest req) {
 super.setupConnectionRequest(client, req);
 req.setUrl(receiptsEndpoint);
 }

 };
 client.find(query, rowset->{
 List<Receipt> out = new ArrayList<Receipt>();
 for (Map m : rowset) {
 Result res = Result.fromContent(m);
 Receipt r = new Receipt();
 r.setTransactionId(res.getAsString("transactionId"));
 r.setPurchaseDate(new Date(res.getAsLong("purchaseDate")));
 r.setQuantity(1);
 r.setStoreCode(m.getAsString("storeCode"));
 r.setSku(res.getAsString("sku"));

 if (m.containsKey("cancellationDate") && m.get("cancellationDate")!= null) {
 r.setCancellationDate(new Date(res.getAsLong("cancellationDate")));
 }
 if (m.containsKey("expiryDate") && m.get("expiryDate")!= null) {
 r.setExpiryDate(new Date(res.getAsLong("expiryDate")));
 }
 out.add(r);

 }
 callback.onSucess(out.toArray(new Receipt[out.size()]));
 });
 }

 @Override
 public void submitReceipt(Receipt r, SuccessCallback<Boolean> callback) {
 Map m = new HashMap();
 m.put("transactionId", r.getTransactionId());
 m.put("sku", r.getSku());
 m.put("purchaseDate", r.getPurchaseDate().getTime());
 m.put("orderData", r.getOrderData());
 m.put("storeCode", r.getStoreCode());
 client.create(m, callback);
 }

 };
}
// end::monetization-java-019[]

// tag::monetization-java-020[]
/**
 * Creates a REST client to connect to a particular endpoint. The REST client
 * generated here will automatically add the Authorization header
 * which tells the service what platform you're on.
 * @param url The url of the endpoint.
 * @return
 */
private RESTfulWebServiceClient createRESTClient(String url) {
 return new RESTfulWebServiceClient(url) {

 @Override
 protected void setupConnectionRequest(ConnectionRequest req) {
 try {
 req.addRequestHeader("Authorization", "Basic " + Base64.encode((getUsername()+":"+getPassword()).getBytes("UTF-8")));
 } catch (Exception ex) {}
 }

 };
}
// end::monetization-java-020[]

// tag::monetization-java-021[]
@Stateless
@Path("com.codename1.demos.iapserver.receipts")
public class ReceiptsFacadeREST extends AbstractFacade<Receipts> {

 //...

 @POST
 @Consumes({"application/xml", "application/json"})
 public void create(Receipts entity) {

 String username = credentialsWithBasicAuthentication(request).getName();
 entity.setUsername(username);

 // Save the receipt first in case something goes wrong in the validation stage
 super.create(entity);

 // Let's validate the receipt
 validateAndSaveReceipt(entity);
 // validates the receipt against appropriate web service
 // and updates database if expiry date has changed.
 }

 //...
 @GET
 @Override
 @Produces({"application/xml", "application/json"})
 public List<Receipts> findAll() {
 String username = credentialsWithBasicAuthentication(request).getName();
 return getEntityManager()
.createNamedQuery("Receipts.findByUsername")
.setParameter("username", username)
.getResultList();
 }
}
// end::monetization-java-021[]

// tag::monetization-java-022[]
private static final long ONE_DAY = 24 * 60 * 60 * 1000;
private static final long ONE_DAY_SANDBOX = 10 * 1000;
@Schedule(hour="*", minute="*")
public void validateSubscriptionsCron() {
 System.out.println("----------- DOING TIMED TASK ---------");
 List<Receipts> res = null;
 final Set<String> completedTransactionIds = new HashSet<String>();
 for (String storeCode : new String[]{Receipt.STORE_CODE_ITUNES, Receipt.STORE_CODE_PLAY}) {
 while (!(res = getEntityManager().createNamedQuery("Receipts.findNextToValidate")
.setParameter("threshold", System.currentTimeMillis() - ONE_DAY_SANDBOX)
.setParameter("storeCode", storeCode)
.setMaxResults(1)
.getResultList()).isEmpty() &&
!completedTransactionIds.contains(res.get(0).getTransactionId())) {

 final Receipts curr = res.get(0);
 completedTransactionIds.add(curr.getTransactionId());
 Receipts[] validatedReceipts = validateAndSaveReceipt(curr);
 em.flush();
 for (Receipts r : validatedReceipts) {
 completedTransactionIds.add(r.getTransactionId());
 }

 }
 }
}
// end::monetization-java-022[]

// tag::monetization-java-023[]
IAPValidator validator = IAPValidator.getValidatorForPlatform(receipt.getStoreCode());
if (validator == null) {
 // no validators were found for this store
 // Do custom validation
} else {
 validator.setAppleSecret(APPLE_SECRET);
 validator.setGoogleClientId(GOOGLE_DEVELOPER_API_CLIENT_ID);
 validator.setGooglePrivateKey(GOOGLE_DEVELOPER_PRIVATE_KEY);
 Receipt[] result = validator.validate(receipt);
...
}
// end::monetization-java-023[]

// tag::monetization-java-024[]
/**
 * Validates a given receipt, updating the expiry date,
 * @param receipt The receipt to be validated
 * @param forInsert If true, then an expiry date will be calculated even if there is no validator.
 */
private Receipts[] validateAndSaveReceipt(Receipts receipt) {
 EntityManager em = getEntityManager();
 Receipts managedReceipt = getManagedReceipt(receipt);
 // managedReceipt == receipt if receipt is in database or null otherwise

 if (Receipt.STORE_CODE_SIMULATOR.equals(receipt.getStoreCode())) { // <1>
 if (receipt.getExpiryDate() == null && managedReceipt == null) {
 //Not inserted yet and no expiry date set yet
 Date dt = calculateExpiryDate(receipt.getSku(), true);
 if (dt!= null) {
 receipt.setExpiryDate(dt.getTime());
 }
 }
 if (managedReceipt == null) {
 // Receipt isn't in the database yet. Add it
 em.persist(receipt);
 return new Receipts[]{receipt};
 } else {
 // The receipt is already in the database. Update it.
 em.merge(managedReceipt);
 return new Receipts[]{managedReceipt};
 }
 } else {
 // It isn't a simulator receipt
 IAPValidator validator = IAPValidator.getValidatorForPlatform(receipt.getStoreCode());
 if (validator == null) {
 // Receipt must have come from a platform other than iTunes or Play
 // Because there is no validator

 if (receipt.getExpiryDate() == null && managedReceipt == null) {
 // No expiry date.
 // Generate one.
 Date dt = calculateExpiryDate(receipt.getSku(), false);
 if (dt!= null) {
 receipt.setExpiryDate(dt.getTime());
 }

 }
 if (managedReceipt == null) {
 em.persist(receipt);
 return new Receipts[]{receipt};
 } else {
 em.merge(managedReceipt);
 return new Receipts[]{managedReceipt};
 }

 }

 // Set credentials for the validator
 validator.setAppleSecret(APPLE_SECRET);
 validator.setGoogleClientId(GOOGLE_DEVELOPER_API_CLIENT_ID);
 validator.setGooglePrivateKey(GOOGLE_DEVELOPER_PRIVATE_KEY);

 // Create a dummy receipt with transaction ID and order data to pass
 // to the validator. Really all it needs is order data to be able to validate
 Receipt r2 = Receipt();
 r2.setTransactionId(receipt.getTransactionId());
 r2.setOrderData(receipt.getOrderData());
 try {
 Receipt[] result = validator.validate(r2);
 // Depending on the platform, result may contain many receipts or a single receipt
 // matching your receipt. In the case of iTunes, none of the receipt transaction IDs
 // might match the original receipt's transactionId because the validator
 // will set the transaction ID to the *original* receipt's transaction ID.
 // If none match, then you should remove your receipt, and update each of the returned
 // receipts in the database.
 Receipt matchingValidatedReceipt = null;
 for (Receipt r3 : result) {
 if (r3.getTransactionId().equals(receipt.getTransactionId())) {
 matchingValidatedReceipt = r3;
 break;
 }
 }

 if (matchingValidatedReceipt == null) {
 // Since the validator didn't find your receipt,
 // you should remove the receipt. The equivalent
 // is stored under the original receipt's transaction ID
 if (managedReceipt!= null) {
 em.remove(managedReceipt);
 managedReceipt = null;
 }
 }
 List<Receipts> out = new ArrayList<Receipts>();
 // Now go through and
 for (Receipt r3 : result) {
 if (r3.getOrderData() == null) {
 // No order data found in receipt. Setting it to the original order data
 r3.setOrderData(receipt.getOrderData());
 }
 Receipts eReceipt = new Receipts();
 eReceipt.setTransactionId(r3.getTransactionId());
 eReceipt.setStoreCode(receipt.getStoreCode());
 Receipts eManagedReceipt = getManagedReceipt(eReceipt);
 if (eManagedReceipt == null) {
 copy(eReceipt, r3);
 eReceipt.setUsername(receipt.getUsername());
 eReceipt.setLastValidated(System.currentTimeMillis());
 em.persist(eReceipt);
 out.add(eReceipt);
 } else {

 copy(eManagedReceipt, r3);
 eManagedReceipt.setUsername(receipt.getUsername());
 eManagedReceipt.setLastValidated(System.currentTimeMillis());
 em.merge(eManagedReceipt);
 out.add(eManagedReceipt);
 }
 }

 return out.toArray(new Receipts[out.size()]);

 } catch (Exception ex) {
 // You should probably store some info about the failure in the
 // database to make it easier to find receipts that aren't validating,
 // but for now you will log it.
 Log.p("Failed to validate receipt "+r2);
 Log.p("Reason: "+ex.getMessage());
 Log.e(ex);
 return new Receipts[]{receipt};

 }
 }
}
// end::monetization-java-024[]

// tag::monetization-java-025[]
public static final String GOOGLE_DEVELOPER_API_CLIENT_ID="iapdemo@iapdemo-152500.iam.gserviceaccount.com";
public static final String GOOGLE_DEVELOPER_PRIVATE_KEY="-----BEGIN PRIVATE KEY-----... -----END PRIVATE KEY-----\n";

...

validator.setGoogleClientId(GOOGLE_DEVELOPER_API_CLIENT_ID);
validator.setGooglePrivateKey(GOOGLE_DEVELOPER_PRIVATE_KEY);
// end::monetization-java-025[]

// tag::monetization-java-026[]
public static final boolean DISABLE_PLAY_STORE_VALIDATION=true;
// end::monetization-java-026[]

// tag::monetization-java-027[]
public static final String APPLE_SECRET = "your-shared-secret-here";
// end::monetization-java-027[]

// tag::monetization-java-028[]
public static final boolean DISABLE_ITUNES_STORE_VALIDATION=true;
// end::monetization-java-028[]
