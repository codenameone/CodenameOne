---
title: "Untitled"
date: 2015-03-03
slug: "untitled-2"
---

<script>if (window.location.protocol !== "https:" && window.location.hostname !== "localhost") { window.location.href = "https:" + window.location.href.substring(window.location.protocol.length); }</script>

# Codename One Build Server

This is where you can manage all your builds, settings and subscription details

1. [Home](/)
2. Build Server

<script>shouldPoll = true;</script>

- [Builds](#builds-tab)
- [Subscription](#subscription-tab)
- [Account](#account-tab)

Check out the new [Codename One Build Web App](https://cloud.codenameone.com/buildapp/index.html), [Android App](https://play.google.com/store/apps/details?id=com.codename1.build.app) and [iOS App (Beta)](https://www.codenameone.com/blog/build-app-on-ios.html)  
Your Builds appear here. Notice that older builds are automatically deleted to preserve server space!

<script>function appendFreeSubscriptionOptions() { if(!userData.usedUpTrial) { $("#subscriptionP").append( '<button class="button button-desc button-3d button-rounded button-green center" onclick="postToPaypal(\'4G5VAJ2X5YUJ6\', \'' + userData.email + '\')">Try The Pro Subscription For Free!<span>Two weeks trial with all the bells and whistles</span></button><br/>' + '<button class="button button-3d button-small button-rounded button-purple" onclick="postToPaypal(\'77MN33T6XS2DA\', \'' + userData.email + '\')">Upgrade To Basic</button><br/>' + '<button class="button button-3d button-small button-rounded button-aqua" onclick="postToPaypal(\'H9GGQ3NLVCDQE\', \'' + userData.email + '\')">Upgrade To Pro (Monthly)</button>' + '<button class="button button-3d button-small button-rounded button-aqua" onclick="postToPaypal(\'AXZ2R34CP9JTY\', \'' + userData.email + '\')">Upgrade To Pro (Annually)</button><br/>' + '<button class="button button-3d button-small button-rounded button-teal" onclick="postToPaypal(\'HW2ZXWY8R35VA\', \'' + userData.email + '\')">Upgrade To Enterprise (Monthly)</button>' + '<button class="button button-3d button-small button-rounded button-teal" onclick="postToPaypal(\'6UNRDS4ZFZ7QY\', \'' + userData.email + '\')">Upgrade To Enterprise (Annually)</button>' ); } else { $("#subscriptionP").append( '<button class="button button-3d button-small button-rounded button-purple" onclick="postToPaypal(\'77MN33T6XS2DA\', \'' + userData.email + '\')">Upgrade To Basic</button><br/>' + '<button class="button button-3d button-small button-rounded button-aqua" onclick="postToPaypal(\'H9GGQ3NLVCDQE\', \'' + userData.email + '\')">Upgrade To Pro (Monthly)</button>' + '<button class="button button-3d button-small button-rounded button-aqua" onclick="postToPaypal(\'AXZ2R34CP9JTY\', \'' + userData.email + '\')">Upgrade To Pro (Annually)</button><br/>' + '<button class="button button-3d button-small button-rounded button-teal" onclick="postToPaypal(\'HW2ZXWY8R35VA\', \'' + userData.email + '\')">Upgrade To Enterprise (Monthly)</button>' + '<button class="button button-3d button-small button-rounded button-teal" onclick="postToPaypal(\'6UNRDS4ZFZ7QY\', \'' + userData.email + '\')">Upgrade To Enterprise (Annually)</button>' ); } } function appendBasicSubscriptionOptions() { $("#subscriptionP").append( '<button class="button button-3d button-small button-rounded button-aqua" onclick="postToPaypal(\'H9GGQ3NLVCDQE\', \'' + userData.email + '\')">Upgrade To Pro (Monthly)</button>' + '<button class="button button-3d button-small button-rounded button-aqua" onclick="postToPaypal(\'AXZ2R34CP9JTY\', \'' + userData.email + '\')">Upgrade To Pro (Annually)</button><br/>' + '<button class="button button-3d button-small button-rounded button-teal" onclick="postToPaypal(\'HW2ZXWY8R35VA\', \'' + userData.email + '\')">Upgrade To Enterprise (Monthly)</button>' + '<button class="button button-3d button-small button-rounded button-teal" onclick="postToPaypal(\'6UNRDS4ZFZ7QY\', \'' + userData.email + '\')">Upgrade To Enterprise (Annually)</button><br/><br/>' ); } function appendProSubscriptionOptions() { $("#subscriptionP").append( '<button class="button button-3d button-small button-rounded button-teal" onclick="postToPaypal(\'HW2ZXWY8R35VA\', \'' + userData.email + '\')">Upgrade To Enterprise (Monthly)</button>' + '<button class="button button-3d button-small button-rounded button-teal" onclick="postToPaypal(\'6UNRDS4ZFZ7QY\', \'' + userData.email + '\')">Upgrade To Enterprise (Annually)</button><br/><br/>' ); } $(function() { if(userData !== null) { var subscriptionType = ""; switch(userData.type) { case 1000: // free $("#subscriptionP").append(subscriptionType); appendFreeSubscriptionOptions(); break; case 9000: // basic subscriptionType = "Basic Subscription"; appendBasicSubscriptionOptions(); break; case 11000: // pro case 11001: // pro subscriptionType = "Professional Subscription"; appendProSubscriptionOptions(); break; case 13000: // enterprise subscriptionType = "Enterprise Subscription"; break; case 100000: // cn1 subscriptionType = "Hi Guys"; break; } if(userData.type > 1000) { var daysSince = (new Date().getTime() - userData.subscriptionStartDate) / (24 * 60 * 60000); $("#subscriptionP").append(subscriptionType + "<br/>Remaining Subscription Days: " + Math.floor(userData.subscriptionDays - daysSince)); } $("#subscriptionFooter").append("<br />If you received an upgrade code by email, paste it here: <br /><input type='text' class='form-control' id='subscriptionActivation'>" ); $("#subscriptionActivation").on("input",function(event) { var activation = $("#subscriptionActivation").val(); if(activation.length === 36) { $.alert("Sending activation code: " + activation, {'title': "Checking", autoClose: true}); upgradeCn1UserCode(activation); } }); $("#updateFirstName").val(userData.firstName); $("#updateSurname").val(userData.surname); $("#updateCompanyName").val(userData.companyName); $("#updateEmail").val(userData.email); if(userData.authToken !== null && typeof userData.authToken !== 'undefined') { $("#tokenId").text("Token: " + userData.authToken); if(userData.userId !== null && typeof userData.userId !== 'undefined') { <div></div> $("#refLink").text("REFERRAL LINK: https://www.codenameone.com/index.html?ref=" + userData.userId); } } else { $("#tokenId").text(""); } $("#updateUserDetailsId").submit(function(event) { event.preventDefault(); var firstName = encodeURIComponent($("#updateFirstName").val()); var surname = encodeURIComponent($("#updateSurname").val()); var company = encodeURIComponent($("#updateCompanyName").val()); var updatePassword = encodeURIComponent($("#updatePasswordSignup").val()); var updatePassword2 = encodeURIComponent($("#updatePasswordSignup2").val()); var url = "/calls?m=update&email=" + email + "&firstName=" + firstName + "&surname=" + surname + "&company=" + company + "&password=" + password; if(updatePassword.length > 0 && updatePassword === updatePassword2) { url += "&newPassword=" + updatePassword; } var updateRequest = new XMLHttpRequest(); updateRequest.open('get', url); updateRequest.onreadystatechange = function() { if ((updateRequest.readyState === 4) && (updateRequest.status === 200)){ var result = JSON.parse(updateRequest.responseText); if(result.firstName !== null && typeof result.firstName !== 'undefined') { if(updatePassword.length() > 0 && updatePassword === updatePassword2) { password = updatePassword; } userData = result; userData.password = password; $.cookie("basket-data", JSON.stringify(userData), { expires: 999, path: '/' }); $.alert("Data updated successfully", {'title': "Update Completed", autoClose: true}); } else { $.alert(result.Error, {'title': "Error", autoClose: false}); } } }; updateRequest.send(null); $.alert("Updating data please wait", {'title': "Updating", autoClose: true}); }); } else { $("#builds-tab").append("<h3><span class='label label-danger' style='color: white;'>To see your data here you need to login or signup using the buttons on the top right of the screen</span></h3>"); } });</script>

You can purchase annual pro/enterprise subscriptions via SWIFT bank transfers based on a purchase order invoice by clicking one of these buttons and following the instructions within.  
Annual Professional Subscription  
Annual Enterprise Subscription

If you wish to cancel your subscription you can do so at any time through the PayPal interface  
[![](/uploads/btn_unsubscribe_SM.gif)](https://www.paypal.com/cgi-bin/webscr?cmd=_subscr-find&alias=FM42CH7XVHZP4)  
notice you can use any credit card with PayPal!  

Subscription plans prevent free users from abusing the servers by sending too many build requests and weighing down the servers.  
To learn more about the benefits of various subscription levels and to upgrade your account check out [this page](/pricing.html).

First Name: 

Surname: 

Company Name: 

E-mail: 

Password: 

Password Verification: 

Token: \[Undefined\]

REFERRAL LINK: \[Undefined\]

Update Details  
  
  

<script>$(document).ready(function(){ if(userData === null) { return; } var myRequest = new XMLHttpRequest(); myRequest.open('get', "/calls?m=login&email=" + encodeURIComponent(userData.email) + "&password=" + encodeURIComponent(userData.password)); myRequest.onreadystatechange = function() { if ((myRequest.readyState === 4) && (myRequest.status === 200)){ var pass = userData.password; var data = JSON.parse(myRequest.responseText); if(data.firstName !== null && typeof data.firstName !== 'undefined') { userData = data; userData.password = pass; $.cookie("basket-data", JSON.stringify(userData), { expires: 999, path: '/' }); <div></div> var credits = "Undefined"; if(userData.type === 1000) { credits = "<h5>Build Credits Left: " + userData.buildsRemaining + "<h5/> Credits are renewed automatically to 100 on the 1st of each month."; $("#creditsP").append(credits); } } } }; myRequest.send(null); });</script>
