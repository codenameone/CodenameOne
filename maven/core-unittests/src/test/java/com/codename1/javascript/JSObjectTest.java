package com.codename1.javascript;

import com.codename1.test.UITestBase;
import com.codename1.ui.BrowserComponent;
import com.codename1.util.Callback;
import com.codename1.util.SuccessCallback;
import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.Queue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JSObjectTest extends UITestBase {
    private BrowserComponent browser;
    private Queue<String> responses;

    private static class RecordingJavascriptContext extends JavascriptContext {
        private final Queue<JSObject> recordedFunctions = new ArrayDeque<JSObject>();
        private final Queue<JSObject> recordedSelfs = new ArrayDeque<JSObject>();
        private final Queue<Object[]> recordedParams = new ArrayDeque<Object[]>();
        private final Queue<Callback> recordedCallbacks = new ArrayDeque<Callback>();

        RecordingJavascriptContext(BrowserComponent browser) {
            super(browser);
        }

        @Override
        public void callAsync(JSObject func, JSObject self, Object[] params, Callback callback) {
            recordedFunctions.add(func);
            recordedSelfs.add(self);
            recordedParams.add(params);
            recordedCallbacks.add(callback);
        }

        Callback removeNextCallback() {
            return recordedCallbacks.remove();
        }

        JSObject removeNextFunction() {
            return recordedFunctions.remove();
        }

        JSObject removeNextSelf() {
            return recordedSelfs.remove();
        }

        Object[] removeNextParams() {
            return recordedParams.remove();
        }
    }

    @BeforeEach
    void setUpBrowser() {
        browser = mock(BrowserComponent.class);
        responses = new ArrayDeque<String>();
        when(browser.getBrowserNavigationCallback()).thenReturn(null);
        when(implementation.createSoftWeakRef(any())).thenAnswer(invocation -> new WeakReference<Object>(invocation.getArgument(0)));
        when(implementation.extractHardRef(any())).thenAnswer(invocation -> {
            Object ref = invocation.getArgument(0);
            if (ref instanceof WeakReference) {
                return ((WeakReference) ref).get();
            }
            return null;
        });
        doNothing().when(browser).execute(anyString());
        when(browser.executeAndReturnString(anyString())).thenAnswer(invocation -> responses.isEmpty() ? "0" : responses.remove());
    }

    @Test
    void constructorStoresObjectIdAndRetains() throws Exception {
        responses.add("ignored");
        responses.add("object");
        responses.add("5");
        JavascriptContext context = createContextSpy();

        JSObject object = new JSObject(context, "window");

        assertEquals(5, object.objectId);
        Map objectMap = getObjectMap(context);
        assertTrue(objectMap.containsKey(Integer.valueOf(5)));
    }

    @Test
    void constructorThrowsWhenExpressionIsNotObject() {
        responses.add("ignored");
        responses.add("string");
        JavascriptContext context = createContextSpy();

        assertThrows(JSException.class, () -> new JSObject(context, "1"));
    }

    @Test
    void typedGettersDelegateToContext() {
        responses.add("ignored");
        responses.add("object");
        responses.add("3");
        RecordingJavascriptContext context = createContextSpy();
        JSObject object = new JSObject(context, "window");

        String pointer = object.toJSPointer();
        doReturn("name").when(context).get(pointer + ".label");
        doReturn(Double.valueOf(10)).when(context).get(pointer + ".count");
        doReturn(Boolean.TRUE).when(context).get(pointer + ".flag");
        doReturn(Double.valueOf(7)).when(context).get(pointer + "[2]");
        doReturn(object).when(context).get(pointer + ".self");

        assertEquals("name", object.getString("label"));
        assertEquals(10, object.getInt("count"));
        assertTrue(object.getBoolean("flag"));
        assertEquals(10.0d, object.getDouble("count"), 0.00001d);
        assertEquals(object, object.getObject("self"));
        assertEquals(7, object.getInt(2));
        assertEquals(7.0d, object.getDouble(2), 0.00001d);
    }

    @Test
    void setWithJSFunctionRegistersCallback() {
        responses.add("ignored");
        responses.add("object");
        responses.add("4");
        JavascriptContext context = createContextSpy();
        JSObject object = new JSObject(context, "window");

        JSFunction function = mock(JSFunction.class);
        object.set("handler", function, true);

        verify(context).addCallback(eq(object), eq("handler"), eq(function), eq(true));
        verify(context, never()).set(anyString(), any());
    }

    @Test
    void setDelegatesToContextForValues() {
        responses.add("ignored");
        responses.add("object");
        responses.add("6");
        JavascriptContext context = createContextSpy();
        JSObject object = new JSObject(context, "window");

        doNothing().when(context).set(anyString(), any());
        doNothing().when(context).set(anyString(), any(), anyBoolean());

        object.set("name", "Codename One");
        object.set("'0'", Integer.valueOf(2));
        object.setBoolean("flag", true);
        object.setDouble("ratio", 3.5d);
        object.setInt(1, 9);

        String pointer = object.toJSPointer();
        verify(context).set(eq(pointer + ".name"), eq((Object) "Codename One"));
        verify(context).set(eq(pointer + "['0']"), eq((Object) Integer.valueOf(2)));
        verify(context).set(eq(pointer + ".flag"), eq((Object) Boolean.TRUE));
        verify(context).set(eq(pointer + ".ratio"), eq((Object) Double.valueOf(3.5d)));
        verify(context).set(eq(pointer + "[1]"), eq((Object) Integer.valueOf(9)), eq(false));
    }

    @Test
    void callDelegatesToJavascriptContext() {
        responses.add("ignored");
        responses.add("object");
        responses.add("8");
        JavascriptContext context = createContextSpy();
        JSObject object = new JSObject(context, "window");

        doReturn("done").when(context).call(anyString(), eq(object), any(Object[].class));

        Object result = object.call("sum", new Object[]{Integer.valueOf(1), Integer.valueOf(2)});

        assertEquals("done", result);
        verify(context).call(eq(object.toJSPointer() + ".sum"), eq(object), any(Object[].class));
    }

    @Test
    void callAsyncDelegatesToJavascriptContext() {
        responses.add("ignored");
        responses.add("object");
        responses.add("9");
        JavascriptContext context = createContextSpy();
        JSObject object = new JSObject(context, "window");

        Callback callback = mock(Callback.class);
        SuccessCallback successCallback = mock(SuccessCallback.class);

        String pointer = object.toJSPointer();
        object.callAsync("action", new Object[]{}, callback);
        object.callAsync("action", new Object[]{}, successCallback);

        verify(context).callAsync(eq(object), eq(object), any(Object[].class), eq(callback));
        verify(context, times(2)).callAsync(eq(object), eq(object), any(Object[].class), any(Callback.class));

        Callback first = context.removeNextCallback();
        assertSame(callback, first);
        assertSame(object, context.removeNextFunction());
        assertSame(object, context.removeNextSelf());
        assertArrayEquals(new Object[]{}, context.removeNextParams());

        Callback second = context.removeNextCallback();
        assertNotSame(successCallback, second);
        second.onSucess("value");
        verify(successCallback).onSucess("value");
        assertSame(object, context.removeNextFunction());
        assertSame(object, context.removeNextSelf());
        assertArrayEquals(new Object[]{}, context.removeNextParams());
    }

    @Test
    void removeCallbackDelegatesToContext() {
        responses.add("ignored");
        responses.add("object");
        responses.add("10");
        JavascriptContext context = createContextSpy();
        JSObject object = new JSObject(context, "window");

        doNothing().when(context).removeCallback(eq(object), anyString(), eq(false));

        object.removeCallback("listener");

        verify(context).removeCallback(eq(object), eq("listener"), eq(false));
    }

    private RecordingJavascriptContext createContextSpy() {
        RecordingJavascriptContext context = new RecordingJavascriptContext(browser);
        RecordingJavascriptContext spyContext = spy(context);
        doAnswer(invocation -> {
            JSObject obj = invocation.getArgument(0);
            Map map = getObjectMap(spyContext);
            map.put(Integer.valueOf(obj.objectId), new WeakReference<Object>(obj));
            return null;
        }).when(spyContext).retain(any(JSObject.class));
        doNothing().when(spyContext).cleanup();
        return spyContext;
    }

    private Map getObjectMap(JavascriptContext context) throws Exception {
        java.lang.reflect.Field field = JavascriptContext.class.getDeclaredField("objectMap");
        field.setAccessible(true);
        return (Map) field.get(context);
    }
}
