---
title: New iOS Certificate Wizard
slug: new-ios-certificate-wizard
url: /blog/new-ios-certificate-wizard/
original_url: https://www.codenameone.com/blog/new-ios-certificate-wizard.html
aliases:
- /blog/new-ios-certificate-wizard.html
date: '2022-01-25'
author: Steve Hannah
description: We recently released a makeover to the iOS certificate wizard that makes
  it much easier to generate your certificates and provisioning profiles for your
  iOS deployments.
---

We recently released a makeover to the iOS certificate wizard that makes it much easier to generate your certificates and provisioning profiles for your iOS deployments.

![New iOS Certificate Wizard - Codename One](/blog/new-ios-certificate-wizard/New-iOS-Certificate-Wizard-1024x536.png)

The key improvements in this new certificate wizard are:

****• Fewer Steps****: A single form with checkboxes to select which items you want to generate, and then a login prompt. That’s it.

****• Much Faster****: Generate all your certs in 60 seconds. Give or take a few.

****• More Reliable****: We’ve taken steps to avoid network failures, which sometimes occurred due to the long-running back-end generation process.

### Launching The Wizard

The Certicate Wizard can be accessed in the same way as usual. Open Codename One Settings, and select ******Device Settings** > **iOS** > **Certificate Wizard**.****

![Launching The Wizard](/blog/new-ios-certificate-wizard/cert-wizard-menu.png) 

Figure 1. Access the certificate wizard under Device Settings > iOS > Certificate Wizard.

### Screenshots

## Select items to generate

![Codename One - iOS Certificate Wizard](/blog/new-ios-certificate-wizard/certificate-wizard-form.png) 

Figure 2. The first form in the wizard. Select which items to generate.

## Tip

> If your project doesn’t have push enabled, then you won’t see the "Push Certificates" options.

## Log in with Apple Developer credentials

![Codename One - iOS Certificate Wizard](/blog/new-ios-certificate-wizard/apple-login.png) 

Figure 3. Of course you still need to log in using your Apple Developer username and password so that the certificate wizard can generate the certificates for you.

## Certificate generation process

![Codename One - iOS Certificate Wizard](/blog/new-ios-certificate-wizard/please-wait.png) 

Figure 4. You still need to wait for a minute while the server generates your certificates and profiles. If all goes well, this screen will remain for about a minute, though in the case of a failure (e.g. login failure), it should inform you sooner.

## Results and instructions

![Codename One - iOS Certificate Wizard](/blog/new-ios-certificate-wizard/results.png) 

Figure 5. The results and instructions appear when generation is complete.

### Installation Location

The certificates, profiles, and instructions will be saved in the **iosCerts** directory of your Codename One project. If you are using a Maven project structure this means they will be in ******common/iosCerts******.

![Codename One - iosCerts](/blog/new-ios-certificate-wizard/iosCerts.png) 

Figure 6. Results are saved in the iosCerts directory of your project. See the readme.txt file for push instructions.

You can also see the results in the ******Device Settings** > **iOS** > **Signing****** section as shown below.

![Codename One - iOS Signing](/blog/new-ios-certificate-wizard/ios-signing.png) 

Figure 7. The iOS signing section will automatically be updated to reflect the locations of the generated certificates and profiles.

## Tip

> The new certificate wizard also generates a provisioning profile for the Notification Service Extension, which is used in the case that you are using [rich push notifications.](https://www.codenameone.com/blog/tich-push-notification-improved-validation.html)
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved here for historical context. New discussion happens in the Discussion section below._


### **ThomasH99** — March 27, 2022 at 6:48 pm ([permalink](https://www.codenameone.com/blog/new-ios-certificate-wizard.html#comment-24520))

> ThomasH99 says:
>
> Hello, it’s great with this kind of improvements, the certificate wizard is a huge help. However, I just tried this new version to regenerate my certificates and the first steps work fine, I confirm the access with the 6 digit code and it’s running, but then I get an error “Certification generation failed. null”. First I thought it was because my credit card had expired, but it’s fixed and I still can’t generate. Any suggestions for what may be going wrong?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fnew-ios-certificate-wizard.html)


### **ThomasH99** — March 27, 2022 at 7:16 pm ([permalink](https://www.codenameone.com/blog/new-ios-certificate-wizard.html#comment-24521))

> ThomasH99 says:
>
> (Sorry just posted this on an unrelated issue in reddit, it should be here:) I connected to my account and noticed I needed to approve the new license conditions (hadn’t see that on my last connect). After approving it, the error message is different: now it says I need to update to Xcode 7.3. I wonder if that is on my side (I guess not)? As a side note, the Certificate error window is too small to show the entire error message and it’s not possible to copy the text which would have been nice.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fnew-ios-certificate-wizard.html)


### **Steve Hannah** — March 28, 2022 at 1:43 pm ([permalink](https://www.codenameone.com/blog/new-ios-certificate-wizard.html#comment-24522))

> Steve Hannah says:
>
> I have opened an issue for this. <https://github.com/codenameone/CodenameOne/issues/3571>  
> I cannot reproduce this issue myself, so it would be helpful if you would update that issue with some more details that might help to reproduce the issue.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fnew-ios-certificate-wizard.html)


### **Paul Beardow** — September 17, 2023 at 9:32 am ([permalink](https://www.codenameone.com/blog/new-ios-certificate-wizard.html#comment-24572))

> Paul Beardow says:
>
> Is there an easy way to generate ad-hoc builds? I want to test my app on a few friendly users but when I select the ad-hoc option the build fails. It seems the production certificate/profile created by the wizard isn’t the one that includes ad-hoc releases, only app store, so there is a mismatch and an error.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fnew-ios-certificate-wizard.html)


### **Shai Almog** — September 18, 2023 at 2:25 am ([permalink](https://www.codenameone.com/blog/new-ios-certificate-wizard.html#comment-24574))

> Shai Almog says:
>
> The release certificate is the same for ad-hoc builds. You might need to make changes to the provisioning profile which you can do on apples site. The wizard creates a simple one for you but you don’t need it for editing that.
>
> The main reason the wizard is needed is for certificates.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fnew-ios-certificate-wizard.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
