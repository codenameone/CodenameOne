/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
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
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package android.view;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import com.codename1.androidcompat.runtime.ResValue;
import com.codename1.androidcompat.runtime.XmlNode;
import com.codename1.androidcompat.testing.AndroidTestSupport;
import com.codename1.androidcompat.testing.MainThreadRule;

import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/// The children of an included `<merge>` layout join the including parent
/// without finishing it: its `onFinishInflate()` runs once, after every
/// child, as on Android. The merged include used to finish the parent
/// itself, so it ran early -- before the siblings after the `<include>` --
/// and then again.
public class MergedIncludeFinishInflateTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    private static final XmlNode[] NONE = new XmlNode[0];
    private static final int MERGE_LAYOUT = 0x7f0b7701;

    /// Records the child count each time it is finished.
    static final class Host extends LinearLayout {
        final List<Integer> finishedWithChildren = new ArrayList<Integer>();

        Host(Context context) {
            super(context);
        }

        @Override
        protected void onFinishInflate() {
            super.onFinishInflate();
            finishedWithChildren.add(Integer.valueOf(getChildCount()));
        }
    }

    /// An element with `layout_width` and `layout_height` of `match_parent`.
    private static XmlNode element(String tag, XmlNode... children) {
        ResValue match = new ResValue(android.util.TypedValue.TYPE_INT_DEC, -1, null);
        return new XmlNode(tag, 1,
                new int[] {android.R.attr.layout_width, android.R.attr.layout_height},
                new byte[] {(byte) XmlNode.NS_ANDROID, (byte) XmlNode.NS_ANDROID},
                new String[] {"layout_width", "layout_height"},
                new ResValue[] {match, match}, null, children);
    }

    private static XmlNode include(int layout) {
        return new XmlNode("include", 2, new int[] {0}, new byte[] {(byte) XmlNode.NS_NONE},
                new String[] {"layout"},
                new ResValue[] {new ResValue(android.util.TypedValue.TYPE_REFERENCE, layout, null)},
                null, NONE);
    }

    private static Context withMergeLayout(Context base, final XmlNode merge) {
        final Resources real = base.getResources();
        final Resources res = new Resources(real.getManager()) {
            @Override
            public XmlNode getXmlNode(int id) {
                return id == MERGE_LAYOUT ? merge : real.getXmlNode(id);
            }
        };
        return new ContextWrapper(base) {
            @Override
            public Resources getResources() {
                return res;
            }
        };
    }

    private static LayoutInflater inflater(Context context) {
        LayoutInflater inflater = LayoutInflater.from(context).cloneInContext(context);
        inflater.setFactory2(new LayoutInflater.Factory2() {
            @Override
            public View onCreateView(View parent, String name, Context ctx, AttributeSet attrs) {
                return name.equals("Host") ? new Host(ctx) : null;
            }

            @Override
            public View onCreateView(String name, Context ctx, AttributeSet attrs) {
                return onCreateView(null, name, ctx, attrs);
            }
        });
        return inflater;
    }

    @Test
    public void parentFinishesOnceAfterEveryChild() {
        XmlNode merge = element("merge", element("View"), element("View"));
        Context context = withMergeLayout(AndroidTestSupport.context(), merge);
        XmlNode root = element("Host", include(MERGE_LAYOUT), element("View"));
        Host host = (Host) inflater(context).inflate(root, null, false);
        assertEquals(3, host.getChildCount());
        assertEquals("finished once, with all three children", "[3]", host.finishedWithChildren.toString());
    }

    @Test
    public void mergeRootDoesNotFinishTheExistingParent() {
        XmlNode merge = element("merge", element("View"));
        Context context = withMergeLayout(AndroidTestSupport.context(), merge);
        Host host = new Host(context);
        inflater(context).inflate(element("merge", include(MERGE_LAYOUT), element("View")), host, true);
        assertEquals(2, host.getChildCount());
        assertEquals("an existing parent is not finished by inflating into it", "[]",
                host.finishedWithChildren.toString());
    }
}
