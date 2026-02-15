---
title: Introduction to In-App Purchase
slug: intro-to-in-app-purchase
url: /blog/intro-to-in-app-purchase/
original_url: https://www.codenameone.com/blog/intro-to-in-app-purchase.html
aliases:
- /blog/intro-to-in-app-purchase.html
date: '2016-12-13'
author: Steve Hannah
---

![Header Image](/blog/intro-to-in-app-purchase/in-app-purchase.jpg)

__ |  This is the first post in a three-part series on In-App purchase. Please check out [Part 2: Introduction to In-App Purchase](/blog/in-app-purchase-non-renewable-subscriptions/) and [Part 3: Auto-renewing Subscriptions in iOS and Android](/blog/autorenewing-subscriptions-in-ios-and-android/).   
---|---  
  
In-app purchase is a helpful tool for making app development profitable. Codename One has supported in-app purchases of consumable and non-consumable products on Android and iOS for some time now, and with the next update we are adding support for subscriptions. For such a seemingly simple task, in-app purchase involves a lot of moving parts – especially when it comes to subscriptions. For this reason, I’ll be splitting this topic up into a few different blog posts. This post will provide a light introduction to in-app purchase and subscriptions, and show you how to support them in your app. In subsequent posts, I’ll go deeper into some more advanced topics such as receipt validation, server-side subscription management (which is required for subscriptions in the iTunes store), and the specifics of how to set up in-app purchases in both Google Play and the iTunes stores.

## The SKU

In-app purchase support is centered around your set of SKUs that you want to sell. Each product that you sell, whether it be a 1-month subscription, an upgrade to the “Pro” version, “10 disco credits”, will have a SKU (stock-keeping-unit). Ideally you will be able to use the same SKU across all of the stores that you sell your app in.

## Types of Products

There are generally 4 classifications for products:

  1. **Non-consumable Product** – This is a product that the user purchases once, and they “own” it. They cannot re-purchase it. One example is a product that upgrades your app to a “Pro” version.

  2. **Consumable Product** – This is a product that the user can purchase multiple times. E.g. You might have a product for “10 Credits” that allow the user to buy things in a game.

  3. **Non-Renewable Subscription** – A subscription that is purchased once, and will not be “auto-renewed” by the app store. These are almost identical to consumable products, except that subscriptions need to be transferable across all of the user’s devices. This means that non-renewable subscriptions require that you have a server server to keep track of the subscriptions.

  4. **Renewable Subscriptions** – A subscription that is completely managed by the app store. The user will be automatically billed when the subscription period ends, and the subscription will be renewed.

__ |  These subscription categories may not be explicitly supported by a given store, or they may be called different things. However each type of product can be implemented in a Codename One app in a cross-platform way. E.g. In Google Play there is no distinction between consumable products and non-renewable subscriptions, but in iTunes there is a distinction.   
---|---  
  
## The “Hello World” of In-App Purchase

Let’s start with a simple example of an app that sells “Worlds”. The first thing we do is pick the SKU for our product. I’ll choose “com.codename1.world” for the SKU.
    
    
    public static final String SKU_WORLD = "com.codename1.world";

Next, our app’s main class needs to implement the `PurchaseCallback` interface
    
    
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
        public void itemRefunded(String sku) {
            ...
        }
    
        @Override
        public void subscriptionStarted(String sku) {
            ...
        }
    
        @Override
        public void subscriptionCanceled(String sku) {
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

Using these callbacks, we’ll be notified whenever something changes in our purchases. For our simple app we’re only interested in `itemPurchased()` and `itemPurchaseError()`.

Now in the start method, we’ll add a button that allows the user to buy the world:
    
    
        public void start() {
            if(current != null){
                current.show();
                return;
            }
            Form hi = new Form("Hi World");
            Button buyWorld = new Button("Buy World");
            buyWorld.addActionListener(e->{
                if (Purchase.getInAppPurchase().wasPurchased(SKU_WORLD)) {
                    Dialog.show("Can't Buy It", "You already Own It", "OK", null);
                } else {
                    Purchase.getInAppPurchase().purchase(SKU_WORLD);
                }
            });
    
            hi.addComponent(buyWorld);
            hi.show();
        }

At this point, we already have a functional app that will track the sale of the world. To make it more interesting, let’s just add some feedback with the ToastBar to show when the purchase completes.
    
    
        @Override
        public void itemPurchased(String sku) {
            ToastBar.showMessage("Thanks.  You now own the world", FontImage.MATERIAL_THUMB_UP);
        }
    
        @Override
        public void itemPurchaseError(String sku, String errorMessage) {
            ToastBar.showErrorMessage("Failure occurred: "+errorMessage);
        }

[See full code listing](https://gist.github.com/shannah/c38d18a1f3524a4a7e8d08d2731cfac7)

__ |  You can test out this code in the simulator without doing any additional setup and it will work. If you want the code to work properly on Android and iOS, you’ll need to set up the app and in-app purchase settings in the Google Play and iTunes stores respectively. I will cover that in a subsequent post.   
---|---  
  
When the app first opens we see our button:

![In-app purchase demo app](/blog/intro-to-in-app-purchase/iap-demo-1.png)

In the simulator, clicking on the “Buy World” button will bring up a prompt to ask you if you want to approve the purchase.

![Approving the purchase in the simulator](/blog/intro-to-in-app-purchase/iap-demo2.png)

Now if I try to buy the product again, it pops up the dialog to let me know that I already own it.

![In App purchase already owned](/blog/intro-to-in-app-purchase/iap-demo3.png)

## Making it Consumable

In the “Buy World” example above, the “world” product was non-consumable, since it could only be purchased once. We could easily change it to a consumable product by simply disregarding whether it had been purchased before and keeping track of how many times it had been purchased.

We’ll use storage to keep track of the number of worlds that have been purchased. We need two methods to manage this count. One to get the number of worlds that we currently own, and another to add a world to this count.
    
    
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

Now we’ll change our purchase code as follows:
    
    
            buyWorld.addActionListener(e->{
                if (Dialog.show("Confirm", "You own "+getNumWorlds()+
                       " worlds.  Do you want to buy another one?", "Yes", "No")) {
                    Purchase.getInAppPurchase().purchase(SKU_WORLD);
                }
            });

And our `itemPurchased()` callback will need to add a world:
    
    
        @Override
        public void itemPurchased(String sku) {
            addWorld();
            ToastBar.showMessage("Thanks.  You now own "+getNumWorlds()+" worlds", FontImage.MATERIAL_THUMB_UP);
        }

[Show full code listing](https://gist.github.com/shannah/9a7bd06951207101918ad93fa809ee56)

__ |  When we eventually set up the products in the iTunes store we will need to mark the product as a consumable product or iTunes will prevent us from purchasing it multiple times.   
---|---  
  
## Next Up: Non-Renewable Subscriptions

Read more: [Part 2: Introduction to In-App Purchase](/blog/in-app-purchase-non-renewable-subscriptions/)
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **salah Alhaddabi** — December 28, 2016 at 7:38 pm ([permalink](/blog/intro-to-in-app-purchase/#comment-23026))

> Very nice Steve.
>
> CN1 is an amazing framework for mobile apps really!!
>
> Please continue with these posts they are the best!!
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
