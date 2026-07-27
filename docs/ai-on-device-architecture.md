# Built-in on-device AI architecture

This document is the maintainer reference for the built-in vision,
language, and LiteRT APIs. User-facing examples live in
`docs/developer-guide/Ai-And-Speech.asciidoc`.

## Scope

The public surface is in:

- `CodenameOne/src/com/codename1/ai/vision`
- `CodenameOne/src/com/codename1/ai/language`
- `CodenameOne/src/com/codename1/ai/inference`

The API is part of `codenameone-core` on every target. The default
`CodenameOneImplementation` factories return `null`, so an unsupported
target has deterministic `isSupported() == false` behavior and never falls
back to a cloud service.

Native backends are:

| Target | Vision | Language | `.tflite` inference |
| --- | --- | --- | --- |
| Android | ML Kit | ML Kit | LiteRT |
| iOS | Apple Vision/Core Image; optional ML Kit | Apple Natural Language for identification; ML Kit for translation, Smart Reply, and optional identification | TensorFlow Lite Objective-C; optional Core ML delegate |
| Mac native | Apple Vision/Core Image through Mac Catalyst | Unsupported fallback | Unsupported fallback |
| JavaSE, JavaScript, native Windows/Linux | Unsupported fallback | Unsupported fallback | Unsupported fallback |
| watchOS, tvOS | Unsupported fallback | Unsupported fallback | Unsupported fallback |

Document correction is intentionally Apple-only. The Google ML Kit document
scanner is an interactive Activity camera flow, while the core
`DocumentScanner` contract analyzes an existing `VisionImage`.

## Build-time selection

`maven/platform-feature-catalog` is the authoritative dependency registry.
`PlatformFeatureCatalog.Accumulator` consumes exact class and method
references from the existing bytecode scanners.

The local Maven plugin and BuildDaemon both consume the same source contract:

- `maven/codenameone-maven-plugin/.../AndroidGradleBuilder.java`
- `maven/codenameone-maven-plugin/.../IPhoneBuilder.java`
- `BuildDaemon/src/com/codename1/build/daemon/AndroidGradleBuilder.java`
- `BuildDaemon/src/com/codename1/build/daemon/IPhoneBuilder.java`

BuildDaemon carries a source copy of `PlatformFeatureCatalog.java` because it
is built and deployed separately. Keep that copy byte-for-byte equal to the
Maven module source and run `cmp` as part of validation.

Local source builds replace the generated iOS/Mac project directory before
copying the new result. This is required for granular removal: a plain
directory merge would retain an old Podfile, workspace, Pods directory, or
optional native source after the application stops referencing a feature.

### Android source granularity

The optional Android sources are excluded from `maven/android` compilation
and compiled in the generated application after dependencies are selected.
The port bundle contains:

- one reflection-loaded group dispatcher (`AndroidVisionImpl` or
  `AndroidLanguageImpl`);
- one dependency-neutral group adapter base;
- one source file per concrete ML Kit feature;
- one `AndroidInferenceImpl`, because LiteRT is already a single dependency.

`AndroidGradleBuilder.androidAiAdapterSource()` maps each exact public entry
point to one source. `pruneOptionalAiSources()` deletes every unselected
source before Gradle compiles the generated application. R8 keeps only the
remaining `com.codename1.impl.android.ai` classes so reflection cannot rename
the selected dispatcher or adapter.

| Core entry point | Retained Android source | Gradle dependency |
| --- | --- | --- |
| `TextRecognizer` | `AndroidTextRecognitionAdapter.java` | `com.google.mlkit:text-recognition:16.0.0` |
| `BarcodeScanner` | `AndroidBarcodeScanningAdapter.java` | `com.google.mlkit:barcode-scanning:17.2.0` |
| `FaceDetector` | `AndroidFaceDetectionAdapter.java` | `com.google.mlkit:face-detection:16.1.5` |
| `ImageLabeler` | `AndroidImageLabelingAdapter.java` | `com.google.mlkit:image-labeling:17.0.7` |
| `PoseDetector` | `AndroidPoseDetectionAdapter.java` | `com.google.mlkit:pose-detection:18.0.0-beta3` |
| `SelfieSegmenter` | `AndroidSelfieSegmentationAdapter.java` | `com.google.mlkit:segmentation-selfie:16.0.0-beta5` |
| `LanguageIdentifier` | `AndroidLanguageIdAdapter.java` | `com.google.mlkit:language-id:17.0.6` |
| `Translator` | `AndroidTranslationAdapter.java` | `com.google.mlkit:translate:17.0.3` |
| `SmartReply` | `AndroidSmartReplyAdapter.java` | `com.google.mlkit:smart-reply:17.0.4` |
| `InferenceSession` | `AndroidInferenceImpl.java` | `com.google.ai.edge.litert:litert:1.0.1` |

### Apple method granularity

`CN1Vision.m` and `CN1Language.m` use `__has_include` around each ML Kit
component. The class scanner always links the small Apple system frameworks
needed by a referenced analyzer. It adds a Google ML Kit vision pod only
when it observes both:

1. the concrete analyzer class; and
2. a call to the matching feature-specific selector, such as
   `VisionBackends.mlKitTextRecognition()`.

On iOS, language identification defaults to the system Natural Language
framework. Calling `LanguageBackends.mlKitLanguageIdentification()` opts
into `GoogleMLKit/LanguageID`; translation and Smart Reply map independently
to `GoogleMLKit/Translate` and `GoogleMLKit/SmartReply`. `InferenceSession` selects
`TensorFlowLiteObjC/CoreML`.

The native implementation is disabled for watchOS and tvOS. Mac native uses
the Catalyst slice of the framework-only Apple Vision implementation. The
official Google ML Kit and TensorFlow Lite Objective-C packages do not ship
Mac Catalyst slices, so their catalog entries are marked incompatible with
Catalyst. The builders omit those package dependencies from a Mac-native
build; language and inference consequently use their explicit unsupported
stubs there.

Google ML Kit's iOS binary frameworks contain device `arm64` and simulator
`x86_64` slices, but no `arm64` simulator slice. Catalog entries declare that
constraint independently from Catalyst support. When one of those entries is
selected, both builders set
`EXCLUDED_ARCHS[sdk=iphonesimulator*]=arm64` on the generated application and
Pods projects. The repository's iOS UI and native-test runners also select
`ARCHS=x86_64`, so Apple Silicon hosts use the supported simulator slice
without requiring an application build hint. TensorFlow Lite's XCFramework
does include an `arm64` simulator slice and does not trigger this fallback.
The same Google ML Kit catalog entries declare an iOS 15.5 deployment floor.
The iOS builder folds that value into its existing maximum-target calculation
before generating the app target and Podfile, preventing a lower application
hint from producing an unsatisfiable CocoaPods resolution.
The iOS NEON implementation reports SIMD as unsupported in that x86_64
configuration and aliases its native entry points to the generic scalar
implementation; device and arm64-simulator builds retain the NEON path.

Apple Vision barcode requests use revision 1 only when compiling for an iOS
simulator. Recent simulator runtimes can otherwise complete the default
request without returning observations for valid QR images. Physical devices
retain the current OS revision and its additional symbologies.

## Image and camera contract

`VisionImage` owns defensive copies of its input. It accepts encoded JPEG or
PNG, NV21, and RGBA8888. Android feeds raw NV21 directly to ML Kit and
converts RGBA to a bitmap. Apple creates a `CGImage` directly from NV21 or
RGBA memory and passes it to Vision or ML Kit without an intermediate JPEG
encode/decode.

`VisionImage.fromCameraFrame()` is safe beyond the
`FrameListener.onFrame()` callback because it copies the callback-owned
arrays. `VisionPipeline` allows one request to run and retains only the newest
pending frame. This bounds memory and latency for live OCR, barcode, face,
pose, labeling, or segmentation.

Native results are converted to stable Codename One value types. Geometry is
normalized to the top-left coordinate system. `VisionMetadata` carries the
actual backend id without exposing platform classes.

## Inference contract

`InferenceSession` supports named typed tensors, multiple inputs and outputs,
input resizing, and reusable native sessions. Model sources are bytes,
resources, private files, or the HTTPS-only `ModelCache`. The cache rejects
redirects that downgrade a request to HTTP on ports where redirects are
observable. iOS follows redirects below the portable network layer, so the
cache requires a SHA-256 digest for every iOS download and rejects its unpinned
overload. Identical concurrent fetches for one cache entry coalesce, while
conflicting in-flight content identities fail instead of sharing a temporary
path. File sources are opened by path without copying the model through the
Java heap. Cache promotion verifies that the final file exists and, when
supplied, still matches the requested digest before publishing its path.
Android invokes LiteRT with null output destinations, then reads each result
from its native tensor buffer. This allows value-dependent output dimensions
to resolve before Codename One allocates or copies the result. LiteRT caches
output shapes when no input reallocation occurred, so the Android port
explicitly refreshes each output shape after invocation; both Android builders
retain that package-private LiteRT method name through R8. NPU selection is
best effort when fallback is enabled. Strict NPU requests are rejected on
Android because LiteRT cannot verify that NNAPI delegated every operation,
and on iOS because the Core ML delegate may schedule work on CPU or GPU.

All native sessions and analyzers must be closed. Expensive open, analysis,
and inference work runs off the EDT; completion and error delivery return to
the EDT.

## Permanent cross-platform coverage

`scripts/hellocodenameone` registers three non-screenshot conformance tests:

- `VisionOnDeviceApiTest` covers `VisionImage.fromCameraFrame()` ownership,
  option normalization, capability queries for every analyzer, close
  semantics, and—when a native barcode backend is available—a deterministic
  QR decode through image marshalling, native detection, JSON parsing, format
  normalization, and corner geometry.
- `LanguageOnDeviceApiTest` covers language value/options contracts,
  capability queries for identification, translation, and smart reply, and
  immediate unsupported resources.
- `InferenceOnDeviceApiTest` covers immutable tensors and model sources,
  validation and options, runtime capability reporting, and the unsupported
  session-open contract.

The tests avoid permission prompts, mutable model downloads, and large bundled
models. Their concrete API references are nevertheless part of the application
bytecode, so the Android and Apple source-build jobs exercise the same granular
builder selection used by applications. The tests map to independent
`on-device-vision`, `on-device-language`, and `on-device-inference` features in
`docs/website/data/port_status.json`; fresh per-port reports replace the
bootstrap `not-run` results after the updated suite runs on `master`.

## Adding a feature

1. Add the vendor-neutral API and result types to core.
2. Extend the appropriate implementation SPI under
   `CodenameOne/src/com/codename1/impl`.
3. Add one Android adapter source and one exact
   `androidAiAdapterSource()` mapping.
4. Add the smallest Android dependency to `PlatformFeatureCatalog`.
5. Add the Apple implementation behind a feature-specific compile guard.
6. Add the system framework and optional pod mapping to the catalog.
7. Copy the catalog into BuildDaemon and update both builders if source
   selection changes.
8. Add core API tests, catalog tests, builder-selection tests, Java 8 Android
   compilation, Objective-C syntax compilation, CocoaPods integration, and
   Xcode device/Catalyst builds as applicable. Some Google ML Kit releases
   cannot link an arm64 simulator even though the sources compile there.

Use an isolated Maven repository for validation:

```sh
mvn -Dmaven.repo.local=/private/tmp/cn1-ai-validation-m2 ...
```

Large model families and native runtimes such as Whisper and Stable Diffusion
remain opt-in cn1libs. The core/builder approach is intended for APIs whose
code can be selected cheaply and whose model payloads are supplied by the OS,
downloaded lazily by the native SDK, or supplied by the application.
