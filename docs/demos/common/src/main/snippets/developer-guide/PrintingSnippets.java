// Generated from docs/developer-guide source blocks. Edit the guide snippets here, not inline.

// tag::printing-java-001[]
import com.codename1.printing.Printer;
import com.codename1.printing.PrintResult;

if (Printer.isPrintingSupported()) {
    Printer.printPDF(reportPath, result -> {
        if (result.isFailed()) {
            ToastBar.showErrorMessage("Print failed: " + result.getError());
        }
    });
}
// end::printing-java-001[]

// tag::printing-java-002[]
Image chart = renderChart();
Printer.printImage(chart, result -> {
    if (result.isFailed()) {
        ToastBar.showErrorMessage("Print failed: " + result.getError());
    }
});
// end::printing-java-002[]
