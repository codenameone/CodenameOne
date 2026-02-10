---
title: Android Push Changes
slug: android-push-changes
url: /blog/android-push-changes/
original_url: https://www.codenameone.com/blog/android-push-changes.html
aliases:
- /blog/android-push-changes.html
date: '2024-06-08'
author: Steve Hannah
---

![](/blog/android-push-changes/4191c4440219cf07f21cdbfd7524bf49.webp-copy.jpg)

We have made some upgrades to our push notification server API. If you deploy apps to Android and use push notifications, you will need to make a small change to the server-side code that sends the HTTP request to our push server. If you do not send push notifications to Android devices, you can ignore this PSA.

## The Short Version

Our Push API now requires a JSON key instead of the old FCM API key. For example, the following is a template for a push notification, (copied from the [push cheatsheet](https://www.codenameone.com/files/push-cheatsheet.pdf)).
    
    
    https://push.codenameone.com/push/push?token=PUSH_TOKEN
     &device=DEVICE_ID1&device=DEVICE_ID2&...&device=DEVICE_IDN
     &type=PUSH_TYPE{1|2|3|4|5|99|100|101}
     &auth=FCM_SERVER_API_KEY
     &certPassword=ITUNES_CERT_PASSWORD
     &cert=ITUNES_CERT_URL
     &body=MESSAGE_BODY
     &production=ITUNES_PRODUCTION_PUSH{true|false}
     &sid=WNS_SID
     &client_secret=WNS_CLIENT_SECRET

Currently, the `auth` parameter will be set to an FCM API key, it might look something like `auth=AAAABbbbCccDddEeeeFfffGGggHhhhIiiiJjjjKkkkLlllMmmmNnnnOooPppQqqRrrrSsssTttUuuuVvvvWxxxYyyyZzzz`

You will need to change this parameter to be a URL (reachable by the Codename One push server) to a the JSON key for your service. It will look something like:
    
    
    auth=https%3A%2F%2Fexample.com%2Fsecret%2Fpath%2Fto%2Fservice-account-file.json

You can find instructions on how to generate this `service-account-file.json` (your JSON key) in the Firebase documentation [here](https://firebase.google.com/docs/cloud-messaging/auth-server#provide-credentials-manually). The following instructions are copied from there:

**To generate a private key file for your service account:**

> In the Firebase console, open **Settings > [Service Accounts](https://console.firebase.google.com/project/_/settings/serviceaccounts/adminsdk)**.  
> Click **Generate New Private Key** , then confirm by clicking **Generate Key**.  
> Securely store the JSON file containing the key.

> ## The Slightly Longer Version

Google deprecated their legacy FCM APIs on June 20, 2023, and will be removing them on June 21, 2024. Our push servers use this API for all push notifications to Android devices, so we were required to [migrate ](https://firebase.google.com/docs/cloud-messaging/migrate-v1)to their new “v1 HTTP API”. This migration involved some non-trivial changes to our push infrastructure, including, but not limited to, changing from using an FCM API key for authentication, to using OAuth2.

The OAuth2 authentication is more complicated than the old API key flow, as it involves multiple pieces of information, including the client ID, client secret, and project ID. To simplify this, Google encapsulates all of these credentials inside a single JSON file which can be used in an opaque manner via its Firebase SDKs.

On our side, we wanted to avoid unnecessary changes to our API to make this transition as seamless as possible for our users, so we haven’t added or removed any parameters from the request. We just changed the `auth` parameter to include the URL to your `service-account-file.json`, instead of an FCM API key. We chose not to include the whole JSON file contents in each request because the JSON file tends to be quite large and presents unnecessary a network overhead. 

We may iterate on this approach in a non-breaking way based on user feedback.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
