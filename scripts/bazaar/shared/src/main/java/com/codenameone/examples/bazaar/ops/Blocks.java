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
package com.codenameone.examples.bazaar.ops;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// The block trees of app UI extensions, read the same way by the server
/// (which validates an upload) and by the app (which renders one natively).
///
/// A tree is JSON over a small fixed vocabulary; nothing in it runs:
///
/// | Node | Properties |
/// |---|---|
/// | `text` | `text`, `tone` |
/// | `heading` | `text` |
/// | `badge` | `text`, `tone` |
/// | `button` | `label` and exactly one of `url` (a link) or `action` (posted to the app) |
/// | `image` | `src`, `alt` |
/// | `list` | `items`, an array of strings |
/// | `field` | `key`, `label`, `kind` (`text`, `number`, `checkbox`, `select`), `options`, `required` |
/// | `divider` | nothing |
/// | `stack` | `direction` (`vertical`, `horizontal`), `children` |
///
/// Validation is strict: an unknown node type or property, a tree deeper
/// than [#MAX_DEPTH] or larger than [#MAX_NODES], or a URL that is not a
/// plain https address is refused, never dropped. Text may carry
/// `{{key}}` placeholders the app's data fills in ([#bind]); URLs may not.
public final class Blocks {
    public static final String TEXT = "text";
    public static final String HEADING = "heading";
    public static final String BADGE = "badge";
    public static final String BUTTON = "button";
    public static final String IMAGE = "image";
    public static final String LIST = "list";
    public static final String FIELD = "field";
    public static final String DIVIDER = "divider";
    public static final String STACK = "stack";

    public static final String KIND_TEXT = "text";
    public static final String KIND_NUMBER = "number";
    public static final String KIND_CHECKBOX = "checkbox";
    public static final String KIND_SELECT = "select";

    public static final String PRODUCT_BLOCK = "admin.product.block";
    public static final String ORDER_BLOCK = "admin.order.block";
    public static final String CUSTOMER_BLOCK = "admin.customer.block";
    public static final String PRODUCT_ACTION = "admin.product.action";
    public static final String ORDER_ACTION = "admin.order.action";
    public static final String CUSTOMER_ACTION = "admin.customer.action";
    public static final String STOREFRONT_BLOCK = "storefront.block";
    public static final String CHECKOUT_BLOCK = "checkout.block";

    /// Every target an extension may name.
    public static final String[] TARGETS = {PRODUCT_BLOCK, ORDER_BLOCK, CUSTOMER_BLOCK,
        PRODUCT_ACTION, ORDER_ACTION, CUSTOMER_ACTION, STOREFRONT_BLOCK, CHECKOUT_BLOCK};

    /// The storefront places a `storefront.block` may go.
    public static final String[] PLACES = {"product-below-price", "cart-below-total",
        "checkout-thank-you"};

    public static final int MAX_DEPTH = 6;
    public static final int MAX_NODES = 80;
    public static final int MAX_CHILDREN = 30;
    public static final int MAX_ITEMS = 30;
    public static final int MAX_OPTIONS = 20;
    public static final int MAX_TEXT = 500;
    public static final int MAX_URL = 500;
    public static final int MAX_KEY = 40;
    public static final int MAX_JSON = 16000;
    private static final int MAX_JSON_NESTING = 24;

    private static final String[] TONES = {"", "subdued", "success", "warning", "critical"};
    private static final String[] KINDS = {KIND_TEXT, KIND_NUMBER, KIND_CHECKBOX, KIND_SELECT};
    private static final String[] DIRECTIONS = {"", "vertical", "horizontal"};

    private Blocks() {
    }

    /// Why a tree, a target or a value was refused.
    public static final class Invalid extends Exception {
        public Invalid(String message) {
            super(message);
        }
    }

    /// One node of a checked tree. `text` is a text's, heading's or badge's
    /// text, a button's or field's label and an image's alt text; `items`
    /// are a list's items or a select field's options.
    public static final class Node {
        public final String type;
        public String text = "";
        public String tone = "";
        public String url = "";
        public String action = "";
        public String src = "";
        public String key = "";
        public String kind = "";
        public boolean required;
        public boolean horizontal;
        public final List<String> items = new ArrayList<String>();
        public final List<Node> children = new ArrayList<Node>();

        Node(String type) {
            this.type = type;
        }
    }

    /// One line of a tree flattened for an HTML page ([#flatten]).
    public static final class Row {
        public final String kind;
        public final String text;
        public final String url;
        public final String tone;
        public final int depth;

        Row(String kind, String text, String url, String tone, int depth) {
            this.kind = kind;
            this.text = text;
            this.url = url;
            this.tone = tone;
            this.depth = depth;
        }
    }

    // ------------------------------------------------------------- targets

    /// True when `target` is one of [#TARGETS].
    public static boolean isTarget(String target) {
        return indexOf(TARGETS, target) >= 0;
    }

    /// True when `place` is one of [#PLACES].
    public static boolean isPlace(String place) {
        return indexOf(PLACES, place) >= 0;
    }

    /// True for the `admin.*.action` targets.
    public static boolean isAction(String target) {
        return target != null && target.startsWith("admin.") && target.endsWith(".action");
    }

    /// The admin page a target belongs to (`product`, `order`, `customer`),
    /// or "" for one that is not an admin target.
    public static String area(String target) {
        if (target == null || !target.startsWith("admin.")) {
            return "";
        }
        int dot = target.indexOf('.', 6);
        return dot < 0 ? "" : target.substring(6, dot);
    }

    private static int indexOf(String[] all, String s) {
        for (int iter = 0; s != null && iter < all.length; iter++) {
            if (all[iter].equals(s)) {
                return iter;
            }
        }
        return -1;
    }

    /// Checks what `target` allows of `root`: an action target is a single
    /// button with an action; a storefront block has no fields and no
    /// actions (its page is shared and cached); a checkout block no actions.
    public static void checkTarget(String target, Node root) throws Invalid {
        if (!isTarget(target)) {
            throw new Invalid("The target is not one Bazaar knows");
        }
        if (isAction(target)) {
            if (!BUTTON.equals(root.type) || root.action.length() == 0) {
                throw new Invalid("An action extension is one button with an action");
            }
            return;
        }
        if (STOREFRONT_BLOCK.equals(target) && !fields(root).isEmpty()) {
            throw new Invalid("A storefront block cannot ask for input");
        }
        if ((STOREFRONT_BLOCK.equals(target) || CHECKOUT_BLOCK.equals(target))
                && !actions(root).isEmpty()) {
            throw new Invalid("Only an admin block may have action buttons");
        }
    }

    // --------------------------------------------------------------- parse

    /// Reads and checks a tree written as JSON.
    public static Node parse(String json) throws Invalid {
        if (json == null || json.length() == 0) {
            throw new Invalid("The block tree is empty");
        }
        if (json.length() > MAX_JSON) {
            throw new Invalid("The block tree is longer than " + MAX_JSON + " characters");
        }
        Reader r = new Reader(json);
        Object value = r.value(0);
        r.space();
        if (r.at != json.length()) {
            throw new Invalid("The block tree has text after its end");
        }
        int[] count = new int[1];
        Node root = node(value, 1, count);
        List<Node> fs = fields(root);
        for (int iter = 0; iter < fs.size(); iter++) {
            for (int other = iter + 1; other < fs.size(); other++) {
                if (fs.get(iter).key.equals(fs.get(other).key)) {
                    throw new Invalid("Two fields use the key " + fs.get(iter).key);
                }
            }
        }
        return root;
    }

    private static Node node(Object value, int depth, int[] count) throws Invalid {
        if (depth > MAX_DEPTH) {
            throw new Invalid("The block tree is deeper than " + MAX_DEPTH + " levels");
        }
        count[0]++;
        if (count[0] > MAX_NODES) {
            throw new Invalid("The block tree has more than " + MAX_NODES + " nodes");
        }
        if (!(value instanceof Map)) {
            throw new Invalid("A node is a JSON object");
        }
        Map m = (Map) value;
        Object t = m.get("type");
        if (!(t instanceof String)) {
            throw new Invalid("A node names its type");
        }
        String type = (String) t;
        Node n = new Node(type);
        if (TEXT.equals(type) || BADGE.equals(type)) {
            allow(m, type, "text", "tone", null);
            n.text = text(m, "text", true);
            n.tone = word(m, "tone", TONES);
        } else if (HEADING.equals(type)) {
            allow(m, type, "text", null, null);
            n.text = text(m, "text", true);
        } else if (BUTTON.equals(type)) {
            button(m, n);
        } else if (IMAGE.equals(type)) {
            allow(m, type, "src", "alt", null);
            n.src = url(m, "src");
            n.text = text(m, "alt", false);
        } else if (LIST.equals(type)) {
            allow(m, type, "items", null, null);
            List l = list(m.get("items"), MAX_ITEMS, "A list has 1 to " + MAX_ITEMS + " items");
            for (int iter = 0; iter < l.size(); iter++) {
                n.items.add(checkText(l.get(iter), "A list item"));
            }
        } else if (FIELD.equals(type)) {
            field(m, n);
        } else if (DIVIDER.equals(type)) {
            allow(m, type, null, null, null);
        } else if (STACK.equals(type)) {
            allow(m, type, "direction", "children", null);
            n.horizontal = "horizontal".equals(word(m, "direction", DIRECTIONS));
            List l = list(m.get("children"), MAX_CHILDREN, "A stack has 1 to " + MAX_CHILDREN
                    + " children");
            for (int iter = 0; iter < l.size(); iter++) {
                n.children.add(node(l.get(iter), depth + 1, count));
            }
        } else {
            throw new Invalid("Unknown node type " + cut(type));
        }
        return n;
    }

    private static void button(Map m, Node n) throws Invalid {
        allow(m, BUTTON, "label", "url", "action");
        n.text = text(m, "label", true);
        boolean link = m.containsKey("url");
        if (link == m.containsKey("action")) {
            throw new Invalid("A button has either a url or an action");
        }
        if (link) {
            n.url = url(m, "url");
        } else {
            n.action = key(m, "action");
        }
    }

    private static void field(Map m, Node n) throws Invalid {
        for (Object k : m.keySet()) {
            String key = String.valueOf(k);
            if (!"type".equals(key) && !"key".equals(key) && !"label".equals(key)
                    && !"kind".equals(key) && !"options".equals(key)
                    && !"required".equals(key)) {
                throw new Invalid("A field has no property " + cut(key));
            }
        }
        n.key = key(m, "key");
        n.text = text(m, "label", true);
        n.kind = word(m, "kind", KINDS);
        if (n.kind.length() == 0) {
            throw new Invalid("A field names its kind");
        }
        Object req = m.get("required");
        if (req != null && !(req instanceof Boolean)) {
            throw new Invalid("A field's required is true or false");
        }
        n.required = Boolean.TRUE.equals(req);
        Object options = m.get("options");
        if (KIND_SELECT.equals(n.kind)) {
            List l = list(options, MAX_OPTIONS, "A select field has 1 to " + MAX_OPTIONS
                    + " options");
            for (int iter = 0; iter < l.size(); iter++) {
                String o = checkText(l.get(iter), "An option");
                if (o.length() > 80 || o.indexOf(',') >= 0 || o.indexOf("{{") >= 0) {
                    throw new Invalid("An option is at most 80 characters, with no comma or "
                            + "placeholder");
                }
                n.items.add(o);
            }
        } else if (options != null) {
            throw new Invalid("Only a select field has options");
        }
    }

    private static List list(Object v, int max, String message) throws Invalid {
        if (!(v instanceof List) || ((List) v).isEmpty() || ((List) v).size() > max) {
            throw new Invalid(message);
        }
        return (List) v;
    }

    private static String cut(String s) {
        return s.length() > 40 ? s.substring(0, 40) : s;
    }

    private static void allow(Map m, String type, String a, String b, String c) throws Invalid {
        for (Object k : m.keySet()) {
            String key = String.valueOf(k);
            if (!"type".equals(key) && !key.equals(a) && !key.equals(b) && !key.equals(c)) {
                throw new Invalid("A " + type + " has no property " + cut(key));
            }
        }
    }

    private static String text(Map m, String name, boolean required) throws Invalid {
        Object v = m.get(name);
        if (v == null && !required) {
            return "";
        }
        return checkText(v, "The " + name);
    }

    private static String checkText(Object v, String what) throws Invalid {
        if (!(v instanceof String)) {
            throw new Invalid(what + " is a string");
        }
        String s = (String) v;
        if (s.length() == 0 || s.length() > MAX_TEXT) {
            throw new Invalid(what + " is 1 to " + MAX_TEXT + " characters");
        }
        for (int iter = 0; iter < s.length(); iter++) {
            char c = s.charAt(iter);
            if (c < ' ' && c != '\n') {
                throw new Invalid(what + " has a control character");
            }
        }
        placeholders(s);
        return s;
    }

    private static String word(Map m, String name, String[] allowed) throws Invalid {
        Object v = m.get(name);
        if (v == null) {
            return "";
        }
        if (!(v instanceof String) || indexOf(allowed, (String) v) < 0) {
            throw new Invalid("The " + name + " is not one Bazaar knows");
        }
        return (String) v;
    }

    private static String key(Map m, String name) throws Invalid {
        Object v = m.get(name);
        if (!(v instanceof String) || !isKey((String) v)) {
            throw new Invalid("The " + name + " is 1 to " + MAX_KEY
                    + " lower case letters, digits, '-' or '_'");
        }
        return (String) v;
    }

    /// True for a field key, an action or a proxy path: 1 to [#MAX_KEY] of
    /// `a-z`, `0-9`, `-` and `_`.
    public static boolean isKey(String s) {
        if (s == null || s.length() == 0 || s.length() > MAX_KEY) {
            return false;
        }
        for (int iter = 0; iter < s.length(); iter++) {
            char c = s.charAt(iter);
            if (!(c >= 'a' && c <= 'z') && !(c >= '0' && c <= '9') && c != '-' && c != '_') {
                return false;
            }
        }
        return true;
    }

    private static String url(Map m, String name) throws Invalid {
        Object v = m.get(name);
        if (!(v instanceof String) || !isHttps((String) v)) {
            throw new Invalid("The " + name + " is a plain https address");
        }
        return (String) v;
    }

    /// True for a plain `https://` address of at most [#MAX_URL] characters:
    /// a host, no user info, and no whitespace, quotes, angle brackets,
    /// backslashes, braces or control characters.
    public static boolean isHttps(String url) {
        if (url == null || url.length() > MAX_URL || !url.startsWith("https://")) {
            return false;
        }
        int hostEnd = url.length();
        for (int iter = 8; iter < url.length(); iter++) {
            char c = url.charAt(iter);
            if (c <= ' ' || c >= 127 || c == '"' || c == '\'' || c == '<' || c == '>'
                    || c == '\\' || c == '`' || c == '{' || c == '}' || c == '|' || c == '^') {
                return false;
            }
            if (hostEnd == url.length() && (c == '/' || c == '?' || c == '#')) {
                hostEnd = iter;
            }
        }
        String host = url.substring(8, hostEnd);
        return host.length() > 0 && host.indexOf('@') < 0 && host.charAt(0) != '.'
                && host.charAt(0) != ':';
    }

    /// Checks the `{{key}}` placeholders of `s`; answers their keys.
    public static List<String> placeholders(String s) throws Invalid {
        List<String> out = new ArrayList<String>();
        int at = s.indexOf("{{");
        while (at >= 0) {
            int end = s.indexOf("}}", at + 2);
            if (end < 0) {
                throw new Invalid("A placeholder is not closed");
            }
            String key = s.substring(at + 2, end).trim();
            if (!isDataKey(key)) {
                throw new Invalid("A placeholder names a key of letters, digits, '.', '-' or "
                        + "'_'");
            }
            out.add(key);
            at = s.indexOf("{{", end + 2);
        }
        return out;
    }

    private static boolean isDataKey(String s) {
        if (s.length() == 0 || s.length() > 60) {
            return false;
        }
        for (int iter = 0; iter < s.length(); iter++) {
            char c = s.charAt(iter);
            if (!(c >= 'a' && c <= 'z') && !(c >= 'A' && c <= 'Z') && !(c >= '0' && c <= '9')
                    && c != '.' && c != '-' && c != '_') {
                return false;
            }
        }
        return true;
    }

    // ----------------------------------------------------------- reading

    /// `text` with each `{{key}}` replaced by its value in `values`, or by
    /// nothing when there is none. The result is text, never markup.
    public static String bind(String text, Map<String, String> values) {
        int at = text.indexOf("{{");
        if (at < 0) {
            return text;
        }
        StringBuilder b = new StringBuilder();
        int from = 0;
        while (at >= 0) {
            int end = text.indexOf("}}", at + 2);
            if (end < 0) {
                break;
            }
            b.append(text.substring(from, at));
            String v = values == null ? null : values.get(text.substring(at + 2, end).trim());
            if (v != null) {
                b.append(v);
            }
            from = end + 2;
            at = text.indexOf("{{", from);
        }
        b.append(text.substring(from));
        return b.toString();
    }

    /// The fields of `root`, in reading order.
    public static List<Node> fields(Node root) {
        List<Node> out = new ArrayList<Node>();
        collect(root, FIELD, out);
        return out;
    }

    /// The action buttons of `root`, in reading order.
    public static List<Node> actions(Node root) {
        List<Node> all = new ArrayList<Node>();
        collect(root, BUTTON, all);
        List<Node> out = new ArrayList<Node>();
        for (int iter = 0; iter < all.size(); iter++) {
            if (all.get(iter).action.length() > 0) {
                out.add(all.get(iter));
            }
        }
        return out;
    }

    /// True when `root` has a button whose action is `action`.
    public static boolean hasAction(Node root, String action) {
        List<Node> all = actions(root);
        for (int iter = 0; iter < all.size(); iter++) {
            if (all.get(iter).action.equals(action)) {
                return true;
            }
        }
        return false;
    }

    private static void collect(Node n, String type, List<Node> out) {
        if (type.equals(n.type)) {
            out.add(n);
        }
        for (int iter = 0; iter < n.children.size(); iter++) {
            collect(n.children.get(iter), type, out);
        }
    }

    /// `root` as lines for a server-rendered page, placeholders bound:
    /// `heading`, `text`, `badge`, `link` (a url button), `image` (`url` is
    /// the source, `text` the alt), `item` (one per list item), `divider`
    /// and `field` (its label). A stack is its children one level deeper;
    /// an action button has no line, since a page cannot post one.
    public static List<Row> flatten(Node root, Map<String, String> values) {
        List<Row> out = new ArrayList<Row>();
        flatten(root, values, 0, out);
        return out;
    }

    private static void flatten(Node n, Map<String, String> values, int depth, List<Row> out) {
        String t = n.type;
        if (STACK.equals(t)) {
            for (int iter = 0; iter < n.children.size(); iter++) {
                flatten(n.children.get(iter), values, depth + 1, out);
            }
        } else if (LIST.equals(t)) {
            for (int iter = 0; iter < n.items.size(); iter++) {
                out.add(new Row("item", bind(n.items.get(iter), values), "", "", depth));
            }
        } else if (BUTTON.equals(t)) {
            if (n.url.length() > 0) {
                out.add(new Row("link", bind(n.text, values), n.url, "", depth));
            }
        } else if (IMAGE.equals(t)) {
            out.add(new Row("image", bind(n.text, values), n.src, "", depth));
        } else {
            out.add(new Row(t, bind(n.text, values), "", n.tone, depth));
        }
    }

    // ------------------------------------------------------------ values

    /// Checks an answer to `field`: a number is a decimal number, a checkbox
    /// `yes` or "", a select one of its options, text at most [#MAX_TEXT]
    /// characters. Answers the value to keep, "" for no answer.
    public static String answer(Node field, String given) throws Invalid {
        String v = given == null ? "" : given.trim();
        if (KIND_CHECKBOX.equals(field.kind)) {
            v = v.length() == 0 || "no".equals(v) || "false".equals(v) ? "" : "yes";
        } else if (KIND_NUMBER.equals(field.kind)) {
            if (v.length() > 0 && !isNumber(v)) {
                throw new Invalid(field.text + " is a number");
            }
        } else if (KIND_SELECT.equals(field.kind)) {
            if (v.length() > 0 && !field.items.contains(v)) {
                throw new Invalid("Choose one of the choices for " + field.text);
            }
        } else if (v.length() > MAX_TEXT) {
            throw new Invalid(field.text + " is at most " + MAX_TEXT + " characters");
        }
        if (v.length() == 0 && field.required) {
            throw new Invalid((KIND_CHECKBOX.equals(field.kind) ? "Tick " : "Fill in ")
                    + field.text);
        }
        return v;
    }

    /// True for an optionally signed decimal such as `12` or `-3.5`, of at
    /// most 18 digits.
    public static boolean isNumber(String s) {
        int digits = 0;
        boolean dot = false;
        for (int iter = 0; iter < s.length(); iter++) {
            char c = s.charAt(iter);
            if (c >= '0' && c <= '9') {
                digits++;
            } else if (c == '.' && !dot && digits > 0) {
                dot = true;
            } else if (c != '-' || iter != 0) {
                return false;
            }
        }
        return digits > 0 && digits <= 18 && s.charAt(s.length() - 1) != '.';
    }

    /// The values an app's data answer carries, `{"values": {"key": v}}`
    /// with string, number or boolean values: at most 50, each cut to 200
    /// characters. Anything else is refused.
    public static Map<String, String> values(String json) throws Invalid {
        if (json == null || json.length() == 0 || json.length() > 32000) {
            throw new Invalid("The data answer is empty or too long");
        }
        Reader r = new Reader(json);
        Object v = r.value(0);
        if (!(v instanceof Map) || !(((Map) v).get("values") instanceof Map)) {
            throw new Invalid("The data answer has no values object");
        }
        Map in = (Map) ((Map) v).get("values");
        if (in.size() > 50) {
            throw new Invalid("The data answer has more than 50 values");
        }
        Map<String, String> out = new LinkedHashMap<String, String>();
        for (Object k : in.keySet()) {
            String key = String.valueOf(k);
            if (!isDataKey(key)) {
                throw new Invalid("A data key is letters, digits, '.', '-' or '_'");
            }
            Object x = in.get(k);
            String s;
            if (x instanceof String) {
                s = (String) x;
            } else if (x instanceof Double) {
                double d = ((Double) x).doubleValue();
                s = d == Math.floor(d) && Math.abs(d) < 1e15 ? Long.toString((long) d)
                        : String.valueOf(d);
            } else if (x instanceof Boolean) {
                s = x.toString();
            } else {
                throw new Invalid("A data value is a string, number or boolean");
            }
            out.put(key, s.length() > 200 ? s.substring(0, 200) : s);
        }
        return out;
    }

    /// A message an app's action answer carries, `{"message": "..."}`, cut
    /// to 300 characters; "" when there is none or the answer is not that.
    public static String message(String json) {
        if (json == null || json.length() == 0 || json.length() > 32000) {
            return "";
        }
        try {
            Object v = new Reader(json).value(0);
            Object m = v instanceof Map ? ((Map) v).get("message") : null;
            if (!(m instanceof String)) {
                return "";
            }
            String s = (String) m;
            return s.length() > 300 ? s.substring(0, 300) : s;
        } catch (Invalid bad) {
            return "";
        }
    }

    // -------------------------------------------------------------- JSON

    /// A small strict JSON reader: objects keep their order, numbers are
    /// doubles. Shared so both sides read a tree identically.
    static final class Reader {
        final String s;
        int at;

        Reader(String s) {
            this.s = s;
        }

        void space() {
            while (at < s.length()) {
                char c = s.charAt(at);
                if (c != ' ' && c != '\n' && c != '\r' && c != '\t') {
                    return;
                }
                at++;
            }
        }

        Invalid bad() {
            return new Invalid("The JSON is not valid (at " + at + ")");
        }

        private boolean next(char c) {
            space();
            if (at < s.length() && s.charAt(at) == c) {
                at++;
                return true;
            }
            return false;
        }

        Object value(int nesting) throws Invalid {
            if (nesting > MAX_JSON_NESTING) {
                throw new Invalid("The JSON nests too deeply");
            }
            space();
            if (at >= s.length()) {
                throw bad();
            }
            char c = s.charAt(at);
            if (c == '{') {
                at++;
                return object(nesting);
            }
            if (c == '[') {
                at++;
                List<Object> l = new ArrayList<Object>();
                if (next(']')) {
                    return l;
                }
                while (true) {
                    l.add(value(nesting + 1));
                    if (next(']')) {
                        return l;
                    }
                    if (!next(',')) {
                        throw bad();
                    }
                }
            }
            if (c == '"') {
                return string();
            }
            if (s.startsWith("true", at)) {
                at += 4;
                return Boolean.TRUE;
            }
            if (s.startsWith("false", at)) {
                at += 5;
                return Boolean.FALSE;
            }
            if (s.startsWith("null", at)) {
                at += 4;
                return null;
            }
            return number();
        }

        private Object object(int nesting) throws Invalid {
            Map<String, Object> m = new LinkedHashMap<String, Object>();
            if (next('}')) {
                return m;
            }
            while (true) {
                space();
                if (at >= s.length() || s.charAt(at) != '"') {
                    throw bad();
                }
                String k = string();
                if (!next(':')) {
                    throw bad();
                }
                if (m.containsKey(k)) {
                    throw new Invalid("A property appears twice: " + cut(k));
                }
                m.put(k, value(nesting + 1));
                if (next('}')) {
                    return m;
                }
                if (!next(',')) {
                    throw bad();
                }
            }
        }

        private Object number() throws Invalid {
            int start = at;
            while (at < s.length()) {
                char d = s.charAt(at);
                if ((d >= '0' && d <= '9') || d == '-' || d == '+' || d == '.' || d == 'e'
                        || d == 'E') {
                    at++;
                } else {
                    break;
                }
            }
            if (start == at) {
                throw bad();
            }
            try {
                return Double.valueOf(Double.parseDouble(s.substring(start, at)));
            } catch (NumberFormatException e) {
                throw bad();
            }
        }

        String string() throws Invalid {
            at++;
            StringBuilder b = new StringBuilder();
            while (at < s.length()) {
                char c = s.charAt(at);
                at++;
                if (c == '"') {
                    return b.toString();
                }
                if (c != '\\') {
                    b.append(c);
                    continue;
                }
                if (at >= s.length()) {
                    throw bad();
                }
                char e = s.charAt(at);
                at++;
                if (e == 'n') {
                    b.append('\n');
                } else if (e == 't') {
                    b.append('\t');
                } else if (e == 'r') {
                    b.append('\r');
                } else if (e == 'b') {
                    b.append('\b');
                } else if (e == 'f') {
                    b.append('\f');
                } else if (e == 'u') {
                    b.append(hex4());
                } else if (e == '"' || e == '\\' || e == '/') {
                    b.append(e);
                } else {
                    throw bad();
                }
            }
            throw bad();
        }

        private char hex4() throws Invalid {
            if (at + 4 > s.length()) {
                throw bad();
            }
            int code = 0;
            for (int iter = 0; iter < 4; iter++) {
                int h = Character.digit(s.charAt(at), 16);
                at++;
                if (h < 0) {
                    throw bad();
                }
                code = code * 16 + h;
            }
            return (char) code;
        }
    }
}
