/*
 * Copyright (c) 2026 Codename One and contributors.
 */
package org.teavm.jso.dom.html;

import org.teavm.jso.JSProperty;
import org.teavm.jso.JSBody;

public interface HTMLScriptElement extends HTMLElement {
    @JSProperty String getSrc();
    @JSProperty void setSrc(String src);
    @JSProperty String getType();
    @JSProperty void setType(String type);
    @JSProperty String getText();
    @JSProperty void setText(String text);
    @JSProperty boolean isAsync();
    @JSProperty void setAsync(boolean async);
    @JSProperty boolean isDefer();
    @JSProperty void setDefer(boolean defer);
}