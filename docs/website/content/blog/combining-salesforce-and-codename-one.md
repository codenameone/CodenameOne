---
title: Combining SalesForce and Codename One
slug: combining-salesforce-and-codename-one
url: /blog/combining-salesforce-and-codename-one/
original_url: https://www.codenameone.com/blog/combining-salesforce-and-codename-one.html
aliases:
- /blog/combining-salesforce-and-codename-one.html
date: '2013-03-25'
author: Shai Almog
---

![Header Image](/blog/combining-salesforce-and-codename-one/combining-salesforce-and-codename-one-1.jpg)

This is a guest post by Bertrand Cirot, based on an original blog post that appeared  
[  
here  
](http://www.tuto-codenameone.ch/salesforce-et-codename-one/)  
. Bertrand works as an SFDC, Flex and Java/J2EE developer in Switzerland. Closely interested in mobile device solutions, he writes on  
[  
Tuto-CodenameOne.ch  
](http://www.tuto-codenameone.ch/)  
which proposes tutorials for the French-speaking community.  

* * *

While investigating the possibilities within Codename One, I made a discovery that I would like to share with you via this short article. But first lets discuss SalesForce, it is a CRM platform. Which has an advantage of an open Web Service API enabling it for use with Codename One. 

**  
This is an ideal combination that provides simple and effective customer management on all mobile devices.  
**

## SalesForce, a complete platform  

  
  
  
[  
![Picture](/blog/combining-salesforce-and-codename-one/combining-salesforce-and-codename-one-1.jpg)  
](http://www.salesforce.com/)

SalesForce is a CRM (Customer Relationship Management) platform that is fully hosted in the cloud, as indicated by it’s logo. SalesForce (SFDC) is a leader in sales management tools, it provides a complete and evolutionary system. Many tools are available and configurable in a simple and efficient way to manage all the essential information about the sales activity. In addition, the platform is fully integrated with communication tools, social networking and instant messaging. 

* * *

[  
![Picture](/blog/combining-salesforce-and-codename-one/combining-salesforce-and-codename-one-2.png)  
](/img/blog/old_posts/combining-salesforce-and-codename-one-large-4.png)

Once You connect the SalesForce interface, you can easily add standard data: Users, Accounts, Products, Contacts, etc. Everything is added via forms based interface, If you wish, you can also add new fields to the Available Objects or create new ones from scratch. 

SalesForce can therefore easily form a set of custom data hosted in the Cloud. More importantly for mobile development, the SFDC platform offers several communication API’s allowing third party applications access to its data (SOAP, Bulk or REST). 

Personally recommend the REST architecture mainly for bandwidth reasons, even though it is more restrictive in terms of implementation and use.

## SalesForce One and Codename: a winning combination  

[  
![Picture](/blog/combining-salesforce-and-codename-one/combining-salesforce-and-codename-one-3.png)  
](http://wiki.developerforce.com/page/REST_API)

In order to make ​​a Codename One application communicate with SalesForce, I performed some tests. 

My starting point was the official documentation of the REST API: I soon managed to authenticate and execute a query to retrieve a list of distant objects.

## What’s next?  

Still, I could not stop there so I decided to use the opportunity to try the new library support that was recently introduced to Codename One and create a SalesForce library. There is still a lot of work to be done, there are bugs and only a small part of the API is exposed in the library. I will try to tackle these issues one at a time and I will keep you informed of my progress. If you are interested, the code is available on Google Code under the name:  
[  
CodenameOne-SFDC-Lib  
](https://code.google.com/p/codenameone-sfdc-lib/)  
. 

**  
If you want to help me in pushing forward the development of the library faster, do not hesitate to contact me, I’d love to have some help  
**  
.

Bertrand CIROT

writer on  
[  
Tuto-CodenameOne  
](http://www.tuto-codenameone.ch/)

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
