// Pass-3 "remaining" category: signature stubs for the few brand-new runtime
// types reached by the leftover static/named-member diagnostics. The static
// members and named constructors themselves were appended to their OWNING
// classes in flutter_material.dart (Navigator.of, SizedBox.shrink, Size.fromRadius,
// Color.fromRGBO, the button .icon factories, Scaffold.of, ...). Only the two
// result types that did not exist anywhere are declared here.

// The mutable Scaffold state reached via Scaffold.of(context) — the surface the
// gallery's bottom-sheet demo touches. showBottomSheet returns a controller
// whose `closed` future completes when the sheet is dismissed.
@JavaName('com.codename1.flutter.material.ScaffoldState')
abstract class ScaffoldState {
  external PersistentBottomSheetController showBottomSheet(WidgetBuilder builder,
      {double? elevation, Color? backgroundColor, Object? shape, Clip? clipBehavior,
       Object? constraints, bool? enableDrag});
  external void showSnackBar(SnackBar snackBar);
  external void openDrawer();
  external void openEndDrawer();
}

// The handle returned by ScaffoldState.showBottomSheet: `closed` is a future
// that resolves with the sheet's result once it is dismissed.
@JavaName('com.codename1.flutter.material.PersistentBottomSheetController')
abstract class PersistentBottomSheetController {
  external Future<dynamic> get closed;
  external void close();
}
