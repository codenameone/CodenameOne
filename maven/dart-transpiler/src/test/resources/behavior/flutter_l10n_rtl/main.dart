// The framework strings an app gets from flutter_localizations, per locale.
//
// An app that lists GlobalMaterialLocalizations.delegate and runs in Hebrew or Spanish
// expects the dialogs, tooltips and pickers the framework draws to speak that language.
// This loads each delegate the way Localizations does and prints what it answers. The
// right-to-left layout half (a Row mirrored under Hebrew) needs a mounted tree and is
// held by RtlMirroringTest in flutter-runtime.
import 'package:flutter/material.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter_localizations/flutter_localizations.dart';

Future<void> main() async {
  for (final String lang in <String>['en', 'es', 'he']) {
    final MaterialLocalizations m =
        await GlobalMaterialLocalizations.delegate.load(Locale(lang));
    print('$lang ok=${m.okButtonLabel} cancel=${m.cancelButtonLabel}');
    print('$lang back=${m.backButtonTooltip} search=${m.searchFieldLabel}');
    print('$lang licenses=${m.licensesPageTitle} date=${m.datePickerHelpText}');
    print('$lang first=${m.firstDayOfWeekIndex} narrow=${m.narrowWeekdays.join()}');
    print('$lang monthYear=${m.formatMonthYear(DateTime(2026, 10, 29))}');
    final CupertinoLocalizations c =
        await GlobalCupertinoLocalizations.delegate.load(Locale(lang));
    print('$lang cupertino today=${c.todayLabel} paste=${c.pasteButtonLabel}');
  }
  final MaterialLocalizations fr =
      await GlobalMaterialLocalizations.delegate.load(const Locale('fr'));
  print('fallback ok=${fr.okButtonLabel}');
}
