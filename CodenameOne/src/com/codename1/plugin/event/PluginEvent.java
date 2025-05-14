/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.plugin.event;

import com.codename1.ui.events.ActionEvent;

/**
 * Parent class for all plugin events.  Plugin events are fired by the Codename One runtime
 * to give plugins an opportunity to override the implementation of certain core methods.
 *
 * <p>For example, the {@link OpenGalleryEvent} is fired when the {@link Display#openGallery()} method
 * is called.  This event will be passed to registered plugins, which  can choose to "consume" the event
 * and override the behaviour.</p>
 *
 * <p>If a plugin wishes to handle the event, it should call {@link #consume()} on the event to prevent
 * Codename One from proceeding to handle the request itself.</p>
 *
 * <p>Events that require a synchronous return value should call the {@link #setPluginEventResponse(Object)} method
 * with the return value.  {@link #setPluginEventResponse(Object)} calls {@link #consume()}, so you do not need
 * to call both of these methods.</p>
 * @param <T> The type of the response to the event. If the event does not require a response, this should be {@link Void}.
 *
 * @since 8.0
 * @author Steve Hannah
 */
public abstract class PluginEvent<T> extends ActionEvent {

    private T pluginEventResponse;

    /**
     * Creates a new plugin event with the given source and type.
     * @param source The source of the event.  May be null.
     * @param type The type of the event.  All PluginEvent classes should have a corresponding enum type in the
     *             {@link Type} enum.
     */
    public PluginEvent(Object source, Type type) {
        super(source, type);
    }

    /**
     * Sets the return value of processing the event.  This method calls {@link #consume()}, so you don't need to
     * call both to consume the event and set the response.
     * @param response
     */
    public void setPluginEventResponse(T response) {
        this.consume();
        this.pluginEventResponse = response;
    }

    /**
     * Gets the response to the event.  This will be null if the event has not been consumed.
     * @return The result of processing the event.
     */
    public T getPluginEventResponse() {
        return pluginEventResponse;
    }
}
