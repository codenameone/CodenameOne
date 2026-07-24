---
title: "Compact strings cut character storage in half"
slug: 2026-07-29-1200-shai-compact-strings-parparvm
platform: linkedin
account: shai
source_slug: compact-strings-parparvm
publish_at: '2026-07-29T12:00:00'
timezone: Asia/Jerusalem
review_by: '2026-07-24'
status: draft
image: /blog/compact-strings-parparvm.jpg
---

We added compact strings to ParparVM this week.

Latin-1 text now uses a `byte[]`. Other text keeps the full `char[]` representation. That cuts the character storage in half for strings such as URLs, JSON keys, class names, numbers, and much Western European text.

Using separate fields for `byte[]` and `char[]` would make every string carry two references when only one can be useful.

We kept one `Object value` field instead. The concrete array type is the encoding marker. Then we taught fused allocation to place either kind of array inline with the String in the same BiBOP block.

The result has no second backing pointer, no reduction in Unicode support, and no application code change. String operations preserve the compact representation, and wider text continues to use `char[]`.

{{canonical}}
