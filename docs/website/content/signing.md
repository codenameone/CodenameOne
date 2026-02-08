---
title: "Signing"
date: 2015-03-03
slug: "signing"
---

# Signing

Code signing and provisioning for the various platforms

1. Home
2. Developers
3. Signing

**Important:** this document is out of date! Please check out the developer guide section [**here**](https://www.codenameone.com/developer-guide.html)

While Codename One can simplify most of the grunt work in creating cross platform mobile applications, signing is not something that can be significantly simplified since it represents the developers individual identity in the markets. In this document we attempt to explain how to acquire certificates for the various markets and how to set them up to the build.

The good news is that this is usually a "one time deal" and once its done the work becomes easier (except for the case of iOS where a provisioning profile should be maintained).

## iOS (iPhone/iPad)

iOS signing has two distinct modes: App Store signing which is only valid for distribution via iTunes (you won't be able to run the application yourself) and development mode.

Signing for iOS can be accomplished using the [Codename One certificate wizard](/blog/ios-certificate-wizard.html), for the advanced cases where you already have a certificate or the wizard can't answer your requirements you can follow the guide below.

You have two major files to keep track of: Certificate - your signature Provisioning Profile - details about your application and who is allowed to run it

You need two of each file (4 total files) one pair is for development and the other pair is for that precious moment when you are ready to upload to the App Store.

**Warning: this process only works on a Mac, the Adobe process linked here for Windows will produce an invalid certificate that will not WORK!** Adobe wrote a pretty [decent signing tutorial](http://help.adobe.com/en_US/as3/iphone/WS789ea67d3e73a8b2-240138de1243a7725e7-8000.html) on this subject for Air which you can pretty much follow to get the files we need (p12 and mobileprovision). They also cover the [certificate to p12 conversion process](http://help.adobe.com/en_US/as3/iphone/WS144092a96ffef7cc-371badff126abc17b1f-7fff.html). Currently as far as we know, the best option for developers who don't have access to a Mac is to use a service like [http://www.macincloud.com/](http://www.macincloud.com/).

The first step you need to accomplish is signing up as a developer to [Apple's iOS development program](http://developer.apple.com/), even for testing on a device this is required!

The Apple website will guide you through the process of applying for a certificate at the end of this process you should have a distribution and development certificate pair. After that point you can go to the [iOS provisioning portal](https://developer.apple.com/ios/manage/overview/index.action) where there are plenty of videos and tutorials to guide you through the process. You need to create an application ID and register your development devices.

You then create a provisioning profile which comes in two flavors: distribution (for building the release version of your application) and development. The development provisioning profile needs to contain the devices on which you want to test.

You can then configure the 4 files in the IDE and start sending builds to the Codename One cloud.

If you have problems along the way or fail to install the resulting files check out the troubleshooting guide [here](/blog/ios-code-signing-fail-checklist.html).

## Android

Its really easy to sign Android applications if you have the JDK installed. Find the keytool executable (it should be under the JDK's bin directory) and execute the following command:

```
keytool -genkey -keystore Keystore.jks -storetype JKS -alias [alias_name] -keyalg RSA -keysize 2048 -validity 15000 -dname "CN=[full name], OU=[ou], O=[comp], L=[City], S=[State], C=[Country Code]" -storepass [password] -keypass [password]
```

The elements in the brackets should be filled up based on this:

- Alias: \[alias\_name\] (just use your name/company name without spaces)
- Full name: \[full name\]
- Organizational Unit: \[ou\]
- Company: \[comp\]
- City: \[City\]
- State: \[State\]
- CountryCode: \[Country Code\]
- Password: \[password\] (we expect both passwords to be identical)

Executing the command will produce a Keystore.ks file in that directory which you need to keep since if you lose it you will no longer be able to upgrade your applications! Fill in the appropriate details in the project properties or in the CodenameOne section in the Netbeans preferences dialog.

For more details see [http://developer.android.com/guide/publishing/app-signing.html](http://developer.android.com/guide/publishing/app-signing.html)

## BlackBerry

You can now get signing keys for free from Blackberry by going [here](https://www.blackberry.com/SignedKeys/). After obtaining the certificates and installing them on your machine (you will need the Blackberry development environment for this). You will have two files: sigtool.db and sigtool.csk on your machine (within the JDE directory hierarchy). We need them and their associated password to perform the signed build for Blackberry application.

## J2ME

Currently signing J2ME application's isn't supported. You can use tools such as the Sprint WTK to sign the resulting jad/jar produced by Codename One.
