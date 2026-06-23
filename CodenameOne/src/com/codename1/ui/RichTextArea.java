/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores
 * CA 94065 USA or visit www.oracle.com if you need additional information or
 * have any questions.
 */
package com.codename1.ui;

import com.codename1.util.SuccessCallback;

/// A native visual editor for rich text / HTML content (a WYSIWYG editor).
///
/// `RichTextArea` lets the user visually edit formatted text - bold, italic, lists, links, colors,
/// headings and more - and exchange the result as HTML with your application. It works on phones and
/// tablets (with the on screen virtual keyboard) as well as on desktops (with a physical keyboard).
///
/// By default the editor is implemented as a `contenteditable` surface hosted inside the platform's
/// native web widget (`BrowserComponent`), which makes it 100% cross platform and gives correct
/// keyboard, selection and IME behavior on every device for free. A platform port may transparently
/// replace this with a fully native editing widget; see
/// `com.codename1.impl.CodenameOneImplementation#createNativeEditorPeer(Object, String)`.
///
/// #### Basic usage
///
/// ```java
/// Form hi = new Form("Rich Text", new BorderLayout());
/// RichTextArea editor = new RichTextArea();
/// editor.setHtml("<p>Hello <b>world</b></p>");
///
/// Toolbar tb = hi.getToolbar();
/// tb.addCommandToRightBar("B", null, e -> editor.bold());
/// tb.addCommandToRightBar("I", null, e -> editor.italic());
///
/// hi.add(BorderLayout.CENTER, editor);
/// hi.show();
///
/// // later, read the edited content back:
/// editor.getHtml(html -> Log.p("User wrote: " + html));
/// ```
///
/// @author Shai Almog
public class RichTextArea extends AbstractEditorComponent {
    private String pendingHtml;
    private String placeholderText = "";

    /// Creates an empty rich text editor.
    public RichTextArea() {
        super("RichTextArea");
    }

    /// Creates a rich text editor initialized with the supplied HTML.
    ///
    /// #### Parameters
    ///
    /// - `html`: the initial HTML content
    public RichTextArea(String html) {
        this();
        setHtml(html);
    }

    String getEditorType() {
        return "richtext";
    }

    /// Replaces the entire editor content with the supplied HTML.
    ///
    /// #### Parameters
    ///
    /// - `html`: the HTML to display and edit
    public void setHtml(String html) {
        pendingHtml = html;
        command("setHtml", html == null ? "" : html);
    }

    /// Retrieves the current editor content as an HTML string. The callback is invoked on the EDT.
    ///
    /// #### Parameters
    ///
    /// - `callback`: receives the HTML content
    public void getHtml(SuccessCallback<String> callback) {
        query("getHtml", null, callback);
    }

    /// Retrieves the current editor content as plain text (markup stripped). The callback is invoked on
    /// the EDT.
    ///
    /// #### Parameters
    ///
    /// - `callback`: receives the plain text content
    public void getText(SuccessCallback<String> callback) {
        query("getText", null, callback);
    }

    /// Inserts the supplied HTML fragment at the current cursor position.
    ///
    /// #### Parameters
    ///
    /// - `html`: the HTML fragment to insert
    public void insertHtml(String html) {
        command("insertHtml", html);
    }

    /// Inserts an image at the current cursor position.
    ///
    /// #### Parameters
    ///
    /// - `url`: the image URL (may be an http(s) URL or a data: URI)
    public void insertImage(String url) {
        command("insertImage", url);
    }

    /// Sets the placeholder text shown when the editor is empty.
    ///
    /// #### Parameters
    ///
    /// - `text`: the placeholder hint
    public void setPlaceholder(String text) {
        placeholderText = text == null ? "" : text;
        command("setPlaceholder", placeholderText);
    }

    /// Returns the current placeholder text.
    public String getPlaceholder() {
        return placeholderText;
    }

    /// Toggles bold styling on the current selection.
    public void bold() {
        command("bold", null);
    }

    /// Toggles italic styling on the current selection.
    public void italic() {
        command("italic", null);
    }

    /// Toggles underline styling on the current selection.
    public void underline() {
        command("underline", null);
    }

    /// Toggles strike-through styling on the current selection.
    public void strikeThrough() {
        command("strikeThrough", null);
    }

    /// Converts the current block(s) into an ordered (numbered) list.
    public void insertOrderedList() {
        command("insertOrderedList", null);
    }

    /// Converts the current block(s) into an unordered (bulleted) list.
    public void insertUnorderedList() {
        command("insertUnorderedList", null);
    }

    /// Increases the indentation of the current block.
    public void indent() {
        command("indent", null);
    }

    /// Decreases the indentation of the current block.
    public void outdent() {
        command("outdent", null);
    }

    /// Left aligns the current block.
    public void justifyLeft() {
        command("justifyLeft", null);
    }

    /// Center aligns the current block.
    public void justifyCenter() {
        command("justifyCenter", null);
    }

    /// Right aligns the current block.
    public void justifyRight() {
        command("justifyRight", null);
    }

    /// Wraps the current selection in a hyperlink.
    ///
    /// #### Parameters
    ///
    /// - `url`: the link target
    public void createLink(String url) {
        command("createLink", url);
    }

    /// Removes the hyperlink covering the current selection.
    public void removeLink() {
        command("unlink", null);
    }

    /// Sets the foreground (text) color of the current selection.
    ///
    /// #### Parameters
    ///
    /// - `rgb`: the color as a 0xRRGGBB integer
    public void setForegroundColor(int rgb) {
        command("foreColor", toCss(rgb));
    }

    /// Sets the highlight (background) color of the current selection.
    ///
    /// #### Parameters
    ///
    /// - `rgb`: the color as a 0xRRGGBB integer
    public void setHighlightColor(int rgb) {
        command("hiliteColor", toCss(rgb));
    }

    /// Applies a block format / heading to the current block. Common values are `"p"`, `"h1"` ..
    /// `"h6"`, `"pre"` and `"blockquote"`.
    ///
    /// #### Parameters
    ///
    /// - `tag`: the block tag name
    public void setBlockFormat(String tag) {
        command("formatBlock", tag);
    }

    /// Applies a relative font size (1 through 7, matching the legacy HTML font size scale) to the
    /// current selection.
    ///
    /// #### Parameters
    ///
    /// - `size`: a value between 1 and 7
    public void setFontSize(int size) {
        command("fontSize", String.valueOf(size));
    }

    /// Removes all inline formatting from the current selection.
    public void removeFormat() {
        command("removeFormat", null);
    }

    /// Undoes the last editing operation.
    public void undo() {
        command("undo", null);
    }

    /// Redoes the last undone editing operation.
    public void redo() {
        command("redo", null);
    }

    /// Queries whether a given inline command (e.g. `"bold"`, `"italic"`, `"underline"`) is currently
    /// active for the selection, which is useful to keep a formatting toolbar in sync. The callback is
    /// invoked on the EDT.
    ///
    /// #### Parameters
    ///
    /// - `command`: the command name to test
    ///
    /// - `callback`: receives true if the command is active for the current selection
    public void queryCommandState(String command, final SuccessCallback<Boolean> callback) {
        query("state", command, new SuccessCallback<String>() {
            public void onSucess(String value) {
                callback.onSucess("1".equals(value));
            }
        });
    }

    private static String toCss(int rgb) {
        String s = Integer.toHexString(rgb & 0xffffff);
        while (s.length() < 6) {
            s = "0" + s;
        }
        return "#" + s;
    }

    String createEditorHtml() {
        // A self-contained contenteditable editor. No external resources are required which keeps the
        // editor fully functional offline and adds zero footprint to apps that do not use it.
        return "<!DOCTYPE html><html><head><meta charset=\"utf-8\">"
            + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no\">"
            + "<style>"
            + "html,body{margin:0;padding:0;height:100%;-webkit-text-size-adjust:100%;}"
            + "*{-webkit-tap-highlight-color:rgba(0,0,0,0);box-sizing:border-box;}"
            + "#ed{min-height:100%;padding:8px;outline:none;font:16px -apple-system,Roboto,'Segoe UI',sans-serif;"
            + "line-height:1.4;word-wrap:break-word;-webkit-user-select:text;user-select:text;}"
            + "#ed:empty:before{content:attr(data-ph);color:#9aa0a6;pointer-events:none;}"
            + "#ed img{max-width:100%;height:auto;}"
            + "#ed[contenteditable=false]{opacity:.7;}"
            + "</style></head><body>"
            + "<div id=\"ed\" contenteditable=\"true\" data-ph=\"\"></div>"
            + "<script>"
            + "var ed=document.getElementById('ed');"
            + "function cn1post(m){try{if(window.cn1PostMessage){window.cn1PostMessage(m);}else if(window.parent&&window.parent!==window){window.parent.postMessage(m,'*');}}catch(e){}}"
            + "function fire(t,v){cn1post('cn1ed:'+t+(v==null?'':(':'+v)));}"
            + "ed.addEventListener('input',function(){fire('change',null);});"
            + "document.addEventListener('selectionchange',function(){fire('selection',null);});"
            + "function exec(c,a){try{document.execCommand(c,false,a);}catch(e){}ed.focus();}"
            + "window.cn1editor={"
            + "cmd:function(name,arg){"
            + "switch(name){"
            + "case 'setHtml':ed.innerHTML=arg||'';fire('change',null);break;"
            + "case 'insertHtml':exec('insertHTML',arg);break;"
            + "case 'insertImage':exec('insertImage',arg);break;"
            + "case 'setPlaceholder':ed.setAttribute('data-ph',arg||'');break;"
            + "case 'setEditable':ed.setAttribute('contenteditable',arg=='1'?'true':'false');break;"
            + "case 'focus':ed.focus();break;"
            + "case 'blur':ed.blur();break;"
            + "case 'createLink':exec('createLink',arg);break;"
            + "case 'foreColor':exec('foreColor',arg);break;"
            + "case 'hiliteColor':if(!document.execCommand('hiliteColor',false,arg)){exec('backColor',arg);}break;"
            + "case 'formatBlock':exec('formatBlock',arg);break;"
            + "case 'fontSize':exec('fontSize',arg);break;"
            + "default:exec(name,arg);break;"
            + "}},"
            + "query:function(name,arg){"
            + "switch(name){"
            + "case 'getHtml':return ed.innerHTML;"
            + "case 'getText':return ed.innerText||ed.textContent||'';"
            + "case 'state':try{return document.queryCommandState(arg)?'1':'0';}catch(e){return '0';}"
            + "default:return '';"
            + "}}};"
            + "fire('ready',null);"
            + "</script></body></html>";
    }
}
