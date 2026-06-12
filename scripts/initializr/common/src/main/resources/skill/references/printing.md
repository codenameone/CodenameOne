# Printing Reference

`com.codename1.printing` is the cross-platform printing API. You hand it a **file** (PDF or image) or a CN1 `Image`; it shows the platform's native print dialog (where the user picks a printer and confirms) and reports the outcome through a listener. There is no page-layout / canvas drawing model — you render to a PDF or image first, then print that artifact.

Supported on **iOS** (AirPrint), **Android** (4.4 / API 19+), **desktop/JavaSE**, **native Windows**, and **JavaScript/web**. Everything funnels through one entry point: `Printer`.

## The whole API in one place

```java
import com.codename1.printing.Printer;
import com.codename1.printing.PrintResult;
import com.codename1.printing.PrintResultListener;
```

| Call | Purpose |
| --- | --- |
| `Printer.isPrintingSupported()` → `boolean` | Gate the print UI; `false` on platforms/devices with no print path. |
| `Printer.print(String filePath, String mimeType, PrintResultListener l)` | Print a file in `FileSystemStorage` with an explicit MIME type. |
| `Printer.printPDF(String filePath, PrintResultListener l)` | Convenience for `application/pdf`. |
| `Printer.printImage(Image image, PrintResultListener l)` | Print a CN1 `Image` directly (encoded to a temp PNG, deleted after). |

Accepted MIME types on every platform: `application/pdf`, `image/png`, `image/jpeg`.

`PrintResultListener` is a single-method callback, invoked **once, on the EDT**:

```java
public interface PrintResultListener { void onResult(PrintResult result); }
```

`PrintResult` is immutable. Query it:

| Method | Meaning |
| --- | --- |
| `isCompleted()` | Job was handed to the platform print system (queued/sent — **not** a guarantee it physically printed). |
| `isCancelled()` | User dismissed the print dialog (only where the platform can observe it — see caveats). |
| `isFailed()` | Job could not start. |
| `getError()` | Platform message when `isFailed()`, else `null`. |
| `getStatus()` | Raw `int` — `STATUS_COMPLETED` / `STATUS_CANCELLED` / `STATUS_FAILED`. |

(The `PrintResult.completed()` / `cancelled()` / `failed(msg)` factories exist for port implementors; app code only ever *reads* a `PrintResult`.)

## Printing an Image you rendered

```java
if (Printer.isPrintingSupported()) {
    Image img = Image.createImage(480, 480, 0xffffffff);
    Graphics g = img.getGraphics();
    g.setColor(0x4986e7);
    g.fillArc(60, 60, 360, 360, 0, 360);

    Printer.printImage(img, result -> {
        if (result.isFailed()) {
            ToastBar.showErrorMessage("Print failed: " + result.getError());
        } else {
            ToastBar.showMessage("Sent to printer", FontImage.MATERIAL_PRINT);
        }
    });
}
```

To print the current screen, snapshot it into an `Image` first
(`Image screenshot = Image.createImage(form.getWidth(), form.getHeight()); form.paintComponent(screenshot.getGraphics());`)
and pass that to `printImage`.

## Printing a PDF (e.g. a generated report or a download)

`print*` takes a path in `FileSystemStorage`, so write/download the PDF there first:

```java
String pdfPath = FileSystemStorage.getInstance().getAppHomePath() + "report.pdf";
Util.downloadUrlToFileSystemInBackground(reportUrl, pdfPath, e -> {
    if (FileSystemStorage.getInstance().exists(pdfPath)) {
        Printer.printPDF(pdfPath, result -> {
            if (result.isFailed()) {
                ToastBar.showErrorMessage("Print failed: " + result.getError());
            }
        });
    }
});
```

CN1 has no built-in PDF *generator*. Produce the PDF with a cn1lib or a server endpoint, land it in `FileSystemStorage`, then `printPDF` it. For arbitrary file types use the generic `print(path, mimeType, listener)` overload.

## Platform behaviour and caveats

| Platform | Backend | Notes |
| --- | --- | --- |
| iOS | `UIPrintInteractionController` (AirPrint) | Full dialog + cancel detection. |
| Android | `android.print` / `PrintHelper` | API 19+. `isPrintingSupported()` is `false` below that or with no Activity. Image prints report `COMPLETED` best-effort (can't see a dismissed dialog). |
| JavaSE / desktop | `PrinterJob` (images) + `Desktop.print` (PDF) | `false` when headless. |
| Native Windows | Win32 `PrintDlg` + GDI + `Windows.Data.Pdf` | Missing printer / headless surfaces as `FAILED`. |
| JavaScript / web | Blob URL in a hidden iframe + browser print dialog | Always "supported". Cancellation **cannot** be detected (reported as `COMPLETED`); some browsers (Firefox) may render a PDF blank. |

**Cancellation is best-effort.** Treat `isCompleted()` as "handed off", not "printed". Only iOS and the Windows/Android dialogs reliably report a user cancel; elsewhere a dismissed dialog still comes back `COMPLETED`. Don't build logic that depends on distinguishing cancel from success on web/desktop.

**No build hints, no permissions.** Printing needs no `codename1.arg.*` key, entitlement, or manifest entry — user confirmation is implicit in the native dialog. Just call `Printer`.

## What NOT to do

- Don't call `print*` off a path that isn't in `FileSystemStorage` — `Storage` keys and classpath resources won't resolve. Copy to a `FileSystemStorage` path first (`getAppHomePath() + name`).
- Don't assume `isCompleted()` means a page came out — it means the OS accepted the job.
- Don't try to lay out print content with CN1 layouts expecting pagination; render to a PDF/image and print that.
- Don't poll for a result — the listener fires exactly once on the EDT.
