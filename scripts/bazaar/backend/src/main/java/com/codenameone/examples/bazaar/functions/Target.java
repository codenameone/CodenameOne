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
package com.codenameone.examples.bazaar.functions;

/// What a checkout function is for: the typed record it reads and the
/// operations it may emit. Every target reads the same cart scalars and the
/// `lines` list; shipping adds `rates` and payment adds `methods`.
///
/// | Target | Second list | Emits |
/// |---|---|---|
/// | `discount` | none | `discount(amount, message)`, `line_discount(index, amount, message)` |
/// | `shipping` | `rates` (index, id, name, price, kind) | `hide(id)`, `rename(id, name)`, `reprice(id, price)`, `move(id, position)` |
/// | `payment` | `methods` (index, type) | `hide(type)`, `move(type, position)` |
/// | `validation` | none | `error(message)` |
///
/// Amounts are long minor units of the store's currency. A type is `L`
/// (long), `S` (string) or `B` (boolean).
public final class Target {
    public static final char LONG = 'L';
    public static final char STRING = 'S';
    public static final char BOOL = 'B';

    /// Scalar slots, the same in every target (`cart.<name>`).
    public static final int SUBTOTAL = 0;
    public static final int DISCOUNT_SO_FAR = 1;
    public static final int QUANTITY = 2;
    public static final int CURRENCY = 3;
    public static final int COUNTRY = 4;
    public static final int CUSTOMER = 5;
    public static final int CODE = 6;
    static final String[] SCALARS = {"subtotal", "discount", "quantity", "currency", "country",
        "customer", "code"};
    static final String SCALAR_TYPES = "LLLSSSS";

    /// List 0 is always `lines`.
    public static final int LINES = 0;
    public static final int LINE_INDEX = 0;
    public static final int LINE_QUANTITY = 1;
    public static final int LINE_PRICE = 2;
    public static final int LINE_TOTAL = 3;
    public static final int LINE_SKU = 4;
    public static final int LINE_PRODUCT = 5;
    public static final int LINE_VARIANT = 6;
    public static final int LINE_TITLE = 7;
    private static final String[] LINE_FIELDS = {"index", "quantity", "price", "total", "sku",
        "product", "variant", "title"};
    private static final String LINE_TYPES = "LLLLSSSS";

    public static final int RATE_INDEX = 0;
    public static final int RATE_ID = 1;
    public static final int RATE_NAME = 2;
    public static final int RATE_PRICE = 3;
    public static final int RATE_KIND = 4;
    public static final int METHOD_INDEX = 0;
    public static final int METHOD_TYPE = 1;

    public static final Target DISCOUNT = new Target("discount", new String[] {"lines"},
            new String[][] {LINE_FIELDS}, new String[] {LINE_TYPES},
            new String[] {"discount", "line_discount"}, new String[] {"LS", "LLS"});
    public static final Target SHIPPING = new Target("shipping", new String[] {"lines", "rates"},
            new String[][] {LINE_FIELDS, {"index", "id", "name", "price", "kind"}},
            new String[] {LINE_TYPES, "LSSLS"},
            new String[] {"hide", "rename", "reprice", "move"},
            new String[] {"S", "SS", "SL", "SL"});
    public static final Target PAYMENT = new Target("payment", new String[] {"lines", "methods"},
            new String[][] {LINE_FIELDS, {"index", "type"}}, new String[] {LINE_TYPES, "LS"},
            new String[] {"hide", "move"}, new String[] {"S", "SL"});
    public static final Target VALIDATION = new Target("validation", new String[] {"lines"},
            new String[][] {LINE_FIELDS}, new String[] {LINE_TYPES},
            new String[] {"error"}, new String[] {"S"});

    private static final Target[] ALL = {DISCOUNT, SHIPPING, PAYMENT, VALIDATION};

    /// Emit indexes, by target.
    public static final int EMIT_DISCOUNT = 0;
    public static final int EMIT_LINE_DISCOUNT = 1;
    public static final int EMIT_HIDE = 0;
    public static final int EMIT_RENAME = 1;
    public static final int EMIT_REPRICE = 2;
    public static final int EMIT_SHIPPING_MOVE = 3;
    public static final int EMIT_PAYMENT_MOVE = 1;
    public static final int EMIT_ERROR = 0;

    public final String name;
    final String[] lists;
    final String[][] fields;
    final String[] fieldTypes;
    final String[] emits;
    final String[] signatures;

    private Target(String name, String[] lists, String[][] fields, String[] fieldTypes,
            String[] emits, String[] signatures) {
        this.name = name;
        this.lists = lists;
        this.fields = fields;
        this.fieldTypes = fieldTypes;
        this.emits = emits;
        this.signatures = signatures;
    }

    /// The target called `name`, or null.
    public static Target named(String name) {
        for (int iter = 0; iter < ALL.length; iter++) {
            if (ALL[iter].name.equals(name)) {
                return ALL[iter];
            }
        }
        return null;
    }

    /// The names of every target, comma separated, for an error message.
    public static String names() {
        StringBuilder b = new StringBuilder();
        for (int iter = 0; iter < ALL.length; iter++) {
            b.append(iter == 0 ? "" : ", ").append(ALL[iter].name);
        }
        return b.toString();
    }

    static int indexOf(String[] names, String name) {
        for (int iter = 0; iter < names.length; iter++) {
            if (names[iter].equals(name)) {
                return iter;
            }
        }
        return -1;
    }

    int listCount() {
        return lists.length;
    }

    int fieldCount(int list) {
        return fields[list].length;
    }

    char fieldType(int list, int field) {
        return fieldTypes[list].charAt(field);
    }

    static char scalarType(int slot) {
        return SCALAR_TYPES.charAt(slot);
    }

    /// The emit called `name` in this target, or -1.
    public int emit(String name) {
        return indexOf(emits, name);
    }

    /// The argument types of emit `index`, one letter each.
    public String signature(int index) {
        return signatures[index];
    }

    /// An empty record for this target with `sizes[list]` rows in each list.
    public FunctionInput input(int... sizes) {
        return new FunctionInput(this, sizes);
    }
}
