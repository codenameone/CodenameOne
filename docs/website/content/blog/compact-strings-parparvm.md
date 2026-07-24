---
title: "Compact Strings Cut Character Storage in Half"
slug: compact-strings-parparvm
url: /blog/compact-strings-parparvm/
date: '2026-07-29'
author: Shai Almog
description: "ParparVM now stores common strings in a byte array and uses a char array only when the text needs it. The design cuts character storage in half without adding a second backing-array pointer, and still works with fused allocation."
feed_html: '<img src="https://www.codenameone.com/blog/compact-strings-parparvm.jpg" alt="Compact strings cut character storage in half" /> ParparVM now stores strings in a byte array when every character fits and falls back to a char array for wider text.'
series: ["release-2026-07-24"]
---

![Compact Strings Cut Character Storage in Half](/blog/compact-strings-parparvm.jpg)

[PR #5421](https://github.com/codenameone/CodenameOne/pull/5421) adds compact strings to ParparVM. Strings that fit in Latin-1 now use a `byte[]`; strings that need wider code units continue to use `char[]`.

ParparVM previously stored every Java `String` in a `char[]`. Class names, JSON keys, URLs, numbers, log messages, and much Western European text therefore used two bytes per code unit when one byte was enough.

The implementation follows the basic approach in [JEP 254](https://openjdk.org/jeps/254) for modern HotSpot. ParparVM also had to preserve fused allocation without adding another pointer to every string.

## One field, two possible array types

Using one field for each array type would add a second reference to every string:

```java
// We did not do this.
private byte[] latin1Value;
private char[] utf16Value;
```

Every string would pay for two references even though one is always `null`. On a heap full of short strings, that fixed cost can erase a meaningful part of the saving.

The actual shape keeps one backing reference:

```java
@Fused
public final class String {
    // Holds either byte[] for Latin-1 or char[] for UTF-16.
    private Object value;
    private final int offset;
    private final int count;

    private char charInternal(int i) {
        Object v = value;
        return v instanceof byte[]
                ? (char) (((byte[]) v)[offset + i] & 0xff)
                : ((char[]) v)[offset + i];
    }
}
```

The concrete array type is the encoding marker. There is no second array reference and no separate `coder` field.

{{< mermaid >}}
flowchart TB
    A["new String content"] --> B{"Every code unit\nfits in Latin-1?"}
    B -->|Yes| C["String.value → byte[]\n1 byte per code unit"]
    B -->|No| D["String.value → char[]\n2 bytes per code unit"]
    C --> E["Same String API"]
    D --> E
    E --> F["charAt, compare, hash,\nsearch, native interop"]
{{< /mermaid >}}

This does not halve the complete memory cost of every `String`. Object headers, the one backing reference, and the other fields remain. It halves the character-array storage when the value fits in Latin-1. Short strings save less in absolute terms; long Latin-1 strings approach the full 50 percent backing-store reduction.

## Fused allocation made the layout harder

ParparVM can mark a class with `@Fused`. When the translator recognizes a suitable constructor, it allocates the owner and its child array in one BiBOP block. A `String` and its backing array can therefore need one allocation instead of two.

Before compact strings, the translator knew that `String.value` was a `char[]`. Changing the field to `Object` hid the child array type from the ordinary descriptor check. Dropping fusion would have kept the code simple, but it would have traded away an existing allocation and locality optimization on one of the most frequently created objects in a Java application.

The translator now handles `String.value` as a narrow special case. It accepts only a freshly created `byte[]` or `char[]`, sizes the fused block for that concrete element type, and installs that array in the same `value` slot.

{{< mermaid >}}
flowchart TB
    A["Separate layout: allocate the String object"] --> B["Allocate its byte[] or char[] backing array"]
    B --> C["Translator proves the constructor and array type"]
    C --> D["Fused layout: String fields and backing storage share one BiBOP block"]
{{< /mermaid >}}

There is a garbage-collector constraint here too. The backing array must be initialized before the fused object becomes visible to the concurrent collector. A half-built string is not merely a performance bug; it can become a bad heap edge.

## String operations keep the compact representation

Native string operations identify the backing array once and keep that check outside their character loops. They do not convert compact strings to `char[]` for each operation.

Numeric conversion has a direct Latin-1 path because decimal digits are known to fit. Common concatenation shapes with two to five string parts are lowered to helpers that first determine the result representation, allocate the result once, and fill it directly:

```java
String path = "users/" + userId + "/settings";
```

When every part is already Latin-1, that result stays compact. If any part needs a wider code unit, the result uses `char[]`.

Hand-written native code needed attention as well. Code that previously cast `String.value` directly to `char[]` had to go through coder-aware access. That audit matters because Java-level tests can pass while an old native cast quietly reads the wrong array layout.

## Unicode still takes the correct path

Latin-1 covers code points from 0 through 255. It does not cover Hebrew, Arabic, most Asian scripts, emoji, or the rest of Unicode.

Those strings continue to use `char[]`:

```java
String compact = "Résumé 2026"; // Latin-1: byte[] backing
String wide = "שלום";           // outside Latin-1: char[] backing
```

The representation changes storage inside the VM, not the supported character set. `length()`, `charAt()`, comparison, hashing, substring operations, encoding, and native interop retain their Java behavior.

Application code does not change. Latin-1 strings use less backing storage and remain compatible with fused allocation, native string operations, and the concurrent collector.

Start the release series with [the free and open-source JavaScript port](/blog/javascript-free-open-source/), or read about [calendar synchronization](/blog/calendar-is-not-add-event/), [Bluetooth support](/blog/bluetooth-beyond-ble/), [pure Codename One text editing](/blog/text-input-without-native-overlay/), and [lightweight rich text](/blog/rich-text-without-webview/).

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
