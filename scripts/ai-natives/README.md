# On-device AI natives: whisper.cpp and llama.cpp

Codename One builds whisper.cpp (speech to text, the `cn1-ai-whisper` cn1lib)
and llama.cpp (`LlmClient.local`, in core) from pinned upstream tags and
publishes them to the Codename One repository as:

| Artifact | Contents |
|---|---|
| `com.codenameone.natives:cn1-whisper-android:<v>@aar` | `libcn1aiwhisper.so`, four ABIs |
| `com.codenameone.natives:cn1-whisper-ios:<v>` (zip) | `CN1Whisper.xcframework`, device + simulator |
| `com.codenameone.natives:cn1-llama-android:<v>@aar` | `libcn1llama.so`, four ABIs |
| `com.codenameone.natives:cn1-llama-ios:<v>` (zip) | `CN1Llama.xcframework`, device + simulator |

Nothing here is ever committed as a binary, and no application carries these
libraries unless it uses them. `PlatformFeatureCatalog` names the artifacts,
and its entries fire only on a reference:

- whisper: any class in `com/codename1/ai/whisper/`, that is, the cn1lib is a
  dependency;
- llama: a call to `LlmClient.local`.

On Android the builder adds the AAR as a Gradle dependency and lists the
Codename One repository only then. On iOS it downloads the zip, checks it
against the published `.sha256`, extracts the xcframework beside the port
natives (the translator links an `.xcframework` like an `.a`) and, for llama,
enables `INCLUDE_CN1_LLAMA`, which compiles `CN1Llama.m`.

## Our C API, not upstream's

The ports never include `whisper.h` or `llama.h`. Each artifact carries a small
C API of ours (`whisper/src/cn1_whisper.h`, `llama/src/cn1_llama.h`) compiled
against the exact upstream revision it was written for, and the ports call
only that. Upstream structs change layout between releases; the iOS whisper
native once declared `whisper_full_params` by hand, as twelve ints, and handed
`whisper_full` garbage.

Each library also links its own ggml with every symbol hidden: the iOS archive
is prelinked (`ld -r` with an exported-symbols list of `_cn1whisper_*` or
`_cn1llama_*`), and the Android `.so` is built with hidden visibility and
`--exclude-libs,ALL`. An application using both carries two private copies of
ggml and no duplicate symbols. For the same reason both AARs link the static
C++ runtime and leave OpenMP off: two AARs that each ship `libc++_shared.so`
or `libomp.so` fail the application's merge.

## Building and publishing

```sh
scripts/ai-natives/build.sh llama ios        # macOS + Xcode
scripts/ai-natives/build.sh llama android    # NDK 28.2.13676358, JDK 17
scripts/ai-natives/build.sh whisper all build/repo
```

Output is a Maven layout with `.sha256`, `.sha1` and `.md5` beside each file.
The `AI natives` workflow (`.github/workflows/ai-natives.yml`, manual) builds
all four and uploads the layout to R2. Publish **before** merging a change to
the versions: the builders fetch exactly what the catalog names.

To move upstream, change the tag in `versions.properties`, reset the revision
to 1, and update `WHISPER_NATIVES_VERSION` / `LLAMA_NATIVES_VERSION` in
`PlatformFeatureCatalog` and the floors in `AndroidSixteenKbPageFloorTest`.
For a glue or flag change alone, bump the revision. A test holds the
properties and the catalog together.

To test a local build without publishing, point the builders at the staged
layout: `codename1.arg.ios.cn1NativesRepository=file:///path/to/build/repo`
and `codename1.arg.android.cn1NativesRepository=file:///path/to/build/repo`.
