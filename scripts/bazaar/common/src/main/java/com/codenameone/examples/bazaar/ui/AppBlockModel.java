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
package com.codenameone.examples.bazaar.ui;

import com.codenameone.examples.bazaar.api.BlockValueDto;
import com.codenameone.examples.bazaar.api.ExtensionBlockDto;
import com.codenameone.examples.bazaar.ops.Blocks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// What one app block shows, worked out from the server's
/// [ExtensionBlockDto] without touching a component: the tree parsed by the
/// same validator the server used, its `{{placeholders}}` bound to the app's
/// data, and the fields and actions it declares. [AppBlocks] turns it into
/// components; a tree that no longer validates becomes an error line rather
/// than a partial block.
public final class AppBlockModel {
    /// Rows to print, in order ([Blocks.Row]); links carry their url.
    public final List<Blocks.Row> rows;
    /// The inputs, with their keys, kinds and options.
    public final List<Blocks.Node> fields;
    /// Action buttons: their labels and the action names they post.
    public final List<Blocks.Node> actions;
    public final String extensionId;
    public final String installationId;
    public final String appName;
    public final String target;
    /// The app's or the tree's failure, or empty.
    public final String error;

    private AppBlockModel(ExtensionBlockDto d, List<Blocks.Row> rows, List<Blocks.Node> fields,
            List<Blocks.Node> actions, String error) {
        this.extensionId = text(d.extensionId());
        this.installationId = text(d.installationId());
        this.appName = text(d.appName());
        this.target = text(d.target());
        this.rows = rows;
        this.fields = fields;
        this.actions = actions;
        this.error = error;
    }

    private static String text(String s) {
        return s == null ? "" : s;
    }

    /// The model of `d`.
    public static AppBlockModel of(ExtensionBlockDto d) {
        List<Blocks.Row> rows = new ArrayList<Blocks.Row>();
        List<Blocks.Node> fields = new ArrayList<Blocks.Node>();
        List<Blocks.Node> actions = new ArrayList<Blocks.Node>();
        String error = text(d.error());
        try {
            Blocks.Node root = Blocks.parse(text(d.tree()));
            rows = Blocks.flatten(root, values(d.values()));
            fields = Blocks.fields(root);
            actions = Blocks.actions(root);
        } catch (Blocks.Invalid bad) {
            error = bad.getMessage();
        }
        return new AppBlockModel(d, rows, fields, actions, error);
    }

    /// The app's data as a map; a key given twice keeps its last value.
    public static Map<String, String> values(List<BlockValueDto> values) {
        Map<String, String> out = new HashMap<String, String>();
        for (int iter = 0; values != null && iter < values.size(); iter++) {
            BlockValueDto v = values.get(iter);
            if (v != null && v.key() != null) {
                out.put(v.key(), v.value() == null ? "" : v.value());
            }
        }
        return out;
    }

    /// The answers in `given` to this block's fields, each checked by its
    /// kind ([Blocks#answer]), as the list an action or checkout sends;
    /// throws naming the first field that is wrong.
    public List<BlockValueDto> answers(Map<String, String> given) throws Blocks.Invalid {
        List<BlockValueDto> out = new ArrayList<BlockValueDto>();
        for (int iter = 0; iter < fields.size(); iter++) {
            Blocks.Node f = fields.get(iter);
            out.add(new BlockValueDto(f.key, Blocks.answer(f, given.get(f.key))));
        }
        return out;
    }

    /// The checkout field id of `field` in this block: `app:<extension>:<key>`.
    public String fieldId(Blocks.Node field) {
        return "app:" + extensionId + ":" + field.key;
    }
}
