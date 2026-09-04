---
title: "Pick one contact without taking the address book"
slug: 2026-09-09-0400-codenameone-private-contact-picker
platform: linkedin
account: codenameone
source_slug: private-contact-picker
publish_at: '2026-09-09T04:00:00'
timezone: Asia/Jerusalem
image: /blog/private-contact-picker.jpg
---

A share screen needs one phone number. A delivery form needs one address. Neither needs a database of everybody the user knows.

`ContactPicker` now asks the system for specific fields and returns only the contacts the user selects.

Android 17 uses the new system Contact Picker. Older Android versions use a permission-free single-contact fallback. iOS uses `CNContactPickerViewController` without broad contacts access. Unsupported ports expose that limit so the application can keep manual entry visible.

The result is a snapshot, not a standing token for future address-book queries. Multi-selection and required fields remain platform-dependent because the system surface owns the choice.

The builder also learned the difference between using a Contact value returned by the picker and calling a broad contact reader. A narrow Java API would be meaningless if packaging added the broad permission anyway.

A narrow feature now has a narrow permission surface.

{{canonical}}
