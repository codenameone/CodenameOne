/*
    Document   : package
    Created on : June 11, 2026
    Author     : Shai Almog
*/

/// Cross platform printing: hand a PDF or image file to the platform
/// printing system, which shows the native print dialog where the user
/// picks a printer and confirms the job.
///
/// [Printer] is the entry point. The document is a file in
/// [com.codename1.io.FileSystemStorage] identified by path and mime
/// type; every printing platform accepts PDF (`application/pdf`) and
/// common image types (`image/png`, `image/jpeg`):
///
/// ```java
/// if (Printer.isPrintingSupported()) {
///     Printer.printPDF(reportPath, new PrintResultListener() {
///         public void onResult(PrintResult result) {
///             if (result.isFailed()) {
///                 ToastBar.showErrorMessage("Print failed: " + result.getError());
///             }
///         }
///     });
/// }
/// ```
///
/// [Printer#printImage] prints a [com.codename1.ui.Image] directly by
/// encoding it to a temporary PNG behind the scenes.
///
/// #### Results
///
/// The outcome arrives as a [PrintResult] on the EDT, exactly once per
/// request. `COMPLETED` means the job was handed to the platform
/// printing system - platforms generally can't observe the physical
/// printer, so it means "queued/sent", not "paper came out".
/// `CANCELLED` means the user dismissed the print dialog, where the
/// platform can observe that; platforms that can't (image printing on
/// Android, PDF hand-off on the desktop, browsers) report `COMPLETED`
/// on a best-effort basis. `FAILED` carries a short platform message
/// in [PrintResult#getError].
///
/// #### Permissions and the user gate
///
/// No platform permission, entitlement, or manifest entry is required.
/// Printing is always user-confirmed: the API can only pose the native
/// print dialog, and nothing reaches a printer until the user approves
/// the job there. Silent printing is deliberately not supported. Device
/// policy (managed Android profiles, MDM restrictions on iOS) can
/// disable printing entirely; that surfaces through
/// [Printer#isPrintingSupported] returning false or a `FAILED` result.
///
/// #### Platform mechanisms
///
/// iOS uses `UIPrintInteractionController` (AirPrint), Android the
/// `android.print` framework, desktop builds and the simulator use the
/// Java print services, Windows uses the Win32 print dialog with GDI
/// rendering, and the JavaScript port prints through the browser's
/// print dialog via a hidden frame.
package com.codename1.printing;
