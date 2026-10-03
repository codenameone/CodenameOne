# Android compatibility samples

Classic Android applications (activities, XML layouts, `res/` values and
drawables, framework widgets) kept as ordinary Android Studio projects. Each one
builds unmodified with the Android Gradle plugin, and is imported into a
Codename One application with:

```bash
mvn cn1:import-android-project -Dcn1.android.import=scripts/android-compat-samples/<name>
```

`maven/integration-tests/android-compat-test.sh` does that for every sample and
checks what the build produced.

| Sample    | Covers |
|-----------|--------|
| `gallery` | `LinearLayout` weights, styles and theme attributes, `values-night`, selector and vector drawables, plurals, assets, `SharedPreferences`, `android:onClick`, explicit intents with extras and results, the options menu and overflow, the compound buttons, progress, seek and rating bars, `ScrollView`, `RelativeLayout`, `ListView` with a recycling `BaseAdapter`, `Spinner`, fragments with `<fragment>`, `ListFragment` and the back stack, `AutoCompleteTextView`, the picker and progress dialogs, `NumberPicker`, SQLite with `SimpleCursorAdapter`, `java.io.File`, view and property animations, `ViewFlipper`, `getXml`, AppCompat with a `Toolbar` action bar, `ConstraintLayout` and `ConstraintSet`, AndroidX fragments with a shared `ViewModel` and `LiveData`, `RecyclerView` with `DiffUtil` and a grid, Material 3 components, and a Kotlin activity |
