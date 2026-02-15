---
title: Implementing Non-Renewable Subscriptions with In-App Purchase
slug: in-app-purchase-non-renewable-subscriptions
url: /blog/in-app-purchase-non-renewable-subscriptions/
original_url: https://www.codenameone.com/blog/in-app-purchase-non-renewable-subscriptions.html
aliases:
- /blog/in-app-purchase-non-renewable-subscriptions.html
date: '2016-12-19'
author: Steve Hannah
---

![Header Image](/blog/in-app-purchase-non-renewable-subscriptions/in-app-purchase.jpg)

__ |  This is the second post in a three-part series on In-App purchase. Please check out [Part I: Introduction to In-App Purchase](/blog/intro-to-in-app-purchase/) and [Part 3: Auto-renewing Subscriptions in iOS and Android](/blog/autorenewing-subscriptions-in-ios-and-android/).   
---|---  
  
In [my last post](/blog/intro-to-in-app-purchase/) we looked at one-off in-app purchases. In this post we’ll look at subscriptions. As we discussed before, there are two types of subscriptions:

  1. Non-renewable

  2. Auto-renewable

Non-renewable subscriptions are really the same as consumable products, except that they are shareable across all of a user’s devices. Auto-renewable subscriptions, on the other hand, will continue as long as the user doesn’t cancel it. They will be re-billed automatically by the appropriate app-store when the chosen period expires, and all management of the subscription is handled by the the app-store itself.

__ |  The concept of an “Non-renewable” subscription is an invention of the iTunes store. There is no formal equivalent in Google play. In order to create a non-renewable subscription SKU that behaves the same in your iOS and Android apps you would create it as a regular product in Google play, and a Non-renewable subscription in the iTunes store. We’ll learn more about that in a later post when we go into the specifics of app store setup.   
---|---  
  
## The Server-Side

Since a subscription purchased on one user device **needs** to be available across all of the user’s devices (Apple’s rules for non-renewable subscriptions), our app will need to have a server-component. In this post, we’ll gloss over that requirement and just “mock” the server interface. We’ll go into the specifics of the server-side in a later post.

### The Receipts API

Subscriptions, in Codename One are handled using a new “Receipts” API. It is up to you to register a receipt store with the In-App purchase instance, which allows Codename one to load receipts (presumably from your server), and submit new receipts (presumably to your server). A `Receipt` includes information such as:

  1. Store code (since you may be dealing with receipts from multiple stores)

  2. SKU

  3. Transaction ID (store specific)

  4. Expiry Date

  5. Cancellation date

  6. Purchase date

  7. Order Data (can be used on the server-side to verify the receipt and load additional receipt details directly from the store it originated from).

The `Purchase` provides a set of methods for interacting with the receipt store, such as:

  1. `isSubscribed([skus])` – Checks to see if the user is currently subscribed to any of the provided skus.

  2. `getExpiryDate([skus])` – Checks the expiry date for a set of skus.

  3. `synchronizeReceipts()` – Synchronizes the receipts with the receipt store. This will attempt to submit any pending purchase receipts to the receipt store, and the reload receipts from the receipt store.

In order for any of this to work, you must implement the `ReceiptStore` interface, and register it with the Purchase instance. Your receipt store must implement two methods:

  1. `fetchReceipts(SuccessCallback<Receipt[]> callback)` – Loads all of the receipts from your receipt store for the current user.

  2. `submitReceipt(Receipt receipt, SuccessCallback<Boolean> callback)` – Submits a receipt to your receipt store. This gives you an opportunity to add additional details to the receipt such as an expiry date.

## The “Hello World” of Non-Renewable Subscriptions

We’ll expand on the theme of “Buying” the world for this app, except, this time we will just “Rent” the world for a period of time. We’ll have two products:

  1. A 1 month subscription

  2. A 1 year subscription

    
    
        public static final String SKU_WORLD_1_MONTH = "com.codename1.world.subscribe.1month";
        public static final String SKU_WORLD_1_YEAR = "com.codename1.world.subscribe.1year";
    
        public static final String[] PRODUCTS = {
            SKU_WORLD_1_MONTH,
            SKU_WORLD_1_YEAR
        };

Notice that we create two separate SKUs for the 1 month and 1 year subscription. **Each subscription period must have its own SKU**. I have created an array (`PRODUCTS`) that contains both of the SKUs. This is handy, as you’ll see in the examples ahead, because all of the APIs for checking status and expiry date of a subscription take all of the SKUs in a “subscription group” as input. This is

__ |  Multiple SKUs that sell the same service/product but for different periods form a “subscription group”. Conceptually, customers are not subscribing to a particular SKU, they are subscribing to the subscription group of which that SKU is a member. As an example, if a user purchases a 1 month subscription to “the world”, they are actually just subscribing to “the world” subscription group.   
---|---  
  
It is up to you to know how your SKUs are grouped together, and any methods in the `Purchase` class that check subscription status or expiry date of a SKU should be passed **all** SKUs of that subscription group. E.g. If you want to know if the user is subscribed to the `SKU_WORLD_1_MONTH` subscription, it would not be sufficient to call `iap.isSubscribed(SKU_WORLD_1_MONTH)`, because that wouldn’t take into account if the user had purchased a 1 year subscription. The correct way is to always call `iap.isSubscribed(SKU_WORLD_1_MONTH, SKU_WORLD_1_YEAR)`, or simply `iap.isSubscribed(PRODUCTS)` since I have placed both SKUs into my PRODUCTS array.

### Implementing the Receipt Store

__ |  The receipt store is intended to interface with a server so that the subscriptions can be synced with multiple devices, as required by Apple’s guidelines. For this post we’ll just store our receipts on device using internal storage. Moving the logic to a server is a simple matter that we will cover in a future post when we cover the server-side.   
---|---  
  
![The Receipt store is a layer between your server and Codename One](/blog/in-app-purchase-non-renewable-subscriptions/in-app-purchase-receipt-store-diagram.png)

A basic receipt store needs to implement just two methods:

  1. `fetchReceipts`

  2. `submitReceipt`

Generally we’ll register it in our app’s init() method so that it is always available.
    
    
        public void init(Object context) {
            ...
    
            Purchase.getInAppPurchase().setReceiptStore(new ReceiptStore() {
    
                @Override
                public void fetchReceipts(SuccessCallback<Receipt[]> callback) {
                    // Fetch receipts from storage and pass them to the callback
                }
    
                @Override
                public void submitReceipt(Receipt receipt, SuccessCallback<Boolean> callback) {
                    // Save a receipt to storage.  Make sure to call callback when done.
                }
            });
        }

These methods are designed to be called asynchronously since real-world apps will always be connecting to some sort of network service. Therefore, instead of returning a value, both of these methods are passed instances of the `SuccessCallback` class. It is important to make sure to call `callback.onSuccess()` **ALWAYS** when the methods have completed, even if there is an error, or the Purchase class will just assume that you’re taking a long time to complete the task, and will continue to wait for you to finish.

Once implemented, our `fetchReceipts()` method will look like:
    
    
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

This is fairly straight forward. We’re checking to see if we already have a list of receipts stored. If so we return that list to the callback. If not we return an empty array of receipts.

__ |  `Receipt` implements `Externalizable` so you are able to write instances directly to Storage.   
---|---  
  
The `submitReceipt()` method is a little more complex, as it needs to calculate the new expiry date for our subscription.
    
    
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
            // This probably won't ever happen (that we'll be asked to submit an
            // existing receipt, but better safe than sorry
            for (Receipt r : receipts) {
                if (r.getStoreCode().equals(receipt.getStoreCode()) &&
                        r.getTransactionId().equals(receipt.getTransactionId())) {
                    // If we've already got this receipt, we'll just this submission.
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
                if (r.getCancellationDate() != null) {
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

The main logic of this method involves iterating through all of the existing receipts to find the **latest** current expiry date, so that when the user purchases a subscription, it is added onto the end of the current subscription (if one exists) rather than going from today’s date. This enables users to safely renew their subscription before the subscription has expired.

In the real-world, we would implement this logic on the server-side.

__ |  The iTunes store and Play store have no knowledge of your subscription durations. This is why it is up to you to set the expiry date in the `submitReceipt` method. Non-renewable subscriptions are essentially no different than regular consumable products. It is up to you to manage the subscription logic – and Apple, in particular, requires you to do so using a server.   
---|---  
  
### Synchronizing Receipts

In order for your app to provide you with current data about the user’s subscriptions and expiry dates, you need to synchronize the receipts with your receipt store. `Purchase` provides a set of methods for doing this. Generally I’ll call one of them inside the `start()` method, and I may resynchronize at other strategic times if I suspect that the information may have changed.

The following methods can be used for synchronization:

  1. `synchronizeReceipts()` – Asynchronously synchronizes receipts in the background. You won’t be notified when it is complete.

  2. `synchronizeReceiptsSync()` – Synchronously synchronizes receipts, and blocks until it is complete. This is safe to use on the EDT as it employs `invokeAndBlock` under the covers.

  3. `synchronizeReceipts(final long ifOlderThanMs, final SuccessCallback<Boolean> callback)` – Asynchronously synchronizes receipts, but only if they haven’t been synchronized in the specified time period. E.g. In your start() method you might decide that you only want to synchronize receipts once per day. This also includes a callback that will be called when synchronization is complete.

  4. `synchronizeReceiptsSync(long ifOlderThanMs)` – A synchronous version that will only refetch if data is older than given time.

In our hello world app we synchronize the subscriptions in a few places.

At the end of the `start()` method:
    
    
        public void start() {
    
           ...
    
            // Now synchronize the receipts
            iap.synchronizeReceipts(0, res->{
                // Update the UI as necessary to reflect
    
            });
        }

And I also provide a button to allow the user to manually synchronize the receipts.
    
    
            Button syncReceipts = new Button("Synchronize Receipts");
    
            syncReceipts.addActionListener(e->{
    
                iap.synchronizeReceipts(0, res->{
                    // Update the UI
                });
            });

### Expiry Dates and Subscription Status

Now that we have a receipt store registered, and we have synchronized our receipts, we can query the `Purchase` instance to see if a SKU or set of SKUs is currently subscribed. There are three useful methods in this realm:

  1. `boolean isSubscribed(String…​ skus)` – Checks to see if the user is currently subscribed to any of the provided SKUs.

  2. `Date getExpiryDate(String…​ skus)` – Gets the latest expiry date of a set of SKUs.

  3. `Receipt getFirstReceiptExpiringAfter(Date dt, String…​ skus)` – This method will return the earliest receipt with an expiry date after the given date. This is needed in cases where you need to decide if the user should have access to some content based on its publication date. E.g. If you published an issue of your e-zine on March 1, and the user purchased a subscription on March 15th, then they should get access to the March 1st issue even though it doesn’t necessarily fall in the subscription period. Being able to easily fetch the first receipt after a given date makes it easier to determine if a particular issue should be covered by a subscription.

If you need to know more information about subscriptions, you can always just call `getReceipts()` to obtain a list of all of the current receipts and determine for yourself what the user should have access to.

In the hello world app we’ll use this information in a few different places. On our main form we’ll include a label to show the current expiry date, and we allow the user to press a button to synchronize receipts manually if they think the value is out of date.
    
    
            // ...
    
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

### Allowing the User to Purchase the Subscription

You should now have all of the background required to implement the Hello World Subscription app. So we’ll return to the code and see how the user purchases a subscription.

In the main form, I want two buttons to subscribe to the “World”, for one month and one year respectively. They look like:
    
    
            Purchase iap = Purchase.getInAppPurchase();
            // ...
            Button rentWorld1M = new Button("Rent World 1 Month");
            rentWorld1M.addActionListener(e->{
                String msg = null;
                if (iap.isSubscribed(PRODUCTS)) {  __**(1)**
                    msg = "You are already renting the world until "
                         +iap.getExpiryDate(PRODUCTS)  __**(2)**
                         +".  Extend it for one more month?";
                } else {
                    msg = "Rent the world for 1 month?";
                }
                if (Dialog.show("Confirm", msg, "Yes", "No")) {
                    Purchase.getInAppPurchase().purchase(SKU_WORLD_1_MONTH); __**(3)**
                }
            });
    
            Button rentWorld1Y = new Button("Rent World 1 Year");
            rentWorld1Y.addActionListener(e->{
                String msg = null;
                if (iap.isSubscribed(PRODUCTS)) {
                    msg = "You are already renting the world until "+
                           iap.getExpiryDate(PRODUCTS)+
                          ".  Extend it for one more year?";
                } else {
                    msg = "Rent the world for 1 year?";
                }
                if (Dialog.show("Confirm", msg, "Yes", "No")) {
                    Purchase.getInAppPurchase().purchase(SKU_WORLD_1_YEAR);
                }
            });

__**1** | In the event handler we check if the user is subscribed by calling `isSubscribed(PRODUCTS)`. Notice that we check it against the array of both the one month and one year subscription SKUs.  
---|---  
__**2** | We are able to tell the user when the current expiry date is so that they can gauge whether to proceed.  
__**3** | Since this is a non-renewable subscription, we use the `Purchase.purchase()` method. See following note about `subscribe()` vs `purchase()`  
  
#### subscribe() vs purchase()

The `Purchase` class includes two methods for initiating a purchase:

  1. `purchase(sku)`

  2. `subscribe(sku)`

Which one you use depends on the type of product that is being purchased. If your product is set up as a subscription in the Google Play store, then you should use `subscribe(sku)`. Otherwise, you should use `purchase(sku)`.

### Handling Purchase Callbacks

The purchase callbacks are very similar to the ones that we implemented in the regular in-app purchase examples:
    
    
        @Override
        public void itemPurchased(String sku) {
            Purchase iap = Purchase.getInAppPurchase();
    
            // Force us to reload the receipts from the store.
            iap.synchronizeReceiptsSync(0);
            ToastBar.showMessage("Your subscription has been extended to "+iap.getExpiryDate(PRODUCTS), FontImage.MATERIAL_THUMB_UP);
        }
    
        @Override
        public void itemPurchaseError(String sku, String errorMessage) {
            ToastBar.showErrorMessage("Failure occurred: "+errorMessage);
        }

Notice that, in `itemPurchased()` we don’t need to explicitly create any receipts or submit anything to the receipt store. This is handled for you automatically. We do make a call to `synchronizeReceiptsSync()` but this is just to ensure that our toast message has the new expiry date loaded already.

## Full Source

[View the full source listing of this application](https://gist.github.com/shannah/f998545f0b17f0c412af54ea2db61e35)

## Screenshots

![Main form](/blog/in-app-purchase-non-renewable-subscriptions/in-app-purchase-subscription-main-form.png)

![Dialog shown when subscribing to a product](/blog/in-app-purchase-non-renewable-subscriptions/in-app-purchase-subscription-dialog.png)

![Simulator confirm dialog when purchasing a subscription](/blog/in-app-purchase-non-renewable-subscriptions/in-app-purchase-subscription-confirm.png)

![Upon successful purchase](/blog/in-app-purchase-non-renewable-subscriptions/in-app-purchase-subscription-toastbar-success.png)

## Summary

This post demonstrated how to set up an app to use non-renewable subscriptions using in-app purchase. Non-renewable subscriptions are the same as regular consumable products except for the fact that they are shared by all of the user’s devices, and thus, require a server component. The app store has no knowledge of the duration of your non-renewable subscriptions. It is up to you to specify the expiry date of purchased subscriptions on their receipts when they are submitted. Google play doesn’t formally have a “non-renewable” subscription product type. To implement them in Google play, you would just set up a regular product. It is how you handle it internally that makes it a subscription, and not just a regular product.

Codename One uses the `Receipt` class as the foundation for its subscriptions infrastructure. You, as the developer, are responsible for implementing the `ReceiptStore` interface to provide the receipts. The `Purchase` instance will load receipts from your ReceiptStore, and use them to determine whether the user is currently subscribed to a subscription, and when the subscription expires.

## Up Next: Auto-Renewable Subscriptions

The [next post](/blog/autorenewing-subscriptions-in-ios-and-android/) in this series covers [Auto-renewable subscriptions](/blog/autorenewing-subscriptions-in-ios-and-android/).
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Giri Sugu** — December 7, 2017 at 6:37 am ([permalink](/blog/in-app-purchase-non-renewable-subscriptions/#comment-23763))

> Hi. In non-renewable subscriptions i have a couple of questions,  
> 1\. if i have 200 products means, do i want to add that all 200 products in iTunes connect  
> [2.is](<http://2.is>) it possible to set the price and expiry date which i want.(bcoz if i am adding 200 products on iTunes connect i have to set the price.But in that we can able to choose the tier they mentioned.)So is it possible to set the prices
>



### **Shai Almog** — December 8, 2017 at 6:51 am ([permalink](/blog/in-app-purchase-non-renewable-subscriptions/#comment-23672))

> Hi,  
> 1\. Yes.  
> 2\. I think there is an ability to pick a price but that makes it hard with international sales and coin fluctuations so tiers might be better overall.
>



### **Julien Sosin** — February 9, 2018 at 7:21 am ([permalink](/blog/in-app-purchase-non-renewable-subscriptions/#comment-23896))

> Hello. An user cancel his purchase but Apple didn’t send the cancellation_date when I refresh the receipts and I can’t know that the user had cancel his purchase. What can I do ?
>



### **Shai Almog** — February 10, 2018 at 6:01 am ([permalink](/blog/in-app-purchase-non-renewable-subscriptions/#comment-23878))

> Hi,  
> Not much. If Apple doesn’t send the cancellation it’s a problem. Generally with in-app-purchase you should only sell stuff you don’t mind losing occasionally on.
>



### **Julien Sosin** — February 10, 2018 at 8:57 am ([permalink](/blog/in-app-purchase-non-renewable-subscriptions/#comment-23750))

> Hi Almog. I guess I should check manually :/
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
