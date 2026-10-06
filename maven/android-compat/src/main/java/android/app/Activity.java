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
package android.app;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.util.AttributeSet;

import java.util.ArrayList;
import java.util.HashMap;

/// One screen of the application, shown as a Codename One form.
///
/// The runtime ([ActivityThread]) drives the lifecycle exactly in Android's
/// order -- create, start, post-create, resume; pause, stop, destroy -- and
/// keeps the back stack: starting an activity pauses and stops the one below
/// it, finishing returns to it and delivers any result.
public class Activity extends ContextThemeWrapper implements Window.Callback, LayoutInflater.Factory2 {

    public static final int RESULT_CANCELED = 0;
    public static final int RESULT_OK = -1;
    public static final int RESULT_FIRST_USER = 1;
    public static final int DEFAULT_KEYS_DISABLE = 0;

    ActivityThread.Record mRecord;
    Intent mIntent;
    Application mApplication;
    boolean mFinished;
    boolean mDestroyed;
    boolean mCalled;
    int mResultCode = RESULT_CANCELED;
    Intent mResultData;
    CharSequence mTitle;
    int mRequestedOrientation = -1;
    Window mWindow;

    static final String FRAGMENTS_TAG = "android:fragments";

    final FragmentManagerImpl mFragments = new FragmentManagerImpl(this, null);
    /// Every top-level fragment manager of this activity: the platform's
    /// first, then any an AndroidX `FragmentActivity` attached. Each is saved
    /// under its own key and moved through the activity's lifecycle together.
    final ArrayList<FragmentManagerImpl> mFragmentHosts = new ArrayList<FragmentManagerImpl>();
    /// Request codes handed out for fragments' `startActivityForResult`,
    /// mapped to the fragment and the code it asked for.
    private final HashMap<Integer, Object[]> mFragmentRequests = new HashMap<Integer, Object[]>();
    private int mNextFragmentRequest = 0x10000;
    Object mLastNonConfigurationInstance;
    /// Fragments retaining their instance across recreation, by host key.
    HashMap<String, ArrayList<Fragment>> mLastRetainedFragments;
    boolean mChangingConfigurations;
    int mConfigChangeFlags;
    /// The instance that replaced this one when it was recreated, or null.
    Activity mReplacement;

    public Activity() {
        super();
        mFragmentHosts.add(mFragments);
    }

    /// Runtime use: the application as base context, and the theme.
    final void attachBaseContextForRuntime(Context base, int theme) {
        attachBaseContext(base);
        setTheme(theme);
        // The activity's inflater turns <fragment> tags into fragments.
        getLayoutInflater().setPrivateFactory(this);
    }

    // ------------------------------------------------------------ lifecycle

    /// Restores the fragments saved with the previous instance and creates
    /// them; subclasses must call through, before inflating layouts that
    /// contain `<fragment>` tags.
    protected void onCreate(Bundle savedInstanceState) {
        for (int i = 0; i < mFragmentHosts.size(); i++) {
            FragmentManagerImpl host = mFragmentHosts.get(i);
            Bundle p = savedInstanceState == null ? null : savedInstanceState.getBundle(host.mSaveKey);
            if (p != null) {
                host.restoreAllState(p, mLastRetainedFragments == null ? null
                        : mLastRetainedFragments.get(host.mSaveKey));
            }
        }
        mLastRetainedFragments = null;
        for (int i = 0; i < mFragmentHosts.size(); i++) {
            mFragmentHosts.get(i).dispatchCreate();
        }
        mCalled = true;
    }

    public void onCreate(Bundle savedInstanceState, android.os.PersistableBundle persistentState) {
        onCreate(savedInstanceState);
    }

    protected void onPostCreate(Bundle savedInstanceState) {
        mCalled = true;
    }

    protected void onStart() {
        mCalled = true;
    }

    protected void onRestart() {
        mCalled = true;
    }

    protected void onResume() {
        mCalled = true;
    }

    protected void onPostResume() {
        mCalled = true;
    }

    protected void onPause() {
        mCalled = true;
    }

    protected void onStop() {
        mCalled = true;
    }

    protected void onDestroy() {
        mCalled = true;
    }

    /// Where [#onSaveInstanceState(Bundle)] keeps the view hierarchy's state,
    /// under Android's own key.
    private static final String VIEW_HIERARCHY_STATE = "android:viewHierarchyState";
    private static final String VIEWS_TAG = "android:views";

    /// Saves the state of every view with an id, and the fragments;
    /// subclasses must call through.
    protected void onSaveInstanceState(Bundle outState) {
        if (mRecord != null) {
            android.util.SparseArray<android.os.Parcelable> views = new android.util.SparseArray<android.os.Parcelable>();
            mRecord.decor.saveHierarchyState(views);
            Bundle hierarchy = new Bundle();
            hierarchy.putSparseParcelableArray(VIEWS_TAG, views);
            outState.putBundle(VIEW_HIERARCHY_STATE, hierarchy);
        }
        for (int i = 0; i < mFragmentHosts.size(); i++) {
            FragmentManagerImpl host = mFragmentHosts.get(i);
            Bundle p = host.saveAllState();
            if (p != null) {
                outState.putBundle(host.mSaveKey, p);
            }
        }
    }

    /// Restores the views' state [#onSaveInstanceState(Bundle)] saved, into
    /// the views the new instance created with the same ids; subclasses
    /// must call through.
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        Bundle hierarchy = savedInstanceState == null ? null : savedInstanceState.getBundle(VIEW_HIERARCHY_STATE);
        if (hierarchy == null || mRecord == null) {
            return;
        }
        android.util.SparseArray<android.os.Parcelable> views = hierarchy.getSparseParcelableArray(VIEWS_TAG);
        if (views != null) {
            mRecord.decor.restoreHierarchyState(views);
        }
    }

    protected void onNewIntent(Intent intent) {
    }

    protected void onUserLeaveHint() {
    }

    public void onUserInteraction() {
    }

    public void onWindowFocusChanged(boolean hasFocus) {
    }

    public void onAttachedToWindow() {
    }

    public void onDetachedFromWindow() {
    }

    public void onConfigurationChanged(Configuration newConfig) {
        mCalled = true;
        for (int i = 0; i < mFragmentHosts.size(); i++) {
            mFragmentHosts.get(i).dispatchConfigurationChanged(newConfig);
        }
    }

    public void onLowMemory() {
        for (int i = 0; i < mFragmentHosts.size(); i++) {
            mFragmentHosts.get(i).dispatchLowMemory();
        }
    }

    public void onTrimMemory(int level) {
    }

    public void onContentChanged() {
    }

    protected void onTitleChanged(CharSequence title, int color) {
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
    }

    public boolean isFinishing() {
        return mFinished;
    }

    public boolean isDestroyed() {
        return mDestroyed;
    }

    /// True while the activity is being destroyed to be recreated, as for a
    /// configuration change it does not handle itself.
    public boolean isChangingConfigurations() {
        return mChangingConfigurations;
    }

    public int getChangingConfigurations() {
        return mConfigChangeFlags;
    }

    /// An object handed to the next instance when the activity is recreated
    /// for a configuration change; see [#getLastNonConfigurationInstance()].
    public Object onRetainNonConfigurationInstance() {
        return null;
    }

    public Object getLastNonConfigurationInstance() {
        return mLastNonConfigurationInstance;
    }

    // ------------------------------------------------------------ fragments

    public FragmentManager getFragmentManager() {
        return mFragments;
    }

    public void onAttachFragment(Fragment fragment) {
    }

    public void startActivityFromFragment(Fragment fragment, Intent intent, int requestCode) {
        startActivityFromFragment(fragment, intent, requestCode, null);
    }

    /// Starts an activity for `fragment`: a result comes back to the
    /// fragment's `onActivityResult` with the request code it used.
    public void startActivityFromFragment(Fragment fragment, Intent intent, int requestCode, Bundle options) {
        if (requestCode < 0) {
            ActivityThread.startActivity(this, intent, -1);
            return;
        }
        if ((requestCode & 0xffff0000) != 0) {
            throw new IllegalArgumentException("Can only use lower 16 bits for requestCode");
        }
        int code = mNextFragmentRequest++;
        if (mNextFragmentRequest < 0x10000) {
            mNextFragmentRequest = 0x10000;
        }
        mFragmentRequests.put(Integer.valueOf(code), new Object[] {fragment, Integer.valueOf(requestCode)});
        ActivityThread.startActivity(this, intent, code);
    }

    /// Runtime use, before the activity is destroyed to be recreated: the
    /// outstanding fragment requests as {code, host, fragment index,
    /// requested code} quadruples, which survive the fragments themselves.
    final int[] snapshotFragmentRequests() {
        int[] out = new int[mFragmentRequests.size() * 4];
        int n = 0;
        for (java.util.Map.Entry<Integer, Object[]> e : mFragmentRequests.entrySet()) {
            Object o = e.getValue()[0];
            if (!(o instanceof Fragment)) {
                continue;
            }
            Fragment f = (Fragment) o;
            int host = mFragmentHosts.indexOf(f.mFragmentManager);
            if (host >= 0 && f.mIndex >= 0) {
                out[n++] = e.getKey().intValue();
                out[n++] = host;
                out[n++] = f.mIndex;
                out[n++] = ((Integer) e.getValue()[1]).intValue();
            }
        }
        int[] trimmed = new int[n];
        System.arraycopy(out, 0, trimmed, 0, n);
        return trimmed;
    }

    /// Runtime use: points the requests [#snapshotFragmentRequests()] took
    /// at this (recreated) instance's fragments.
    final void restoreFragmentRequests(int[] snapshot, int nextRequest) {
        for (int i = 0; i + 3 < snapshot.length; i += 4) {
            int host = snapshot[i + 1];
            int index = snapshot[i + 2];
            FragmentManagerImpl fm = host < mFragmentHosts.size() ? mFragmentHosts.get(host) : null;
            Fragment f = fm != null && index < fm.mActive.size() ? fm.mActive.get(index) : null;
            if (f != null) {
                mFragmentRequests.put(Integer.valueOf(snapshot[i]), new Object[] {f, Integer.valueOf(snapshot[i + 3])});
            }
        }
        mNextFragmentRequest = nextRequest;
    }

    final int nextFragmentRequest() {
        return mNextFragmentRequest;
    }

    /// Runtime use: a finished activity's result, for this activity or for
    /// the fragment that asked for it.
    final void dispatchActivityResult(int requestCode, int resultCode, Intent data) {
        hostsNoteStateNotSaved();
        Object[] f = mFragmentRequests.remove(Integer.valueOf(requestCode));
        if (f != null) {
            Fragment frag = (Fragment) f[0];
            // Delivered while the fragment is still active -- detached
            // included, as Android finds it by its active-fragment id --
            // and dropped once it has been removed for good.
            if (frag.mIndex >= 0) {
                frag.onActivityResult(((Integer) f[1]).intValue(), resultCode, data);
            }
            return;
        }
        onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public View onCreateView(String name, Context context, AttributeSet attrs) {
        return null;
    }

    @Override
    public View onCreateView(View parent, String name, Context context, AttributeSet attrs) {
        if (!"fragment".equals(name)) {
            return onCreateView(name, context, attrs);
        }
        return mFragments.onCreateView(parent, name, context, attrs);
    }

    public void recreate() {
        ActivityThread.recreate(this);
    }

    // ------------------------------------------------------------ content

    public void setContentView(int layoutResID) {
        ViewGroup content = contentParent();
        content.removeAllViews();
        getLayoutInflater().inflate(layoutResID, content, true);
        onContentChanged();
    }

    public void setContentView(View view) {
        ViewGroup content = contentParent();
        content.removeAllViews();
        content.addView(view, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        onContentChanged();
    }

    public void setContentView(View view, ViewGroup.LayoutParams params) {
        ViewGroup content = contentParent();
        content.removeAllViews();
        content.addView(view, params);
        onContentChanged();
    }

    public void addContentView(View view, ViewGroup.LayoutParams params) {
        contentParent().addView(view, params);
        onContentChanged();
    }

    private ViewGroup contentParent() {
        if (mRecord == null) {
            throw new IllegalStateException("setContentView called before the activity was attached");
        }
        return mRecord.content;
    }

    public <T extends View> T findViewById(int id) {
        return mRecord == null ? null : mRecord.decor.<T>findViewById(id);
    }

    public final <T extends View> T requireViewById(int id) {
        T v = findViewById(id);
        if (v == null) {
            throw new IllegalArgumentException("ID does not reference a View inside this Activity");
        }
        return v;
    }

    public View getCurrentFocus() {
        return mRecord == null ? null : mRecord.decor.findFocus();
    }

    public Window getWindow() {
        return mWindow;
    }

    public WindowManager getWindowManager() {
        return (WindowManager) getSystemService(WINDOW_SERVICE);
    }

    public LayoutInflater getLayoutInflater() {
        return LayoutInflater.from(this);
    }

    public MenuInflater getMenuInflater() {
        return new MenuInflater(this);
    }

    public final boolean requestWindowFeature(int featureId) {
        return mWindow != null && mWindow.requestFeature(featureId);
    }

    public ActionBar getActionBar() {
        return mRecord == null ? null : mRecord.actionBar;
    }

    public void setTitle(CharSequence title) {
        mTitle = title;
        onTitleChanged(title, 0);
        if (mRecord != null) {
            mRecord.actionBar.setTitle(title);
        }
    }

    public void setTitle(int titleId) {
        setTitle(getText(titleId));
    }

    public void setTitleColor(int textColor) {
    }

    public final CharSequence getTitle() {
        return mTitle;
    }

    public void setVisible(boolean visible) {
    }

    public void setFinishOnTouchOutside(boolean finish) {
    }

    public void setRequestedOrientation(int requestedOrientation) {
        mRequestedOrientation = requestedOrientation;
        ActivityThread.applyOrientation(this);
    }

    public int getRequestedOrientation() {
        return mRequestedOrientation;
    }

    public void setImmersive(boolean immersive) {
    }

    public void setShowWhenLocked(boolean showWhenLocked) {
    }

    public void setTurnScreenOn(boolean turnScreenOn) {
    }

    public void setVolumeControlStream(int streamType) {
    }

    public void overridePendingTransition(int enterAnim, int exitAnim) {
    }

    public void setDefaultKeyMode(int mode) {
    }

    // ------------------------------------------------------------ intents & navigation

    public Intent getIntent() {
        return mIntent;
    }

    public void setIntent(Intent newIntent) {
        mIntent = newIntent;
    }

    public final Application getApplication() {
        return mApplication;
    }

    public ComponentName getComponentName() {
        return new ComponentName(getPackageName(), getClass().getName());
    }

    public String getLocalClassName() {
        String pkg = getPackageName();
        String cls = getClass().getName();
        return cls.startsWith(pkg + ".") ? cls.substring(pkg.length() + 1) : cls;
    }

    public ComponentName getCallingActivity() {
        return mRecord == null || mRecord.caller == null ? null : mRecord.caller.getComponentName();
    }

    @Override
    public void startActivity(Intent intent) {
        ActivityThread.startActivity(this, intent, -1);
    }

    @Override
    public void startActivity(Intent intent, Bundle options) {
        startActivity(intent);
    }

    public void startActivityForResult(Intent intent, int requestCode) {
        ActivityThread.startActivity(this, intent, requestCode);
    }

    public void startActivityForResult(Intent intent, int requestCode, Bundle options) {
        startActivityForResult(intent, requestCode);
    }

    public boolean startActivityIfNeeded(Intent intent, int requestCode) {
        startActivityForResult(intent, requestCode);
        return true;
    }

    public final void setResult(int resultCode) {
        mResultCode = resultCode;
        mResultData = null;
    }

    public final void setResult(int resultCode, Intent data) {
        mResultCode = resultCode;
        mResultData = data;
    }

    public void finish() {
        ActivityThread.finish(this);
    }

    public void finishAffinity() {
        ActivityThread.finishAll();
    }

    public void finishAndRemoveTask() {
        ActivityThread.finishAll();
    }

    public void finishActivity(int requestCode) {
        ActivityThread.finishChild(this, requestCode);
    }

    public boolean isTaskRoot() {
        return ActivityThread.isRoot(this);
    }

    public boolean moveTaskToBack(boolean nonRoot) {
        com.codename1.ui.Display.getInstance().minimizeApplication();
        return true;
    }

    public boolean navigateUpTo(Intent upIntent) {
        finish();
        return true;
    }

    public boolean onNavigateUp() {
        if (getParentActivityIntent() != null || mRecord != null) {
            finish();
            return true;
        }
        return false;
    }

    public Intent getParentActivityIntent() {
        return null;
    }

    public boolean shouldUpRecreateTask(Intent targetIntent) {
        return false;
    }

    /// Pops the fragment back stack, or finishes the activity when it is
    /// empty.
    public void onBackPressed() {
        if (!mFragments.isStateSaved() && mFragments.popBackStackImmediate()) {
            return;
        }
        finish();
    }

    public final void runOnUiThread(Runnable action) {
        if (com.codename1.ui.Display.getInstance().isEdt()) {
            action.run();
        } else {
            com.codename1.ui.CN.callSerially(action);
        }
    }

    public SharedPreferences getPreferences(int mode) {
        return getSharedPreferences(getLocalClassName(), mode);
    }

    @Override
    public Object getSystemService(String name) {
        return super.getSystemService(name);
    }

    // ------------------------------------------------------------ permissions

    public final void requestPermissions(String[] permissions, int requestCode) {
        ActivityThread.requestPermissions(this, permissions, requestCode);
    }

    public boolean shouldShowRequestPermissionRationale(String permission) {
        return false;
    }

    // ------------------------------------------------------------ menus

    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    public boolean onPrepareOptionsMenu(Menu menu) {
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home && mRecord != null
                && (mRecord.actionBar.getDisplayOptions() & ActionBar.DISPLAY_HOME_AS_UP) != 0) {
            return onNavigateUp();
        }
        return false;
    }

    public void onOptionsMenuClosed(Menu menu) {
    }

    public void invalidateOptionsMenu() {
        ActivityThread.invalidateOptionsMenu(this);
    }

    public void openOptionsMenu() {
        ActivityThread.openOptionsMenu(this);
    }

    public void closeOptionsMenu() {
    }

    public void registerForContextMenu(View view) {
        view.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
            @Override
            public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
                Activity.this.onCreateContextMenu(menu, v, menuInfo);
            }
        });
    }

    public void unregisterForContextMenu(View view) {
        view.setOnCreateContextMenuListener(null);
    }

    public void openContextMenu(View view) {
        view.showContextMenu();
    }

    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
    }

    public boolean onContextItemSelected(MenuItem item) {
        return false;
    }

    /// A context menu item was chosen: the activity first, then its
    /// fragments, as on Android.
    final boolean dispatchContextItemSelected(MenuItem item) {
        if (onContextItemSelected(item)) {
            return true;
        }
        for (int i = 0; i < mFragmentHosts.size(); i++) {
            if (mFragmentHosts.get(i).dispatchContextItemSelected(item)) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------ fragment hosts

    final void hostsNoteStateNotSaved() {
        for (int i = 0; i < mFragmentHosts.size(); i++) {
            mFragmentHosts.get(i).noteStateNotSaved();
        }
    }

    final void hostsExecPendingActions() {
        for (int i = 0; i < mFragmentHosts.size(); i++) {
            mFragmentHosts.get(i).execPendingActions();
        }
    }

    final void hostsDispatchActivityCreated() {
        for (int i = 0; i < mFragmentHosts.size(); i++) {
            mFragmentHosts.get(i).dispatchActivityCreated();
        }
    }

    final void hostsDispatchStart() {
        for (int i = 0; i < mFragmentHosts.size(); i++) {
            mFragmentHosts.get(i).dispatchStart();
        }
    }

    final void hostsDispatchResume() {
        for (int i = 0; i < mFragmentHosts.size(); i++) {
            mFragmentHosts.get(i).dispatchResume();
        }
    }

    final void hostsDispatchPause() {
        for (int i = 0; i < mFragmentHosts.size(); i++) {
            mFragmentHosts.get(i).dispatchPause();
        }
    }

    final void hostsDispatchStop() {
        for (int i = 0; i < mFragmentHosts.size(); i++) {
            mFragmentHosts.get(i).dispatchStop();
        }
    }

    final void hostsDispatchDestroy() {
        for (int i = 0; i < mFragmentHosts.size(); i++) {
            mFragmentHosts.get(i).dispatchDestroy();
        }
    }

    final boolean hostsCreateOptionsMenu(Menu menu, android.view.MenuInflater inflater) {
        boolean show = false;
        for (int i = 0; i < mFragmentHosts.size(); i++) {
            show |= mFragmentHosts.get(i).dispatchCreateOptionsMenu(menu, inflater);
        }
        return show;
    }

    final boolean hostsPrepareOptionsMenu(Menu menu) {
        boolean show = false;
        for (int i = 0; i < mFragmentHosts.size(); i++) {
            show |= mFragmentHosts.get(i).dispatchPrepareOptionsMenu(menu);
        }
        return show;
    }

    final boolean hostsOptionsItemSelected(MenuItem item) {
        for (int i = 0; i < mFragmentHosts.size(); i++) {
            if (mFragmentHosts.get(i).dispatchOptionsItemSelected(item)) {
                return true;
            }
        }
        return false;
    }

    /// The fragments of every host that retain their instance, by host key.
    final HashMap<String, ArrayList<Fragment>> hostsRetainNonConfig() {
        HashMap<String, ArrayList<Fragment>> out = null;
        for (int i = 0; i < mFragmentHosts.size(); i++) {
            FragmentManagerImpl host = mFragmentHosts.get(i);
            ArrayList<Fragment> retained = host.retainNonConfig();
            if (retained != null) {
                if (out == null) {
                    out = new HashMap<String, ArrayList<Fragment>>();
                }
                out.put(host.mSaveKey, retained);
            }
        }
        return out;
    }

    public void onContextMenuClosed(Menu menu) {
    }

    // ------------------------------------------------------------ input

    /// The back key is claimed on the down and acted on at the up, as on
    /// Android: the down only starts tracking, so an `onKeyUp` override sees
    /// the up, and an `onKeyDown` override that returns true without calling
    /// through still blocks back (its up is not tracked).
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            event.startTracking();
            return true;
        }
        return false;
    }

    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.isTracking() && !event.isCanceled()) {
            onBackPressed();
            return true;
        }
        return false;
    }

    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        return false;
    }

    public boolean dispatchKeyEvent(KeyEvent event) {
        if (mRecord != null && mRecord.decor.dispatchKeyEvent(event)) {
            return true;
        }
        return event.getAction() == KeyEvent.ACTION_DOWN ? onKeyDown(event.getKeyCode(), event)
                : onKeyUp(event.getKeyCode(), event);
    }

    public boolean onTouchEvent(MotionEvent event) {
        return false;
    }

    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getActionMasked() == MotionEvent.ACTION_DOWN) {
            onUserInteraction();
        }
        if (mRecord != null && mRecord.decor.dispatchTouchEvent(ev)) {
            return true;
        }
        return onTouchEvent(ev);
    }

    // ------------------------------------------------------------ dialogs (legacy)

    @Deprecated
    public final void showDialog(int id) {
        Dialog d = onCreateDialog(id);
        if (d != null) {
            d.show();
        }
    }

    @Deprecated
    protected Dialog onCreateDialog(int id) {
        return null;
    }

    @Deprecated
    public final void dismissDialog(int id) {
    }

    @Override
    public String toString() {
        return getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(this));
    }
}
