---
title: 'Important: New Sign-In Behavior'
slug: new-sign-in-behavior
url: /blog/new-sign-in-behavior/
original_url: https://www.codenameone.com/blog/new-sign-in-behavior.html
aliases:
- /blog/new-sign-in-behavior.html
date: '2020-11-26'
author: Shai Almog
---

Today, we are finally integrating the new **Single-Sign-On** to **most** of our tools. 

This means that you will need to create a new account using the login button in this site in order to use Codename One from now on. 

To make the migration easier, we tried to keep the existing login working as much as possible with the one notable exception of password reset which is no longer wired. So if you lose your password, you would need to migrate.

If you’re creating a new account, this should be pretty seamless. Just use the product as it is.

If you already have a Codename One account (paid or otherwise) just signup as a new account by pressing **[Login](https://auth.codenameone.com/auth/realms/Realm/protocol/openid-connect/auth?client_id=wordpressauth&scope=openid%20address&redirect_uri=https://www.codenameone.com&response_type=code&state=a2V5Y2xvYWs=)** and clicking register. Make sure to use the email address used for your existing account. You will get a confirmation email which you will need to click. At this point the new account and the old account will be bound. If you’re a paid subscriber things should “just work”.

### **This is the current status our various tools when it comes to the new login:**  

  * **Website: **Already works with the new login system.

  * **Build from IDE:** This is launching on Friday the 27th of November.

  * **Codename One Settings:** Settings still uses the old login. We hope to release an update in a couple of weeks.  
  

  * **Codename One Build On Web: **There are two versions of build. The old one [here](https://cloud.codenameone.com/buildapp/index.html) and the new login [here](https://cloud.codenameone.com/secure/index.html).  
  

  * **Codename One Build Mobile App:** The Android app will be updated within a couple of weeks. It still uses the old login.  

As always, please use the website chat to let us know if you’re experiencing difficulties. Notice this isn’t a live chat, we get email alerts and get back to you when available so it works best if you leave an email.

### Coming Up…

The Single Sign On work has taken up a lot of my time and made me a bottleneck for information and actual productivity. But there’s a lot going on while we’re doing this.

  * **New build app** : Chen has re-imagined the Codename One Build app and Settings as a hybrid console for Codename One. It looks stunning and we plan to launch it in the next couple of weeks.  
  

  * **Discussion forum** : We hope to import the Google Group discussion forum into a website standard forum which will give us greater control. This is especially important due to recent changes from Google to the group interface and control.   
  

  * **Comments** : We still don’t have website comments. Our web developer was having a hard time dealing with this and I was busy with SSO. We’ll try to get them up and about soon enough so we can return to our communicative selves. As of now we’re still up on the [discussion forum](https://groups.google.com/g/codenameone-discussions) and in [stack overflow](https://stackoverflow.com/tags/codenameone).

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
