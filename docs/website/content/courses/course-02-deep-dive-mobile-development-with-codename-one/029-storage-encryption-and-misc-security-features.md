---
title: "Storage Encryption and Misc Security Features"
layout: "course-lesson"
course_id: "course-02-deep-dive-mobile-development-with-codename-one"
course_title: "Deep Dive into Mobile Development with Codename One - Free Online Course Material"
module_title: "Security"
module_key: "09-security"
module_order: 9
lesson_order: 2
weight: 29
is_course_lesson: true
description: "Use storage encryption and other defensive features where they solve a real application problem."
---
> Module 9: Security

{{< youtube GMnWMB_JfaQ >}}

Once transport security is in place, the next questions are usually local ones: what happens if the device is compromised, if a screenshot leaks sensitive information, or if another app can inspect copied data? These are the kinds of risks the second security lesson is trying to address.

Storage encryption is the clearest example. If the app stores sensitive information locally, encrypting that storage can add a useful defensive layer. But encryption is never just "turn it on and forget it." Key management becomes the real issue immediately. If the decryption key is easy for an attacker to recover from the app itself, then the protection is weaker than it first appears. That is why the lesson discusses trade-offs like server-provided keys versus offline usability.

There is also an important distinction between storage mechanisms. The older lesson is right to separate Codename One `Storage` from lower-level filesystem access and SQL persistence. The abstraction level matters because it determines where transparent protections can realistically be applied and where you need a more explicit design.

The other features discussed here are more situational, but still useful when the threat model justifies them. Screenshot blocking can reduce accidental disclosure on some platforms. Copy/paste restrictions can help for highly sensitive fields. Jailbreak or root detection may serve as one signal among several, though it should never be treated as a perfect truth oracle because compromised devices are designed to hide that fact.

So the practical rule for these features is simple: use them when they protect something concrete, and do not confuse them with complete security. They are layers, not guarantees. Applied carefully, they raise the cost of attack and reduce common mistakes. Applied blindly, they mostly add friction.

## Further Reading

- [Developer Guide](/developer-guide/)
- [Security Basics and Certificate Pinning](/courses/course-02-deep-dive-mobile-development-with-codename-one/028-security-basics-and-certificate-pinning/)
- [How Do I Use Crash Protection, Get Device Logs](/how-do-i/how-do-i-use-crash-protection-get-device-logs/)
