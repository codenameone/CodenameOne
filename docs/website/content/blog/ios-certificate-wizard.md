---
title: iOS Certificate Wizard
slug: ios-certificate-wizard
url: /blog/ios-certificate-wizard/
original_url: https://www.codenameone.com/blog/ios-certificate-wizard.html
aliases:
- /blog/ios-certificate-wizard.html
date: '2015-07-07'
author: Steve Hannah
---

![Header Image](/blog/ios-certificate-wizard/ios-cert-wizard-blog-post-header.png)

So you have finished your app and tested it on the simulator. Everything looks good. You’re now ready to proceed with testing on your iPhone. You select the “Send iOS Debug Build” menu item and wait for the build server to work its magic, but then you’re faced with a notice that iOS builds require a valid certificate and provisioning profile. What a hassle!

If you’ve done any iOS development (not just Codename One) I’m sure you’ve hit this speed-bump before. You can’t **just** test your app on your iPhone. You have to jump through a series of hoops imposed by Apple before you get the privilege of testing your app on your phone. You need to create an App ID with the necessary permissions, generate a certificate signing request, download your certificates, generate mobile provisioning profiles to register your iPhone to be able to test your app. And finally, you have to export your certificates into a format that can be used in Codename One. If you have a Mac, this process is annoying at best. If you don’t, then this process might be your show-stopper.

Personally, I find the iOS certificate process to be the single most painful part of app development. That is why I’m happy to announce that the next version of the Codename One plugin will include a wizard that generates all of these things for you with just a few mouse clicks.

## How it works

To generate your certificates and profiles, simply open your project’s properties, and click on “iOS” in the left menu. This will show the “iOS Signing” panel that includes fields to select your certificates and mobile provisioning profiles.

![Netbeans iOS Signing properties panel](/blog/ios-certificate-wizard/ios-cert-wizard-1-signing.png)

If you already have valid certificates and profiles, you can just enter their locations here. If you don’t, then you can use the new wizard by clicking the “Generate” button in the lower part of the form.

### Logging into the Wizard

After clicking “Generate” you’ll be shown a login form. Log into this form using your **iTunes Connect** user ID and password. **NOT YOUR CODENAME ONE LOGIN**.

image::/img/blog/ios-cert-wizard-2-login.png[Wizard login form]

### Selecting Devices

Once you are logged in you will be shown a list of all of the devices that you currently have registered on your Apple developer account.

![Devices form](/blog/ios-certificate-wizard/ios-cert-wizard-3-devices.png)

Select the ones that you want to include in your provisioning profile and click next.

![After selecting devices](/blog/ios-certificate-wizard/ios-cert-wizard-4-devices-selected.png)

If you don’t have any devices registered yet, you can click the “Add New Device” button, which will prompt you to enter the UDID for your device.

### Decisions & Edge Cases

After you click “Next” on the device form, the wizard checks to see if you already have a valid certificate. If your project already has a valid certificate and it matches the one that is currently active in your apple developer account, then it will just use the same certificate. If the certificate doesn’t match the currently-active one, or you haven’t provided a certificate, you will be prompted to overwrite the old certificate with a new one.

![Prompt to overwrite existing certificate](/blog/ios-certificate-wizard/ios-cert-wizard-4.1-overwrite-cert.png)

![Prompt to overwrite other certificate](/blog/ios-certificate-wizard/ios-cert-wizard-4.2-overwrite-cert.png)

The same “decisions” need to be made twice: Once for the development certificate, and once for the Apptore certificate.

### App IDs and Provisioning Profiles

The next form in the wizard asks for your app’s bundle ID. This should have been prefilled, but you can change the app ID to a wildcard ID if you prefer.

![Enter the app bundle ID](/blog/ios-certificate-wizard/ios-cert-wizard-5-bundle-id.png)

### Installing Files Locally

Once the wizard is finished generating your provisioning profiles, you should click “Install Locally”, which will open a file dialog for you to navigate to a folder in which to store the generated files.

![Install files locally](/blog/ios-certificate-wizard/ios-cert-wizard-6-install-now.png)

![Select directory to save files in](/blog/ios-certificate-wizard/ios-cert-wizard-7-select-directory.png)

![All done](/blog/ios-certificate-wizard/ios-cert-wizard-8-complete.png)

### Building Your App

After selecting your local install location, and closing the wizard, you should see the fields of the “iOS Signing” properties panel filled in correctly. You should now be able to send iOS debug or Appstore builds without the usual hassles.

![Filled in signing panel after wizard complete](/blog/ios-certificate-wizard/ios-cert-wizard-9-signing-panel.png)

## Future Improvements

This wizard is just the next step in our mission to simplify the app-development process. In the next while we’ll be rolling out more features like this. Some planned features include push certificate generation and Appstore uploads. If there are particular aspects of the app development and deployment process that you still find cumbersome, make sure to let us know so we can work on finding solutions.

## Screencast

If you want to see the wizard in action, you can check out this 2 minute screencast.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Chidiebere Okwudire** — July 19, 2015 at 1:10 pm ([permalink](/blog/ios-certificate-wizard/#comment-21550))

> Great work, guys! I’ll try this out in the coming weeks


### **Lukman Javalove Idealist Jaji** — July 20, 2015 at 8:23 am ([permalink](/blog/ios-certificate-wizard/#comment-22020))

> Why do we have to select devices? Is that for testing purposes only?


### **Shai Almog** — July 20, 2015 at 2:43 pm ([permalink](/blog/ios-certificate-wizard/#comment-22240))

> Yes. Apple allows running on up to 100 devices and you need to add them to your account as testers.


### **Clement Levallois** — July 23, 2015 at 8:36 am ([permalink](/blog/ios-certificate-wizard/#comment-22384))

> After selecting my device and clicking on Next I get an error saying that my session has expired and I should login again? See pic attached


### **shannah78** — July 23, 2015 at 1:17 pm ([permalink](/blog/ios-certificate-wizard/#comment-22401))

> Not sure yet what would cause that. If you log into your Apple developer account are there any pending agreements that you have to agree to? Are you able to access the “create certificate form” in your apple developer account? Also, does your account have multiple teams? (i.e. when you log into apple developer to you need to select a team?)


### **shannah78** — July 23, 2015 at 3:38 pm ([permalink](/blog/ios-certificate-wizard/#comment-22194))

> Actually I have just made some fixes to the wizard. Please try again and it should work.


### **Clement Levallois** — July 23, 2015 at 7:20 pm ([permalink](/blog/ios-certificate-wizard/#comment-22231))

> It wooorks! Amazing, takes just 2 seconds. Congrats on an amazing job for the community! 🙂


### **sao** — July 28, 2015 at 3:16 pm ([permalink](/blog/ios-certificate-wizard/#comment-21495))

> This is a great achievement. Well done CodeNameOne team


### **Tom Arn** — August 5, 2015 at 1:05 pm ([permalink](/blog/ios-certificate-wizard/#comment-22315))

> That’s fantastic! Especially if you are developping on Windows, certificate creation was such a pain! Very well done!
>
> If I could also do the appstore upload from the Codename One plugin as planned, then I could finally cease using MacInCloud again.  
> That would be an even better enhancement than the certificate wizard, because I do a lot more uploads than I need to create certificates!
>
> Tom


### **Shai Almog** — August 5, 2015 at 2:03 pm ([permalink](/blog/ios-certificate-wizard/#comment-22323))

> It was something we wanted to add but the deployment complexity is far bigger.  
> I want it too since that’s pretty much the only reason to own a Mac and we have at least the rudimentary understanding of how to do that.


### **Chidiebere Okwudire** — August 13, 2015 at 3:54 am ([permalink](/blog/ios-certificate-wizard/#comment-22021))

> Chidiebere Okwudire says:
>
> Hi Steve,
>
> I tried the wizard and it works just fine. However, I have two test apps and I noticed that after using the wizard to generate certificates for the second app, the first one’s certificates are marked as ‘invalid’ and the app no longer runs on my device. Is this a restriction from Apple or from the wizard?


### **Shai Almog** — August 13, 2015 at 4:23 am ([permalink](/blog/ios-certificate-wizard/#comment-22140))

> Shai Almog says:
>
> The wizard asks you if you already have P12 files otherwise it regenerates them for you and effectively invalidates the old ones.


### **Chidiebere Okwudire** — August 13, 2015 at 7:24 am ([permalink](/blog/ios-certificate-wizard/#comment-22365))

> Chidiebere Okwudire says:
>
> So are you saying that I can use the p12 files generated for the first app in all others? I vaguely remember that the build failed when I tried that but I’ll have to recheck based on your answer.


### **Sanny Sanoff** — August 14, 2015 at 8:50 am ([permalink](/blog/ios-certificate-wizard/#comment-24180))

> Sanny Sanoff says:
>
> Wizard button did not appear in the place described here. IDEA 14/15.  
> Installed Macos, Xcode and attached developer account to xcode, tested device build. Restarted IDEA. No luck.  
> Moreover, in IDEA preferences, there are 2 instances of Codename One configuration nodes.


### **Shai Almog** — August 14, 2015 at 1:14 pm ([permalink](/blog/ios-certificate-wizard/#comment-22419))

> Shai Almog says:
>
> That feature isn’t available yet in the IntelliJ version of the plugin.


### **Barak** — November 10, 2015 at 11:41 pm ([permalink](/blog/ios-certificate-wizard/#comment-22152))

> Barak says:
>
> After pressing generate and logging in to my Itunes account  
> I get this message(attached image)  
> any idea how to fix it?
>
> Thanks in advance.


### **Shai Almog** — November 11, 2015 at 4:55 am ([permalink](/blog/ios-certificate-wizard/#comment-21614))

> Shai Almog says:
>
> You shouldn’t get that message so its definitely a bug. Is the page [http://developer.apple.com/…](<http://developer.apple.com/ios/manage/provisioningprofiles/index.action>) reachable or do you see notices that you need to approve something?  
> Dismissing Apple notices is horribly unintuitive.


### **Barak** — November 11, 2015 at 6:35 am ([permalink](/blog/ios-certificate-wizard/#comment-24172))

> Barak says:
>
> Everything seems fine in that page ,I suppose I didn’t ‘enroll’ and paid the 100$ fee.  
> I hope that’s not the problem…  
> is it?


### **Shai Almog** — November 11, 2015 at 8:01 am ([permalink](/blog/ios-certificate-wizard/#comment-22220))

> Shai Almog says:
>
> You need to pay Apple and wait for approval which takes a couple of days.


### **Barak** — November 11, 2015 at 3:39 pm ([permalink](/blog/ios-certificate-wizard/#comment-22234))

> Barak says:
>
> Well that’s unfortunate,thanks anyway!


### **Yngve Moe** — November 14, 2015 at 5:58 pm ([permalink](/blog/ios-certificate-wizard/#comment-22415))

> Yngve Moe says:
>
> I can’t see any login dialog. When i press Generate, I just get the message “This feature requires you to be logged in.” This happens on a fresh install of NetBeans with Codename One plugin, both on Mac and Windows. Am i doing something wrong?


### **Shai Almog** — November 15, 2015 at 4:12 am ([permalink](/blog/ios-certificate-wizard/#comment-22488))

> Shai Almog says:
>
> Yes, I think its a mistake to require that. It was added to simplify the process of generating push certificates.  
> You login in the main Codename One section of the preferences at the bottom of the UI. That might be hidden if you have a small screen.


### **Yngve Moe** — November 15, 2015 at 10:29 am ([permalink](/blog/ios-certificate-wizard/#comment-22299))

> Yngve Moe says:
>
> You’re right, that section was hidden. I found it myself after some googling. Apart from that, the wizard is very simple and smooth – saved me a lot of work!


### **Andrey** — December 21, 2015 at 1:24 pm ([permalink](/blog/ios-certificate-wizard/#comment-22506))

> Andrey says:
>
> I used the free version Codename one and Intellij IDEA. Why is there no button generate? What should I do to make it come from?


### **Shai Almog** — December 22, 2015 at 5:56 am ([permalink](/blog/ios-certificate-wizard/#comment-22599))

> Shai Almog says:
>
> The certificate wizard is available for free. The IntelliJ plugin is a bit out of date with many new features such as Java 8 support etc.  
> We are working on a complete overhaul of the plugin but get side tracked a lot. I hope we’ll be able to bring it into feature parity with the current NetBeans/Eclipse plugins for 3.3.


### **Andrey** — December 22, 2015 at 9:41 am ([permalink](/blog/ios-certificate-wizard/#comment-22532))

> Andrey says:
>
> Shai, thanks for answer. I will try to set up Eclipse or NetBeans.


### **Andrey** — December 23, 2015 at 3:12 pm ([permalink](/blog/ios-certificate-wizard/#comment-22360))

> Andrey says:
>
> Shai, can you answer me, in last version Eclipse for java developers (mars) is there this button? I don’t found that.


### **Shai Almog** — December 24, 2015 at 10:21 am ([permalink](/blog/ios-certificate-wizard/#comment-22185))

> Shai Almog says:
>
> This is what I’m seeing, how did you install the plugin?


### **tomm0** — January 19, 2016 at 3:47 pm ([permalink](/blog/ios-certificate-wizard/#comment-22658))

> tomm0 says:
>
> Does each app needs its own Certificate? I am confused by that aspect. I have just been testing things out, and whenever I choose to Overwrite the certificate I get an email from Apple saying my certificate has been revoked. What should the general process be? Should each app have its own p12 certificate?


### **Shai Almog** — January 20, 2016 at 3:41 am ([permalink](/blog/ios-certificate-wizard/#comment-22515))

> Shai Almog says:
>
> That should indeed be clearer in the wizard, I’m not exactly sure how we can do that though…
>
> You need one set of signing certificates for all your apps. You need to renew them once per year as they do expire.
>
> Provisioning profiles are done per-app so when you run the wizard on a new app just tell it NO when it asks to generate the certificates and point the UI to the P12 files you generated last!
>
> Now this is the point where it gets hard… When doing push you DO need new P12 files that are specific to the app, they are totally unrelated to signing/building and are only used for push. So if you check the “push” checkbox you will need to generate those. You will get an automatic email message with instructions when you check that flag.


### **Paul Willworth** — January 25, 2016 at 6:06 am ([permalink](/blog/ios-certificate-wizard/#comment-21492))

> Paul Willworth says:
>
> This is a great feature. The one thing I don’t understand is where it gets the certificate passwords from. I can complete the wizard process successfully, but when I’m done I find that it has used the value “password” for my certificate passwords. I don’t want it to use that, how do I specify what password the wizard should use for my certificates? I am using Eclipse. I saw some talk in another thread about setting some project level password but I don’t see that.


### **Shai Almog** — January 26, 2016 at 3:16 am ([permalink](/blog/ios-certificate-wizard/#comment-22516))

> Shai Almog says:
>
> I don’t think that’s configurable for the build p12 files. Notice that the security of those files isn’t crucial as they are stored on your local machine. Normally on a Mac when building locally the keys are just protected by your OS password.


### **James Mason** — May 24, 2016 at 12:48 pm ([permalink](/blog/ios-certificate-wizard/#comment-22632))

> James Mason says:
>
> When I run the iOS Certificate Wizard, a window pops up saying “Select Team”. But no teams are listed and there is no response to the Next button either. How do I get beyond this?


### **Shai Almog** — May 25, 2016 at 5:57 am ([permalink](/blog/ios-certificate-wizard/#comment-22643))

> Shai Almog says:
>
> If you navigate to [http://developer.apple.com/…](<http://developer.apple.com/ios/manage/provisioningprofiles/index.action>) do you see teams or something like that?  
> Do you have the ability to create a new certificate/provisioning profile?
>
> Is there something “special” about your account? (Enterprise, University etc.)


### **James Mason** — May 25, 2016 at 4:03 pm ([permalink](/blog/ios-certificate-wizard/#comment-22869))

> James Mason says:
>
> Thanks very much, Shai, for pointing me in the right direction. I think Codename One is great and have used it successfully with no problems to develop an Android app. Now, not being an Apple person, I just have to get by their hurdles to become a registered iOS developer.


### **3lix** — August 22, 2016 at 6:24 am ([permalink](/blog/ios-certificate-wizard/#comment-22814))

> 3lix says:
>
> Hello I am getting the following error “could not create development profile. No matching provisioning profile was found” when I try to generate the certificated.  
> I do have a developer account which I use to login with. (this step is successful)  
> Selecting device step is also successful as I see the newly added device on my developer’s account.  
> I would appreciate any help.


### **Ian** — August 22, 2016 at 9:35 am ([permalink](/blog/ios-certificate-wizard/#comment-21654))

> Ian says:
>
> Hi. I’m trying to use the wizard to generate a certificate. It worked fine previously but now, it isn’t working. It asks me for the devices, I select them, it asks if I want to overwrite existing iOS certificate and I say “Yes”.
>
> But then it doesn’t generate any certificate. Any idea why? I don’t get any error message.


### **3lix** — August 22, 2016 at 3:08 pm ([permalink](/blog/ios-certificate-wizard/#comment-22913))

> 3lix says:
>
> Also I noticed that my “Certificates” screen shows up with “Enable Push Pro Feature only” at the bottom.  
> I am not sure if this is causing any issues?


### **Shai Almog** — August 24, 2016 at 8:03 am ([permalink](/blog/ios-certificate-wizard/#comment-22588))

> Shai Almog says:
>
> There was an issue with Apple changing it’s certificate process, we’ve deployed an update that should fix this.


### **Shai Almog** — August 24, 2016 at 8:04 am ([permalink](/blog/ios-certificate-wizard/#comment-22978))

> Shai Almog says:
>
> There was an issue with Apple changing it’s certificate process, we’ve deployed an update that should fix this.
>
> The enable push is unrelated.


### **3lix** — August 24, 2016 at 3:09 pm ([permalink](/blog/ios-certificate-wizard/#comment-22684))

> 3lix says:
>
> Thank you very much! Looks like its working now,


### **hesham mohamed** — October 20, 2016 at 9:09 pm ([permalink](/blog/ios-certificate-wizard/#comment-23125))

> hesham mohamed says:
>
> Hello, i am getting same Message (Select team ), and i checked my Apple account and i can’t see anything like teams, and don’t know what could be the reason behind !  
> appreciate your help


### **Jacob Rachoene** — October 27, 2016 at 10:42 pm ([permalink](/blog/ios-certificate-wizard/#comment-22796))

> Jacob Rachoene says:
>
> Hi James, have you actually figured out your way around this? Exactly what did you do? Any one has an idea?  
> Thanks…


### **Shai Almog** — October 28, 2016 at 3:45 am ([permalink](/blog/ios-certificate-wizard/#comment-23159))

> Shai Almog says:
>
> You need to have a paid Apple account for this to work


### **Shubhanjan Medhi** — January 26, 2017 at 10:22 am ([permalink](/blog/ios-certificate-wizard/#comment-23027))

> Shubhanjan Medhi says:
>
> Hi, i am unable to generate a certificate using the wizard, it asks me to select a team but there is no team listed. I tried logging into my apple account [https://developer.apple.com…](<https://developer.apple.com/account/#/welcome>) but even there i could not find any certificate for signing.
>
> How do i do it?


### **Shai Almog** — January 27, 2017 at 7:33 am ([permalink](/blog/ios-certificate-wizard/#comment-23220))

> Shai Almog says:
>
> Hi,  
> do you have a paid iOS developer account?  
> Can you generate a CSR in your apple account?
>
> That’s a requirement to generating the certificate.


### **Kai** — February 3, 2017 at 10:16 am ([permalink](/blog/ios-certificate-wizard/#comment-23326))

> Kai says:
>
> Hi,  
> I am using a university dev account, I know that I cannot create Appstore certificates.  
> Using the ios Signing wizard from Codenameone settings panel (NetBeans, latest Plug-in),  
> everything works so far and the certificate + provisioning profile + appid get created.  
> However the needed certificate password is not stored in the [codenameone_settings.proper…](<http://codenameone_settings.properties>) file.  
> Is there a bug? Otherwise I’m having troubles as I do not own a Mac for the CSR.  
> Additionally the created files (I downloaded them from the developer account website, as I couldn’t find them on my system) do not get added into the related fields.
>



### **Kai** — February 3, 2017 at 3:21 pm ([permalink](/blog/ios-certificate-wizard/#comment-23097))

> Kai says:
>
> I fixed it by creating the .p12 file on my own.
>



### **Shai Almog** — February 4, 2017 at 8:34 am ([permalink](/blog/ios-certificate-wizard/#comment-24124))

> Shai Almog says:
>
> Hi,  
> this should work even with a university account
>



### **Kai** — February 6, 2017 at 9:08 am ([permalink](/blog/ios-certificate-wizard/#comment-23104))

> Kai says:
>
> Hi, unfortunately it only creates a .cer file and not the needed .p12 file
>



### **Shai Almog** — February 7, 2017 at 10:08 am ([permalink](/blog/ios-certificate-wizard/#comment-23118))

> Shai Almog says:
>
> I’m assuming you downloaded the cer files from the apple site and didn’t generate them via the tool. Right?
>



### **Kai** — February 7, 2017 at 10:44 am ([permalink](/blog/ios-certificate-wizard/#comment-23127))

> Kai says:
>
> No, I used the tool for the whole process. It shows that the AppStore certificate was not generated, after setting App Name (you should add validation that no special characters are allowed) & App ID, it runs into an error “Could not create appstore profile. No matching provisioning profile was found”. Then it returns to the view where App name and ID are set. Then I tried to download the files from [developer.apple.com](<http://developer.apple.com>), because I couldn’t find any generated/downloaded file on my system, neither are any paths/settings set in the Codenameone ios signature settings.  
> In my case I managed to create the .p12 file and build iOS debug-app, but I could not use the wizard at all.
>



### **Shai Almog** — February 8, 2017 at 9:05 am ([permalink](/blog/ios-certificate-wizard/#comment-23236))

> Shai Almog says:
>
> Then you did download the cer file. P12 files aren’t generated by Apple they can only be generated thru the wizard. Unfortunately we can’t debug the university accounts since we aren’t an educational institute.
>



### **Tommy Mogaka** — February 17, 2017 at 8:43 am ([permalink](/blog/ios-certificate-wizard/#comment-23164))

> Tommy Mogaka says:
>
> Hi Steve… great post and awesome work you guys doing. CN1 is one of the great enablers and equalizers out there.  
> One thing I would like to see is the App Store upload process especially for Apple. Right now I have a challenge as using the Application Loader 3.0 fails saying it is not available right now and that no suitable application records were found and that I should upload my bundle identifier…. What could be the cause of this? And how can I resolve this problem? My App name and App ID as written in netbeans initially didn’t match the one on the developer portal. I edited the portal to match Netbeans but no luck. Any ideas? Any pointers will be highly appreciated.
>



### **Tommy Mogaka** — February 17, 2017 at 9:24 am ([permalink](/blog/ios-certificate-wizard/#comment-23360))

> Tommy Mogaka says:
>
> Got the issue! I had not selected the correct Bundle ID under General information of the app in the developer portal. Now I am getting this error:  
> ERROR ITMS-90168: “The binary you uploaded was invalid”  
> Looks like I have to start over… will clean out the portal of any id, certs and profiles and try again.
>



### **Shai Almog** — February 18, 2017 at 10:47 am ([permalink](/blog/ios-certificate-wizard/#comment-23190))

> Shai Almog says:
>
> It’s a painful process the first time around. We’d like to add an upload to itunes wizard at some point but it’s not a trivial task.
>



### **Chris** — June 16, 2017 at 5:51 pm ([permalink](/blog/ios-certificate-wizard/#comment-23586))

> Chris says:
>
> Hi Shai, I’m encountering same issue now. is there any fix needed on codename server. Through the wizard it says Developer Cert not Generated and Appstore cert not generated. When I complete the process, it creates profiles and certificates but the passwords showing empty. Build is getting failed.
>



### **Shai Almog** — June 16, 2017 at 7:06 pm ([permalink](/blog/ios-certificate-wizard/#comment-23200))

> Shai Almog says:
>
> It’s a regression due to some server changes we made see [https://groups.google.com/d…](<https://groups.google.com/d/msg/codenameone-discussions/DerFbZQK0tU/hlEkRDrwAAAJ>)  
> Should be fixed now
>



### **beck** — September 6, 2017 at 5:57 pm ([permalink](/blog/ios-certificate-wizard/#comment-23746))

> beck says:
>
> “I’m happy to announce that the next version of the Codename One plugin will include a wizard that generates all of these things for you with just a few mouse clicks.” Has it happened yet?
>



### **Shai Almog** — September 7, 2017 at 8:17 am ([permalink](/blog/ios-certificate-wizard/#comment-23627))

> Shai Almog says:
>
> This is a post from 2015… It happened 2 years ago.
>



### **Json** — September 15, 2017 at 11:21 pm ([permalink](/blog/ios-certificate-wizard/#comment-21472))

> Json says:
>
> I’m faced with the same problem.. I’m developing 2 apps, been test building on android with no problems and wanted to do test iOS builds, the howto above is great and I was able to send a debug build and install etc.. however when trying to do the same for the second app I’m stuck, I used the same wizard.. _DID_NOT_ create a new signing cert (as I thought it would invalidate the original certs and it looks like I’m correct) and only got the provisioning profiles created. My question is how do we use the same certs for other apps when we dont know the password? I get build errors which I suspect may be caused by the fact that I dont know the password for the certificates in the first place =(
>



### **Shai Almog** — September 16, 2017 at 5:19 am ([permalink](/blog/ios-certificate-wizard/#comment-23769))

> Shai Almog says:
>
> You need to copy the P12 files from the other project into your new project and update then in the iOS signing section with their passwords.
>



### **Json** — September 17, 2017 at 1:21 pm ([permalink](/blog/ios-certificate-wizard/#comment-23531))

> Json says:
>
> thanks shai! ok I figured out the default password for the p12, certs.. its in the properties file of the other project (of course!), I was able to generate the ios build for the second project with the same cert but diff provisioning profile.
>
> my hats off to you and your team! — from a 20 something year java veteran + 2 week CN1 dev =)
>



### **ZombieLover** — March 18, 2018 at 4:27 pm ([permalink](/blog/ios-certificate-wizard/#comment-21535))

> ZombieLover says:
>
> So we can’t produce apple builds without having a paid Apple account then?
>



### **Shai Almog** — March 19, 2018 at 5:33 am ([permalink](/blog/ios-certificate-wizard/#comment-23870))

> Shai Almog says:
>
> That’s an Apple restriction. When we launched we provided an option to sign with our own certificate but we removed that as we think it violates Apples terms of service. For that to work you had to have a jailbroken device and that’s not a practical requirement today.
>
> Apple provides free certificates for educational institutes & non-profits so you might be able to obtain a certificate through one of those venues.
>



### **ZombieLover** — March 19, 2018 at 6:03 am ([permalink](/blog/ios-certificate-wizard/#comment-23897))

> ZombieLover says:
>
> Thanks for the quick reply
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
