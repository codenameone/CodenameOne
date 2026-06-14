package com.codename1.samples;

import com.codename1.components.SpanLabel;
import com.codename1.components.ToastBar;
import com.codename1.io.FileSystemStorage;
import com.codename1.io.Log;
import com.codename1.io.Util;
import com.codename1.printing.PrintResult;
import com.codename1.printing.PrintResultListener;
import com.codename1.printing.Printer;
import static com.codename1.ui.CN.*;
import com.codename1.ui.Button;
import com.codename1.ui.Dialog;
import com.codename1.ui.Font;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.Toolbar;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.Resources;

/**
 * Demonstrates the printing API: prints a generated image and a downloaded
 * PDF through the platform print dialog.
 */
public class PrinterSample {

    private static final String SAMPLE_PDF_URL = "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf";

    private Form current;
    private Resources theme;

    public void init(Object context) {
        theme = UIManager.initFirstTheme("/theme");
        Toolbar.setGlobalToolbar(true);
        Log.bindCrashProtection(true);
    }

    public void start() {
        if (current != null) {
            current.show();
            return;
        }
        Form hi = new Form("Printing", BoxLayout.y());
        hi.add(new SpanLabel("Printing supported: " + Printer.isPrintingSupported()));

        Button printImage = new Button("Print Image");
        printImage.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                Printer.printImage(createSampleImage(), new PrintResultListener() {
                    public void onResult(PrintResult result) {
                        showResult(result);
                    }
                });
            }
        });
        hi.add(printImage);

        Button printPdf = new Button("Print PDF");
        printPdf.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                final String path = FileSystemStorage.getInstance().getAppHomePath() + "sample.pdf";
                Util.downloadUrlToFileSystemInBackground(SAMPLE_PDF_URL, path, new ActionListener() {
                    public void actionPerformed(ActionEvent downloadEvent) {
                        if (!FileSystemStorage.getInstance().exists(path)) {
                            ToastBar.showErrorMessage("Failed to download the sample PDF");
                            return;
                        }
                        Printer.printPDF(path, new PrintResultListener() {
                            public void onResult(PrintResult result) {
                                showResult(result);
                            }
                        });
                    }
                });
            }
        });
        hi.add(printPdf);

        hi.show();
    }

    private void showResult(PrintResult result) {
        if (result.isFailed()) {
            ToastBar.showErrorMessage("Print failed: " + result.getError());
        } else {
            ToastBar.showMessage("Print result: " + result, FontImage.MATERIAL_PRINT);
        }
    }

    private Image createSampleImage() {
        int size = convertToPixels(60);
        Image img = Image.createImage(size, size, 0xffffffff);
        Graphics g = img.getGraphics();
        g.setAntiAliased(true);
        g.setColor(0x4986e7);
        g.fillArc(size / 8, size / 8, size * 3 / 4, size * 3 / 4, 0, 360);
        g.setColor(0xffffff);
        g.setFont(Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_LARGE));
        g.drawString("Codename One", size / 2 - g.getFont().stringWidth("Codename One") / 2,
                size / 2 - g.getFont().getHeight() / 2);
        return img;
    }

    public void stop() {
        current = getCurrentForm();
        if (current instanceof Dialog) {
            ((Dialog) current).dispose();
            current = getCurrentForm();
        }
    }

    public void destroy() {
    }
}
