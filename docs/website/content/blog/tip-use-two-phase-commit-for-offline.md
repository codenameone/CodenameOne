---
title: 'TIP: Use the Two Phase Commit Algorithm for Offline Support'
slug: tip-use-two-phase-commit-for-offline
url: /blog/tip-use-two-phase-commit-for-offline/
original_url: https://www.codenameone.com/blog/tip-use-two-phase-commit-for-offline.html
aliases:
- /blog/tip-use-two-phase-commit-for-offline.html
date: '2018-07-29'
author: Shai Almog
---

![Header Image](/blog/tip-use-two-phase-commit-for-offline/tip.jpg)

I used to do a lot of enterprise consulting in the day and used to lecture a lot on J2EE (as it was known back then). A lot of that knowledge isn’t as applicable even in the server side today, but the algorithms are surprisingly even applicable in mobile.  
One of the algorithms I would explain a lot when teaching J2EE was the 2PC AKA Two Phase Commit.

This is builtin to enterprise servers but I won’t go into that here, there is plenty of discussion about that online. What matters to me is how this can be used in a mobile context to build an app that’s friendly for offline/online modes.

### What’s 2PC

To understand 2PC you need to understand what’s a transaction. I hope you do but just in case you don’t here’s an oversimplified definition: A transaction contains more than one operation where we want all the operations to succeed or we want all of them to fail.

This is explained with a simple example of a transfer between two bank accounts: If I transfer $5 from my account to my friends account I want the deduction of $5 to be in the same transaction as adding the $5. Otherwise if there is a failure money can evaporate and vanish!

Here’s where 2PC comes in, say my account is in one bank and my friends is in another bank in a different server (obviously this is more complicated, again over simplifying). How would that work?

The two phase commit splits every transaction into two phases:

  * Perform the operations of the transaction but don’t commit

  * Perform the actual commit

That way a system can send all the separate operations to all the servers (yes there can be more than two…​) and when all the servers say they can perform the commit it will notify them all to commit. There are still points of failure in this system but usually when it fails it will be more consistent.

### Working Offline in a Mobile Device

Offline synchronization is one of the hardest things to do right in mobile. Especially in cases where your network is unreliable.

Some apps just fail and ask for a valid network connection. But there’s another way.

Assuming you have a cached local database you can let the user keep working locally and then synchronize the data with the server once you get access. This obviously carries several risks:

  * Conflict – user changes might conflict with changes going on in the server while he is working offline

  * Synchronization – you would need to somehow synchronize the changes to the server

  * Connection Reliability – if server synchronization fails the client and server might be left in an illegal state

That’s where the key ideas of two phase commit become applicable.

#### Command Pattern

Don’t be confused with the Codename One `Command` class. The GoF `Command` pattern represents a queue of tasks that allow us to perform the tasks in order. Assuming your server contains a set of REST calls a command would probably map to each call.

So if we proceed with the bank account transfer analogy I’ll have a command that says “transfer $5 from account X to account Y”. The implementation of said command could just invoke the REST API. However, when we are offline we can just log the command to run later when we are online.

We can create a queue of offline commands and wait for the moment we get a stable server connection so we can send the commands in queue.

#### All, Nothing or Conflict

Here’s the tricky part. The server might have changed and the client might have multiple commands.

In an ideal world we can just perform the REST call for each command and be done with it. In practice this can make things worse. If the server changed we can get a conflict. We can also succeed with some operations and fail with others which can create an illegal state in the client.

The solution is to have a similar command abstraction in the server to support the offline mode. So when we go back online instead of sending the regular REST requests we’ll send a special set of commands e.g.:

  * `/beginTransaction` – would return a transaction id. It might also accept a timestamp which could be useful for conflict detection on the server. The server can use timestamps on database entries to indicate when they were changed. Your timestamp would indicate when you last fetched the database so if an entry was changed since you fetched the database you might have a conflict.

  * Send REST calls with special `transaction=id` parameter or HTTP header. These operations won’t commit but would return an error in case of a failure. Handling this error is the interesting part. You can do that per-command and let the user decide if this command is crucial or not. You can offer a UI to merge a command or discard it. You can offer to discard all local changes etc.

  * Assuming all went well you can send a `commit` call.

This sounds a bit difficult but once you divide things correctly and make them modular enough it isn’t very hard.

### Summary

You need to think in advance about offline/online in some systems. If you are building a social network it isn’t a big deal if some data is stale or offline behavior isn’t perfect. But if you are building something that needs reliability you need to understand this theory.

Some tools try to abstract these ideas. That sometimes creates a situation of automatic merges (or failures) that don’t give the user enough control. Even if you use such a tool you need to have a decent understanding of the underlying logic.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Francesco Galgani** — July 30, 2018 at 10:32 pm ([permalink](/blog/tip-use-two-phase-commit-for-offline/#comment-23873))

> Francesco Galgani says:
>
> It’s very interesting. Thank you!
>
> About the knowledge, my opinion is that the most of things expires soon… but the good ideas will never expire.
>



### **ZombieLover** — September 4, 2018 at 10:46 am ([permalink](/blog/tip-use-two-phase-commit-for-offline/#comment-24078))

> ZombieLover says:
>
> I used a local SqlLite DB and a server side DB that stores the transactions in a large txt field  
> Users can upload changes which basically sends the queries they executed on local db and a timestamp (send via rest API)  
> Downloading changes then gets all the stored queries on the Server after the timestamp they last updated their local db with (which is kept in a local variable) and executes them on localDB
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
