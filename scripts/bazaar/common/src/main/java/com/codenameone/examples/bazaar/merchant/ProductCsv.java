/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.bazaar.merchant;

import com.codenameone.examples.bazaar.ops.Amounts;

import com.codename1.io.CSVParser;
import com.codename1.io.CSVWriter;
import com.codenameone.examples.bazaar.api.OptionDto;
import com.codenameone.examples.bazaar.api.ProductDto;
import com.codenameone.examples.bazaar.api.ProductPhotoDto;
import com.codenameone.examples.bazaar.api.StockDto;
import com.codenameone.examples.bazaar.api.VariantDto;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/// Products as a spreadsheet: one row per variant, the product's own columns
/// filled on its first row, the layout every commerce platform imports.
///
/// The text goes through `com.codename1.io.CSVWriter` and `CSVParser`, the same
/// pair `com.codename1.spreadsheet.Workbook` reads and writes CSV with, so a
/// file exported here opens in any spreadsheet and comes back unchanged: the
/// writer escapes cells that would run as formulas, and SKUs and barcodes stay
/// text. Prices are written in major units ("12.50") and read back to cents.
/// Pure logic: the screens do the file picking.
public final class ProductCsv {
    /// The header row, in order.
    public static final String[] HEADER = {
        "Handle", "Title", "Description", "Vendor", "Type", "Tags", "Status",
        "Option1 Name", "Option1 Value", "Option2 Name", "Option2 Value",
        "Option3 Name", "Option3 Value",
        "SKU", "Barcode", "Price", "Compare At", "Cost", "Weight Grams"
    };

    private static final int HANDLE = 0;
    private static final int TITLE = 1;
    private static final int DESCRIPTION = 2;
    private static final int VENDOR = 3;
    private static final int TYPE = 4;
    private static final int TAGS = 5;
    private static final int STATUS = 6;
    private static final int OPTION1 = 7;
    private static final int SKU = 13;
    private static final int BARCODE = 14;
    private static final int PRICE = 15;
    private static final int COMPARE = 16;
    private static final int COST = 17;
    private static final int WEIGHT = 18;

    private ProductCsv() {
    }

    /// The rows for `products`, the header first.
    public static String[][] rows(List<ProductDto> products) {
        return rows(products, "USD");
    }

    /// [#rows(List)] with prices in the minor units of `currency`.
    public static String[][] rows(List<ProductDto> products, String currency) {
        List<String[]> out = new ArrayList<String[]>();
        out.add(HEADER);
        for (int p = 0; products != null && p < products.size(); p++) {
            ProductDto product = products.get(p);
            List<VariantDto> variants = product.variants() == null
                    ? new ArrayList<VariantDto>() : product.variants();
            int count = Math.max(1, variants.size());
            for (int v = 0; v < count; v++) {
                String[] row = new String[HEADER.length];
                for (int c = 0; c < row.length; c++) {
                    row[c] = "";
                }
                row[HANDLE] = text(product.handle());
                if (v == 0) {
                    row[TITLE] = text(product.title());
                    row[DESCRIPTION] = text(product.description());
                    row[VENDOR] = text(product.vendor());
                    row[TYPE] = text(product.productType());
                    row[TAGS] = VariantMatrix.join(product.tags());
                    row[STATUS] = text(product.status());
                }
                List<OptionDto> options = product.options();
                for (int o = 0; options != null && o < options.size() && o < 3; o++) {
                    if (v == 0) {
                        row[OPTION1 + o * 2] = text(options.get(o).name());
                    }
                }
                if (v < variants.size()) {
                    VariantDto variant = variants.get(v);
                    row[OPTION1 + 1] = text(variant.option1());
                    row[OPTION1 + 3] = text(variant.option2());
                    row[OPTION1 + 5] = text(variant.option3());
                    row[SKU] = text(variant.sku());
                    row[BARCODE] = text(variant.barcode());
                    row[PRICE] = major(variant.priceCents(), currency);
                    row[COMPARE] = variant.compareAtCents() > 0 ? major(variant.compareAtCents(), currency) : "";
                    row[COST] = variant.costCents() > 0 ? major(variant.costCents(), currency) : "";
                    row[WEIGHT] = variant.weightGrams() > 0 ? String.valueOf(variant.weightGrams()) : "";
                }
                out.add(row);
            }
        }
        return out.toArray(new String[out.size()][]);
    }

    /// The CSV text of `products`.
    public static String write(List<ProductDto> products) {
        return write(products, "USD");
    }

    /// [#write(List)] with prices in the minor units of `currency`.
    public static String write(List<ProductDto> products, String currency) {
        CSVWriter writer = new CSVWriter(',');
        writer.setEscapeFormulas(true);
        return writer.toString(rows(products, currency));
    }

    /// The products in CSV text.
    public static List<ProductDto> read(String text) throws IOException {
        return read(text, "USD");
    }

    /// [#read(String)] with prices in the minor units of `currency`.
    public static List<ProductDto> read(String text, String currency) throws IOException {
        return read(new ByteArrayInputStream(text.getBytes("UTF-8")), currency);
    }

    /// The products in a CSV stream; the separator is detected, so a file
    /// saved by a spreadsheet set to semicolons reads too.
    public static List<ProductDto> read(InputStream in) throws IOException {
        return read(in, "USD");
    }

    /// [#read(InputStream)] with prices in the minor units of `currency`.
    public static List<ProductDto> read(InputStream in, String currency) throws IOException {
        CSVParser parser = new CSVParser(',');
        parser.setDetectSeparator(true);
        return fromRows(parser.parse(in, "UTF-8"), currency);
    }

    /// The products in parsed rows. Columns are found by their header, ignoring
    /// ASCII case, so a file with its columns in another order still reads.
    /// Rows with the handle of the row above are more variants of the same
    /// product. Values the escaping writer guarded with a leading apostrophe
    /// are read without it.
    public static List<ProductDto> fromRows(String[][] rows) {
        return fromRows(rows, "USD");
    }

    /// [#fromRows(String[][])] with prices in the minor units of `currency`.
    public static List<ProductDto> fromRows(String[][] rows, String currency) {
        List<ProductDto> out = new ArrayList<ProductDto>();
        if (rows == null || rows.length == 0) {
            return out;
        }
        int[] at = new int[HEADER.length];
        for (int c = 0; c < HEADER.length; c++) {
            at[c] = -1;
            for (int h = 0; h < rows[0].length; h++) {
                if (HEADER[c].equalsIgnoreCase(unguard(rows[0][h]).trim())) {
                    at[c] = h;
                    break;
                }
            }
        }
        if (at[HANDLE] < 0 && at[TITLE] < 0) {
            throw new IllegalArgumentException("The file has no Handle or Title column");
        }
        Draft current = null;
        for (int r = 1; r < rows.length; r++) {
            String[] row = rows[r];
            if (blank(row)) {
                continue;
            }
            String handle = cell(row, at[HANDLE]);
            String title = cell(row, at[TITLE]);
            if (handle.length() == 0) {
                handle = Handles.of(title);
            }
            if (current == null || !current.handle.equalsIgnoreCase(handle)) {
                if (current != null) {
                    out.add(current.build());
                }
                current = new Draft(handle);
                current.title = title.length() == 0 ? handle : title;
                current.description = cell(row, at[DESCRIPTION]);
                current.vendor = cell(row, at[VENDOR]);
                current.type = cell(row, at[TYPE]);
                current.tags = VariantMatrix.split(cell(row, at[TAGS]));
                String status = cell(row, at[STATUS]);
                current.status = status.length() == 0 ? "DRAFT" : status;
                for (int o = 0; o < 3; o++) {
                    current.optionNames[o] = cell(row, at[OPTION1 + o * 2]);
                }
            }
            String o1 = cell(row, at[OPTION1 + 1]);
            String o2 = cell(row, at[OPTION1 + 3]);
            String o3 = cell(row, at[OPTION1 + 5]);
            current.optionValue(0, o1);
            current.optionValue(1, o2);
            current.optionValue(2, o3);
            String[] values = present(o1, o2, o3);
            current.variants.add(new VariantDto(null, null, VariantMatrix.title(values),
                    nullIfEmpty(o1), nullIfEmpty(o2), nullIfEmpty(o3),
                    cell(row, at[SKU]), cell(row, at[BARCODE]),
                    cents(cell(row, at[PRICE]), currency), cents(cell(row, at[COMPARE]), currency),
                    cents(cell(row, at[COST]), currency), integer(cell(row, at[WEIGHT])),
                    true, true, false, current.variants.size(), 0, new ArrayList<StockDto>(),
                    false, null, null, 0L, null, 0, null));
        }
        if (current != null) {
            out.add(current.build());
        }
        return out;
    }

    /// Hundredths as "12.50", a minus in front when negative: two decimals
    /// always, which is what basis points and percentages want. Money goes
    /// through [#major(long, String)].
    public static String major(long cents) {
        return Amounts.plain(cents, 2);
    }

    /// An amount in the minor units of `currency` as it is typed: "12.50"
    /// dollars, "1250" yen, "1.250" dinars.
    public static String major(long minor, String currency) {
        return Amounts.plain(minor, Amounts.digits(currency));
    }

    /// [#cents(String)] in the minor units of `currency`: "1250" yen is
    /// 1250, "1.25" dinars is 1250 fils.
    public static long cents(String text, String currency) {
        return cents(text, Amounts.digits(currency));
    }

    /// A whole number typed by hand; 0 for anything that is not one, so
    /// "12-5" is not -125 and a number past `int` is not a wrapped value. A
    /// minus sign counts only first, spaces and thousands commas are skipped,
    /// and any other character ("1.5", "12a") makes it not a number, so it is
    /// never read as the digits around it.
    public static int typedInteger(String text) {
        if (text == null) {
            return 0;
        }
        String t = text.trim();
        long out = 0;
        boolean negative = false;
        for (int iter = 0; iter < t.length(); iter++) {
            char c = t.charAt(iter);
            if (c == '-') {
                if (iter != 0) {
                    return 0;
                }
                negative = true;
            } else if (c >= '0' && c <= '9') {
                out = out * 10 + (c - '0');
                if (out > Integer.MAX_VALUE) {
                    return 0;
                }
            } else if (c != ' ' && c != ',') {
                return 0;
            }
        }
        return (int) (negative ? -out : out);
    }

    /// "12.5", "12,50", "$12" or "1,299.00" as cents; 0 for anything that is
    /// not a price, including a minus sign anywhere but first ("12-5") and a
    /// value too large to hold. A comma followed by exactly one or two digits at the end
    /// is a decimal comma; any other comma groups thousands.
    public static long cents(String text) {
        return cents(text, 2);
    }

    /// [#cents(String)] with `digits` decimals. A comma followed by 1 to
    /// `digits` digits at the end is a decimal comma, so for a currency
    /// without decimals every comma groups thousands.
    static long cents(String text, int digits) {
        if (text == null) {
            return 0;
        }
        String t = text.trim();
        int lastComma = t.lastIndexOf(',');
        boolean decimalComma = lastComma >= 0 && t.indexOf('.') < 0
                && t.length() - lastComma - 1 >= 1 && t.length() - lastComma - 1 <= digits;
        long whole = 0;
        long frac = 0;
        int fracDigits = -1;
        boolean negative = false;
        boolean any = false;
        for (int iter = 0; iter < t.length(); iter++) {
            char c = t.charAt(iter);
            if (c == '-') {
                if (iter != 0) {
                    return 0;
                }
                negative = true;
            } else if (c == '.' || (c == ',' && decimalComma && iter == lastComma)) {
                if (fracDigits >= 0) {
                    return 0;
                }
                fracDigits = 0;
            } else if (c >= '0' && c <= '9') {
                any = true;
                if (fracDigits < 0) {
                    whole = whole * 10 + (c - '0');
                    if (whole > 100000000000L) {
                        return 0;
                    }
                } else if (fracDigits < digits) {
                    frac = frac * 10 + (c - '0');
                    fracDigits++;
                }
            }
        }
        if (!any) {
            return 0;
        }
        for (int d = Math.max(0, fracDigits); d < digits; d++) {
            frac *= 10;
        }
        long value = whole * Amounts.unit(digits) + frac;
        return negative ? -value : value;
    }

    private static int integer(String text) {
        long c = 0;
        for (int iter = 0; text != null && iter < text.length(); iter++) {
            char ch = text.charAt(iter);
            if (ch >= '0' && ch <= '9') {
                c = c * 10 + (ch - '0');
                if (c > Integer.MAX_VALUE) {
                    return 0;
                }
            } else if (ch == '.') {
                break;
            }
        }
        return (int) c;
    }

    private static String[] present(String o1, String o2, String o3) {
        List<String> v = new ArrayList<String>();
        if (o1.length() > 0) {
            v.add(o1);
        }
        if (o2.length() > 0) {
            v.add(o2);
        }
        if (o3.length() > 0) {
            v.add(o3);
        }
        return v.toArray(new String[v.size()]);
    }

    private static boolean blank(String[] row) {
        for (int iter = 0; row != null && iter < row.length; iter++) {
            if (row[iter] != null && row[iter].trim().length() > 0) {
                return false;
            }
        }
        return true;
    }

    private static String cell(String[] row, int index) {
        if (index < 0 || index >= row.length || row[index] == null) {
            return "";
        }
        return unguard(row[index]).trim();
    }

    /// The escaping writer puts an apostrophe in front of a cell starting
    /// with = + - or @; take it off again.
    private static String unguard(String value) {
        if (value != null && value.length() > 1 && value.charAt(0) == '\'') {
            char c = value.charAt(1);
            if (c == '=' || c == '+' || c == '-' || c == '@') {
                return value.substring(1);
            }
        }
        return value == null ? "" : value;
    }

    private static String text(String s) {
        return s == null ? "" : s;
    }

    private static String nullIfEmpty(String s) {
        return s == null || s.length() == 0 ? null : s;
    }

    /// One product while its rows are read.
    private static final class Draft {
        final String handle;
        String title;
        String description;
        String vendor;
        String type;
        String status;
        List<String> tags;
        final String[] optionNames = new String[3];
        final List<List<String>> optionValues = new ArrayList<List<String>>();
        final List<VariantDto> variants = new ArrayList<VariantDto>();

        Draft(String handle) {
            this.handle = handle;
            for (int iter = 0; iter < 3; iter++) {
                optionValues.add(new ArrayList<String>());
            }
        }

        void optionValue(int index, String value) {
            if (value.length() == 0) {
                return;
            }
            List<String> values = optionValues.get(index);
            for (int iter = 0; iter < values.size(); iter++) {
                if (values.get(iter).equalsIgnoreCase(value)) {
                    return;
                }
            }
            values.add(value);
        }

        ProductDto build() {
            List<OptionDto> options = new ArrayList<OptionDto>();
            for (int o = 0; o < 3; o++) {
                if (optionValues.get(o).isEmpty()) {
                    continue;
                }
                String name = optionNames[o] == null || optionNames[o].length() == 0
                        ? "Option " + (o + 1) : optionNames[o];
                options.add(new OptionDto(null, name, optionValues.get(o), o));
            }
            return new ProductDto(null, null, handle, title, description, vendor, type, tags,
                    status, "", "", null, 0, 0, options, variants,
                    new ArrayList<ProductPhotoDto>(), new ArrayList<String>(), -1);
        }
    }
}
