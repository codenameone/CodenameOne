package com.codename1.javascript;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.BrowserComponent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.events.BrowserNavigationCallback;
import com.codename1.util.Callback;
import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.Queue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JavascriptContextTest extends UITestBase {
    private BrowserComponent browser;

    @BeforeEach
    void setUpBrowser() {
        browser = mock(BrowserComponent.class);
        when(browser.getBrowserNavigationCallback()).thenReturn(null);
        doNothing().when(browser).execute(anyString());
        when(browser.executeAndReturnString(anyString())).thenReturn("undefined");
    }

    @FormTest
    void constructorInstallsNavigationCallbackAndListener() {
        BrowserNavigationCallback original = mock(BrowserNavigationCallback.class);
        when(browser.getBrowserNavigationCallback()).thenReturn(original);

        JavascriptContext context = new JavascriptContext(browser);

        assertNotNull(context);
        verify(browser).getBrowserNavigationCallback();
        verify(browser).setBrowserNavigationCallback(any(BrowserNavigationCallback.class));
        verify(browser).addWebEventListener(eq("scriptMessageReceived"), any(ActionListener.class));
    }

    @FormTest
    void setBrowserComponentUninstallsPreviousInstance() {
        BrowserNavigationCallback original = mock(BrowserNavigationCallback.class);
        when(browser.getBrowserNavigationCallback()).thenReturn(original);
        JavascriptContext context = new JavascriptContext(browser);

        ArgumentCaptor<ActionListener> listenerCaptor = ArgumentCaptor.forClass(ActionListener.class);
        verify(browser).addWebEventListener(eq("scriptMessageReceived"), listenerCaptor.capture());

        BrowserComponent replacement = mock(BrowserComponent.class);
        when(replacement.getBrowserNavigationCallback()).thenReturn(null);

        context.setBrowserComponent(replacement);

        InOrder order = inOrder(browser);
        order.verify(browser).setBrowserNavigationCallback(any(BrowserNavigationCallback.class));
        order.verify(browser).setBrowserNavigationCallback(original);
        verify(browser).removeWebEventListener(eq("scriptMessageReceived"), eq(listenerCaptor.getValue()));
        verify(replacement).setBrowserNavigationCallback(any(BrowserNavigationCallback.class));
        verify(replacement).addWebEventListener(eq("scriptMessageReceived"), any(ActionListener.class));
    }

    @FormTest
    void getReturnsNumericValuesAndResetsReturnVariable() {
        JavascriptContext context = new JavascriptContext(browser);
        Queue<String> responses = new ArrayDeque<String>();
        responses.add("3");
        responses.add("number");
        when(browser.executeAndReturnString(anyString())).thenAnswer(invocation -> responses.remove());

        Object value = context.get("1+2");

        assertTrue(value instanceof Double);
        assertEquals(3.0d, ((Double) value).doubleValue(), 0.00001d);
        verify(browser).execute(contains("=undefined"));
    }

    @FormTest
    void getReturnsBooleanValues() {
        JavascriptContext context = new JavascriptContext(browser);
        Queue<String> responses = new ArrayDeque<String>();
        responses.add("true");
        responses.add("boolean");
        when(browser.executeAndReturnString(anyString())).thenAnswer(invocation -> responses.remove());

        Object value = context.get("1<2");

        assertSame(Boolean.TRUE, value);
    }

    @FormTest
    void getWrapsObjectsWithJSObject() {
        JavascriptContext context = new JavascriptContext(browser);
        Queue<String> responses = new ArrayDeque<String>();
        responses.add("objectPointer");
        responses.add("object");
        responses.add("ignored");
        responses.add("object");
        responses.add("0");
        when(browser.executeAndReturnString(anyString())).thenAnswer(invocation -> responses.remove());

        Object value = context.get("window");

        assertTrue(value instanceof JSObject);
        JSObject object = (JSObject) value;
        assertEquals(context.jsLookupTable + "[" + object.objectId + "]", object.toJSPointer());
    }

    @FormTest
    void setEscapesStringsAndHandlesNumbers() {
        JavascriptContext context = new JavascriptContext(browser);
        Queue<String> responses = new ArrayDeque<String>();
        responses.add("ok");
        when(browser.executeAndReturnString(anyString())).thenAnswer(invocation -> responses.remove());

        context.set("window.title", "O'Reilly\\Docs");

        ArgumentCaptor<String> jsCaptor = ArgumentCaptor.forClass(String.class);
        verify(browser).executeAndReturnString(jsCaptor.capture());
        String executed = jsCaptor.getValue();
        assertTrue(executed.contains("(window.title='O\\'Reilly\\\\Docs')"), executed);

        responses.add("ok");
        context.set("counter", Integer.valueOf(5));
        verify(browser, times(2)).executeAndReturnString(anyString());
    }

    @FormTest
    void setUsesJSObjectPointerWhenProvided() {
        JavascriptContext context = new JavascriptContext(browser);
        Queue<String> responses = new ArrayDeque<String>();
        responses.add("ok");
        when(browser.executeAndReturnString(anyString())).thenAnswer(invocation -> responses.remove());

        JSObject pointer = mock(JSObject.class);
        when(pointer.toJSPointer()).thenReturn("lookup[7]");

        context.set("window.child", pointer);

        ArgumentCaptor<String> jsCaptor = ArgumentCaptor.forClass(String.class);
        verify(browser).executeAndReturnString(jsCaptor.capture());
        assertTrue(jsCaptor.getValue().contains("window.child=lookup[7]"));
    }

    @FormTest
    void getAsyncRegistersCallbackAndExecutesScript() {
        JavascriptContext context = new JavascriptContext(browser);
        JSObject window = mock(JSObject.class);
        setWindow(context, window);
        Callback callback = mock(Callback.class);

        final JSFunction[] captured = new JSFunction[1];
        doAnswer(invocation -> {
            JSFunction fn = invocation.getArgument(1);
            captured[0] = fn;
            return null;
        }).when(window).set(anyString(), any(JSFunction.class), eq(true));
        doNothing().when(window).set(anyString(), isNull(), eq(true));

        context.getAsync("1+2", callback);

        verify(window).set(eq("callback$$0"), any(JSFunction.class), eq(true));
        verify(browser).execute(contains("callback$$0(1+2)"));

        JSFunction registered = captured[0];
        Object[] args = new Object[]{"value"};
        registered.apply(window, args);
        verify(callback).onSucess("value");
        verify(window).set(eq("callback$$0"), isNull(), eq(true));
    }

    @FormTest
    void cleanupReleasesCollectedEntries() throws Exception {
        JavascriptContext context = new JavascriptContext(browser);
        java.lang.reflect.Field mapField = JavascriptContext.class.getDeclaredField("objectMap");
        mapField.setAccessible(true);
        java.util.Map map = (java.util.Map) mapField.get(context);
        map.put(Integer.valueOf(3), new WeakReference<Object>(null));

        context.cleanup();

        verify(browser).execute(argThat(script -> script.contains("var id = 3")
                && script.contains("delete " + context.jsLookupTable + "[id]")));
        assertTrue(map.isEmpty());
    }

    private void setWindow(JavascriptContext context, JSObject window) {
        try {
            java.lang.reflect.Field field = JavascriptContext.class.getDeclaredField("window");
            field.setAccessible(true);
            field.set(context, window);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
