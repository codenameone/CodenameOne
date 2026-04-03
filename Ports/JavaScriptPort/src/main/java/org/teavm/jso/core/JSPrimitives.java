/*
 * Copyright (c) 2026 Codename One and contributors.
 */
package org.teavm.jso.core;

import org.teavm.jso.JSObject;

public interface JSString extends JSObject {
    String stringValue();
}

public interface JSBoolean extends JSObject {
    boolean booleanValue();
}

public interface JSNumber extends JSObject {
    double doubleValue();
    int intValue();
}

public interface JSFunction extends JSObject {}

public interface JSRegExp extends JSObject {
    boolean test(String str);
}