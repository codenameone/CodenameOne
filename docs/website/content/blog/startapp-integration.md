---
title: StartApp integration
slug: startapp-integration
url: /blog/startapp-integration/
original_url: https://www.codenameone.com/blog/startapp-integration.html
aliases:
- /blog/startapp-integration.html
date: '2015-08-19'
author: Chen Fishbein
---

![Header Image](/blog/startapp-integration/startapp.png)

When StartApp first launched a few years ago, they were a unique innovative new monetization channel to make money on Android.  
The Android developer bundled their SDK and once the app was installed a new search shortcut would appear on the user’s device,   
this allowed them to monetize on the search functionality.  
We had a smart integration with StartApp, were the developer checked a CheckBox on the plugin and the SDK got bundled on the cloud build. 

This unique new monetization channel was a hit.  
[StartApp on VentureBeat 2 years ago](http://venturebeat.com/2013/03/12/startapps-growth-explodes-500-million-downloads-more-searches-on-android-than-anyone-but-google/)  
But it was a matter of time until Google will prohibit this from their platform, taking money from Google on Android with a different Search engine utility…  
Well, Google did changed their terms eventually and started banning apps from their play store with this integration. 

Nowadays, StartApp has managed to recover and become a strong new innovative Ads network for iOS and Android.  
We are now introducing a new cn1lib to allow you to monetize your apps using their channel. 

The new integration is quite simple. 

  1. First get yourself an account from their [portal](http://www.startapp.com/)
  2. Create 2 apps on their portal – one for Android and another for iOS 
  3. Grab the cn1lib from [here](https://github.com/chen-fishbein/startapp-codenameone/tree/master/CN1StartApp/dist)
  4. Place the CN1StartApp.cn1lib under your projects lib folder 
  5. Right click on your project and select “Codename One->Refresh Libs” 
  6. Now follow the Usage instructions [here](https://github.com/chen-fishbein/startapp-codenameone)
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **ugochukwu** — August 24, 2015 at 4:15 pm ([permalink](https://www.codenameone.com/blog/startapp-integration.html#comment-21559))

> ugochukwu says:
>
> shai thanks for this great tutorial
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fstartapp-integration.html)


### **Valeriy Skachko** — January 11, 2016 at 2:07 pm ([permalink](https://www.codenameone.com/blog/startapp-integration.html#comment-22591))

> Valeriy Skachko says:
>
> Hi Chen! I could not make integration.
>
> private StartAppManager manager;  
> ….  
> public void init(Object context) {  
> try{  
> Resources theme = Resources.openLayered(“/beer”);  
> UIManager.getInstance().setThemeProps(theme.getTheme(theme.getThemeResourceNames()[0]));  
> //init the startapp SDK  
> manager = new StartAppManager();  
> manager.initAndroidSDK(“000000000”, “00000000”, true);  
> }catch(IOException e){  
> e.printStackTrace();  
> }}  
> …  
> public void start() {  
> manager.loadAd(StartAppManager.AD_INTERSTITIALS);  
> …  
> manager.showAd();  
> }
>
> But, receive on android device – An internal application error occurred: java.lang. AssertionError
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fstartapp-integration.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
