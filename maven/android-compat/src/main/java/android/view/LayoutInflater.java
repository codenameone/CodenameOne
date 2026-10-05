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
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import com.codename1.androidcompat.runtime.AndroidRuntime;
import com.codename1.androidcompat.runtime.CompiledAttributeSet;
import com.codename1.androidcompat.runtime.FrameworkViews;
import com.codename1.androidcompat.runtime.ResValue;
import com.codename1.androidcompat.runtime.XmlNode;

/// Instantiates layout XML into views.
///
/// Layouts were compiled at build time, so there is no parsing here: the
/// compiled tree is walked and every element is constructed with `new`
/// through the application's generated factory (or the framework's), which
/// is what lets inflation work where reflection does not.
public abstract class LayoutInflater {

    public interface Factory {
        View onCreateView(String name, Context context, AttributeSet attrs);
    }

    public interface Factory2 extends Factory {
        View onCreateView(View parent, String name, Context context, AttributeSet attrs);
    }

    public interface Filter {
        boolean onLoadClass(Class clazz);
    }

    protected final Context mContext;
    private Factory mFactory;
    private Factory2 mFactory2;
    private Factory2 mPrivateFactory;
    private Filter mFilter;

    protected LayoutInflater(Context context) {
        mContext = context;
    }

    protected LayoutInflater(LayoutInflater original, Context newContext) {
        mContext = newContext;
        mFactory = original.mFactory;
        mFactory2 = original.mFactory2;
        mPrivateFactory = original.mPrivateFactory;
        mFilter = original.mFilter;
    }

    public static LayoutInflater from(Context context) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (inflater == null) {
            throw new AssertionError("LayoutInflater not found.");
        }
        return inflater;
    }

    public abstract LayoutInflater cloneInContext(Context newContext);

    public Context getContext() {
        return mContext;
    }

    public final Factory getFactory() {
        return mFactory;
    }

    public final Factory2 getFactory2() {
        return mFactory2;
    }

    public void setFactory(Factory factory) {
        if (mFactory != null) {
            throw new IllegalStateException("A factory has already been set on this LayoutInflater");
        }
        mFactory = factory;
    }

    public void setFactory2(Factory2 factory) {
        if (mFactory != null) {
            throw new IllegalStateException("A factory has already been set on this LayoutInflater");
        }
        mFactory = factory;
        mFactory2 = factory;
    }

    /// The framework's own factory, asked after the application's: an
    /// activity or fragment installs its fragment manager here, which is
    /// what turns `<fragment>` tags into fragments.
    public void setPrivateFactory(Factory2 factory) {
        mPrivateFactory = factory;
    }

    public Filter getFilter() {
        return mFilter;
    }

    public void setFilter(Filter filter) {
        mFilter = filter;
    }

    public View inflate(int resource, ViewGroup root) {
        return inflate(resource, root, root != null);
    }

    public View inflate(int resource, ViewGroup root, boolean attachToRoot) {
        XmlNode node = mContext.getResources().getXmlNode(resource);
        return inflate(node, root, attachToRoot);
    }

    public View inflate(org.xmlpull.v1.XmlPullParser parser, ViewGroup root) {
        return inflate(parser, root, root != null);
    }

    /// Inflates from a parser over a compiled layout (`getResources().getLayout`):
    /// the element the parser is on, or its root when it has not started.
    /// Layouts are compiled at build time, so a parser over XML text cannot
    /// be inflated, as on Android.
    public View inflate(org.xmlpull.v1.XmlPullParser parser, ViewGroup root, boolean attachToRoot) {
        if (!(parser instanceof com.codename1.androidcompat.runtime.CompiledXmlParser)) {
            throw new InflateException("only compiled layout resources can be inflated");
        }
        com.codename1.androidcompat.runtime.CompiledXmlParser p =
                (com.codename1.androidcompat.runtime.CompiledXmlParser) parser;
        XmlNode node = p.getCurrentNode();
        return inflate(node == null ? p.getRootNode() : node, root, attachToRoot);
    }

    /// Inflates a compiled layout tree.
    public View inflate(XmlNode node, ViewGroup root, boolean attachToRoot) {
        if (node.tag.equals("merge")) {
            if (root == null || !attachToRoot) {
                throw new InflateException("<merge /> can be used only with a valid ViewGroup root and attachToRoot=true");
            }
            rInflate(node, root, mContext);
            return root;
        }
        CompiledAttributeSet attrs = new CompiledAttributeSet(node);
        View temp = createViewFromTag(root, node.tag, mContext, attrs);
        ViewGroup.LayoutParams params = null;
        if (root != null) {
            params = root.generateLayoutParams(attrs);
            if (!attachToRoot) {
                temp.setLayoutParams(params);
            }
        }
        rInflateChildren(node, temp, attrs);
        if (root != null && attachToRoot) {
            root.addView(temp, params);
            return root;
        }
        return temp;
    }

    private void rInflateChildren(XmlNode node, View parent, AttributeSet attrs) {
        rInflate(node, parent, parent.getContext());
    }

    private void rInflate(XmlNode node, View parent, Context context) {
        for (XmlNode child : node.children) {
            String name = child.tag;
            if (name.equals("requestFocus")) {
                parent.requestFocus();
            } else if (name.equals("tag")) {
                parseTag(child, parent);
            } else if (name.equals("include")) {
                if (!(parent instanceof ViewGroup)) {
                    throw new InflateException("<include /> can only be used inside of a ViewGroup");
                }
                parseInclude(child, (ViewGroup) parent, context);
            } else if (name.equals("merge")) {
                throw new InflateException("<merge /> must be the root element");
            } else {
                if (!(parent instanceof ViewGroup)) {
                    throw new InflateException(parent.getClass().getName() + " cannot have child views ("
                            + child.getSource() + " line " + child.line + ")");
                }
                ViewGroup viewGroup = (ViewGroup) parent;
                CompiledAttributeSet attrs = new CompiledAttributeSet(child);
                View view = createViewFromTag(parent, name, context, attrs);
                ViewGroup.LayoutParams params = viewGroup.generateLayoutParams(attrs);
                rInflateChildren(child, view, attrs);
                viewGroup.addView(view, params);
            }
        }
        parent.onFinishInflate();
    }

    private void parseTag(XmlNode node, View view) {
        ResValue id = node.valueForAttr(android.R.attr.id);
        ResValue value = node.value(XmlNode.NS_ANDROID, "value");
        if (id != null) {
            view.setTag(id.data, value == null ? null : value.string);
        }
    }

    private void parseInclude(XmlNode include, ViewGroup group, Context context) {
        ResValue layoutRef = include.value(XmlNode.NS_NONE, "layout");
        if (layoutRef == null || layoutRef.data == 0) {
            throw new InflateException("You must specify a valid layout reference in <include />");
        }
        int layoutId = layoutRef.data;
        if (layoutRef.type == android.util.TypedValue.TYPE_ATTRIBUTE) {
            android.util.TypedValue tv = new android.util.TypedValue();
            if (context.getTheme().resolveAttribute(layoutId, tv, true)) {
                layoutId = tv.resourceId != 0 ? tv.resourceId : tv.data;
            }
        }
        XmlNode child = context.getResources().getXmlNode(layoutId);
        CompiledAttributeSet includeAttrs = new CompiledAttributeSet(include);
        // An android:theme on the <include> themes the included tree, merged
        // or not, and replaces any theme the included root declares, as on
        // Android.
        Context themed = applyThemeAttr(context, includeAttrs);
        boolean themeOverride = themed != context;
        context = themed;
        if (child.tag.equals("merge")) {
            rInflate(child, group, context);
            return;
        }
        CompiledAttributeSet childAttrs = new CompiledAttributeSet(child);
        View view = createViewFromTag(group, child.tag, context, childAttrs, themeOverride);
        ViewGroup.LayoutParams params = null;
        // The <include>'s own layout_* attributes win when it sets both
        // dimensions, as on Android.
        if (include.valueForAttr(android.R.attr.layout_width) != null
                && include.valueForAttr(android.R.attr.layout_height) != null) {
            try {
                params = group.generateLayoutParams(includeAttrs);
            } catch (RuntimeException ignored) {
                params = null;
            }
        }
        if (params == null) {
            params = group.generateLayoutParams(childAttrs);
        }
        view.setLayoutParams(params);
        rInflateChildren(child, view, childAttrs);
        TypedArray a = context.obtainStyledAttributes(includeAttrs, android.R.styleable.Include);
        int id = a.getResourceId(android.R.styleable.Include_id, View.NO_ID);
        int visibility = a.getInt(android.R.styleable.Include_visibility, -1);
        a.recycle();
        if (id != View.NO_ID) {
            view.setId(id);
        }
        switch (visibility) {
            case 0:
                view.setVisibility(View.VISIBLE);
                break;
            case 1:
                view.setVisibility(View.INVISIBLE);
                break;
            case 2:
                view.setVisibility(View.GONE);
                break;
            default:
                break;
        }
        group.addView(view);
    }

    View createViewFromTag(View parent, String name, Context context, AttributeSet attrs) {
        return createViewFromTag(parent, name, context, attrs, false);
    }

    /// `context` wrapped in the theme `attrs` names with `android:theme`, or
    /// `context` itself when it names none.
    private static Context applyThemeAttr(Context context, CompiledAttributeSet attrs) {
        ResValue themeRef = attrs.valueForAttr(android.R.attr.theme);
        if (themeRef == null || themeRef.data == 0) {
            return context;
        }
        int themeId = themeRef.data;
        if (themeRef.type == android.util.TypedValue.TYPE_ATTRIBUTE) {
            android.util.TypedValue tv = new android.util.TypedValue();
            themeId = context.getTheme().resolveAttribute(themeId, tv, true) ? tv.resourceId : 0;
        }
        return themeId != 0 ? new ContextThemeWrapper(context, themeId) : context;
    }

    private View createViewFromTag(View parent, String name, Context context, AttributeSet attrs,
                                   boolean ignoreThemeAttr) {
        if (name.equals("view")) {
            String cls = attrs.getAttributeValue("", "class");
            if (cls != null) {
                name = cls;
            }
        }
        CompiledAttributeSet cas = attrs instanceof CompiledAttributeSet ? (CompiledAttributeSet) attrs : null;
        if (cas != null && !ignoreThemeAttr) {
            context = applyThemeAttr(context, cas);
        }
        View view = null;
        if (mFactory2 != null) {
            view = mFactory2.onCreateView(parent, name, context, attrs);
        } else if (mFactory != null) {
            view = mFactory.onCreateView(name, context, attrs);
        }
        if (view == null && mPrivateFactory != null) {
            view = mPrivateFactory.onCreateView(parent, name, context, attrs);
        }
        if (view == null) {
            view = onCreateView(parent, name, context, attrs);
        }
        if (view == null) {
            throw new InflateException((cas == null ? "" : cas.getPositionDescription() + ": ")
                    + "Error inflating class " + name);
        }
        return view;
    }

    protected View onCreateView(View parent, String name, Context context, AttributeSet attrs) {
        return createView(context, name, null, attrs);
    }

    protected View onCreateView(String name, AttributeSet attrs) throws ClassNotFoundException {
        return createView(mContext, name, null, attrs);
    }

    public final View createView(String name, String prefix, AttributeSet attrs) throws ClassNotFoundException {
        return createView(mContext, name, prefix, attrs);
    }

    /// Constructs `name` through the generated application factory, falling
    /// back to the framework's own view classes.
    public final View createView(Context context, String name, String prefix, AttributeSet attrs) {
        String full = prefix == null ? name : prefix + name;
        AndroidRuntime rt = AndroidRuntime.getInstance();
        View v = rt == null ? null : rt.createView(full, context, attrs);
        if (v == null) {
            v = FrameworkViews.create(full, context, attrs);
        }
        return v;
    }
}
