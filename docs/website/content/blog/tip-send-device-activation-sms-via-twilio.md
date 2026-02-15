---
title: 'TIP: Send Device Activation SMS via Twilio'
slug: tip-send-device-activation-sms-via-twilio
url: /blog/tip-send-device-activation-sms-via-twilio/
original_url: https://www.codenameone.com/blog/tip-send-device-activation-sms-via-twilio.html
aliases:
- /blog/tip-send-device-activation-sms-via-twilio.html
date: '2017-09-11'
author: Shai Almog
---

![Header Image](/blog/tip-send-device-activation-sms-via-twilio/tip.jpg)

A very common question we get from developers is “how do I get the devices phone number”. The answer is “you can’t really and you shouldn’t”. To clarify, this is possible on Android but would require a scary set of permissions. It’s blocked on iOS completely though so you’d need a different solution anyway…​  
If you look at apps like Uber, whatsapp etc. they all use SMS activation. They just ask you to type your number and activate your phone via SMS. Usually this SMS is sent from the server side but for simplicity lets discuss how this can be done entirely from your app.

__ |  You should send the SMS from the server for security reasons as the Twillo keys will never appear in the client side   
---|---  
  
For simplicities sake I’ll break this into several different pieces and the first of which is the REST request to the Twilio API to send the message. This is pretty trivial, you would need to signup to Twilio and have the following 3 variable values:
    
    
    String accountSID = "----------------";
    String authToken = "---------------";
    String fromPhone = "your Twilio phone number here";

__ |  You can open a trial Twilio account and it just tags all of your SMS’s. Notice you would need to use a US based number if you don’t want to pay   
---|---  
  
I can also generate a 4 digit value variable pretty easily using something like this:
    
    
    Random r = new Random();
    String val = "" + r.nextInt(10000);
    while(val.length() < 4) {
        val = "0" + val;
    }

We can now send val as an SMS to the end user. Once this is in place sending an SMS via REST is just a matter of using the new REST callback API:
    
    
    Response<Map> result = Rest.post("https://api.twilio.com/2010-04-01/Accounts/" + accountSID + "/Messages.json").
            queryParam("To", destinationPhone).
            queryParam("From", fromPhone).
            queryParam("Body", val).
            header("Authorization", "Basic " + Base64.encodeNoNewline((accountSID + ":" + authToken).getBytes())).
            getAsJsonMap();

Notice that there is currently a bug with the builtin basic authentication in the REST API, it will be fixed for the next version where you could write this instead:
    
    
    Response<Map> result = Rest.post("https://api.twilio.com/2010-04-01/Accounts/" + accountSID + "/Messages.json").
            queryParam("To", destinationPhone).
            queryParam("From", fromPhone).
            queryParam("Body", val).
            basicAuth(accountSID, authToken)).
            getAsJsonMap();

What we do here is actually pretty trivial, we open a connection the the api messages URL. We add arguments to the body of the post request and define the basic authentication data.

The result is in JSON form we mostly ignore it since it isn’t that important but it might be useful for error handling. This is a sample response (redacted keys):
    
    
    {
        "sid": "[sid value]",
        "date_created": "Sat, 09 Sep 2017 19:47:30 +0000",
        "date_updated": "Sat, 09 Sep 2017 19:47:30 +0000",
        "date_sent": null,
        "account_sid": "[sid value]",
        "to": "[to phone number]",
        "from": "[from phone number]",
        "messaging_service_sid": null,
        "body": "Sent from your Twilio trial account - 2222",
        "status": "queued",
        "num_segments": "1",
        "num_media": "0",
        "direction": "outbound-api",
        "api_version": "2010-04-01",
        "price": null,
        "price_unit": "USD",
        "error_code": null,
        "error_message": null,
        "uri": "/2010-04-01/Accounts/[sid value]/Messages/SMe802d86b9f2246989c7c66e74b2d84ef.json",
        "subresource_uris": {
            "media": "/2010-04-01/Accounts/[sid value]/Messages/[message value]/Media.json"
        }
    }

Notice the error message entry which is null meaning there was no error, if there was an error we’d have a message there or an error code that isn’t in the 200-210 range.

This should display an error message to the user if there was a problem sending the SMS:
    
    
    if(result.getResponseData() != null) {
        String error = (String)result.getResponseData().get("error_message");
        if(error != null) {
            ToastBar.showErrorMessage(error);
        }
    } else {
        ToastBar.showErrorMessage("Error sending SMS: " + result.getResponseCode());
    }

### Next Time

Next time I’ll talk about doing the actual activation process and maybe discuss some of the more ambitious behaviors such as SMS interception. Ideally this is something that should fit into a cn1lib which might be something I’ll cover moving forward.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Gareth Murfin** — October 5, 2018 at 8:23 pm ([permalink](/blog/tip-send-device-activation-sms-via-twilio/#comment-24079))

> Gareth Murfin says:
>
> Awesome, I would LOVE a twilio library for cn1,to abstract away from their HELL.. ! using it right now on android, what a mess….
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
