---
title: Server Initiated Push
slug: server-initiated-push
url: /blog/server-initiated-push/
original_url: https://www.codenameone.com/blog/server-initiated-push.html
aliases:
- /blog/server-initiated-push.html
date: '2015-04-15'
author: Shai Almog
---

![Header Image](/blog/server-initiated-push/push.jpg)

Sending a push notification from the simulator or mobile device is pretty trivial when we use the `Push`  
class. However, sending push messages from the server seems to be a bit more complicated for most developers  
since its not as well documented. The main point of complexity is that we didn’t provide any samples of server  
push code and from the fact that the server expects arguments as POST. 

This code should work for Java desktop and server side to perform a simple push, notice that the complexity is  
mostly related to JavaSE’s lack of simplified POST arguments. 
    
    
    URLConnection connection = new URL("https://codename-one.appspot.com/sendPushMessage").openConnection();
    connection.setDoOutput(true); 
    connection.setRequestMethod("POST");
    connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
    String query = "packageName=PACKAGE_NAME&[[email protected]](/cdn-cgi/l/email-protection)&device=" + deviceId +
                        "&type=1&auth=GOOGLE_AUTHKEY&certPassword=CERTIFICATE_PASSWORD&" +
                        "cert=LINK_TO_YOUR_P12_FILE&body=" + URLEncoder.encode(MESSAGE_BODY, "UTF-8"); 
    try (OutputStream output = connection.getOutputStream()) {
        output.write(query.getBytes("UTF-8"));
    }
    int c = connection.getResponseCode();
    

The PHP code below was sent to us by a developer, we didn’t test it but got feedback that it works fine. 
    
    
    $args = http_build_query(array(
    'certPassword' => 'CERTIFICATE_PASSWORD',
    'cert' => 'LINK_TO_YOUR_P12_FILE',
    'production' => false,
    'device' => $device['deviceId'],
    'packageName' => 'YOUR_APP_PACKAGE_HERE',
    'email' => 'YOUR_EMAIL_ADDRESS_HERE',
    'type' => 1,
    'auth' => 'YOUR_GOOGLE_AUTH_KEY',
    'body' => $wstext));
    $opts = array('http' =>
        array(
            'method'  => 'POST',
            'header'  => 'Content-type: application/x-www-form-urlencoded',
            'content' => $args 
        )
    );
    $context = stream_context_create($opts);
    $response = file_get_contents("https://codename-one.appspot.com/sendPushMessage", false, $context);

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
