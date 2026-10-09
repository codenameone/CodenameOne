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
package com.codename1.unitycompat.unityengine;

import com.codename1.unitycompat.system.collections.IEnumerator;

/// `UnityEngine.MonoBehaviour`: the base of every game script.
///
/// Unity finds `Update`, `Start` and the rest by name, private or not. Here
/// each is a method that does nothing, and the translator gives a script
/// that declares `Update` an override of `$update` that calls it -- so the
/// engine makes an ordinary virtual call and nothing is looked up at run
/// time. `Invoke("Name", ...)` is served the same way, by [#$invoke].
@SuppressWarnings("PMD.MethodNamingConventions") // C# member names: translated code binds to them by name
public class MonoBehaviour extends Behaviour {
    boolean awake;
    boolean started;
    /// `OnEnable` has run and `OnDisable` has not.
    boolean enabledSeen;

    public void $awake() {
    }

    public void $start() {
    }

    public void $update() {
    }

    public void $fixedUpdate() {
    }

    public void $lateUpdate() {
    }

    public void $onEnable() {
    }

    public void $onDisable() {
    }

    public void $onDestroy() {
    }

    public void $onCollisionEnter2D(Collision2D collision) {
    }

    public void $onCollisionStay2D(Collision2D collision) {
    }

    public void $onCollisionExit2D(Collision2D collision) {
    }

    public void $onTriggerEnter2D(Collider2D other) {
    }

    public void $onTriggerStay2D(Collider2D other) {
    }

    public void $onTriggerExit2D(Collider2D other) {
    }

    /// The pointer went down on a 2D collider of this script's object.
    public void $onMouseDown() {
    }

    /// The pointer that went down on this script's object came up.
    public void $onMouseUp() {
    }

    /// The application went to the background (`true`) or came back.
    public void $onApplicationPause(boolean pauseStatus) {
    }

    /// The application lost the user's attention (`false`) or has it again.
    public void $onApplicationFocus(boolean hasFocus) {
    }

    /// Calls the method of this script that takes nothing and has this
    /// name, and says whether there is one. The translator overrides it in
    /// every script with a chain of string comparisons.
    public boolean $invoke(String methodName) {
        return false;
    }

    @Override
    void registered() {
        UnityRuntime.adopt(this);
    }

    @Override
    void unregistered() {
        UnityRuntime.forget(this);
    }

    @Override
    void activeChanged(boolean nowLive) {
        UnityRuntime.enabledChanged(this, nowLive);
    }

    public Coroutine StartCoroutine(IEnumerator routine) {
        return UnityRuntime.startCoroutine(this, routine);
    }

    public void StopCoroutine(Coroutine routine) {
        if (routine != null) {
            routine.done = true;
        }
    }

    public void StopAllCoroutines() {
        UnityRuntime.stopCoroutines(this);
    }

    public void Invoke(String methodName, float time) {
        UnityRuntime.invokeLater(this, methodName, time, 0f);
    }

    /// A repeat rate of zero or less would never let a frame end; it is
    /// taken, as Unity takes it, to mean no repeat.
    public void InvokeRepeating(String methodName, float time, float repeatRate) {
        UnityRuntime.invokeLater(this, methodName, time, repeatRate > 0f ? repeatRate : 0f);
    }

    public void CancelInvoke() {
        UnityRuntime.cancelInvoke(this, null);
    }

    public void CancelInvoke(String methodName) {
        UnityRuntime.cancelInvoke(this, methodName);
    }

    public boolean IsInvoking() {
        return UnityRuntime.isInvoking(this, null);
    }

    public boolean IsInvoking(String methodName) {
        return UnityRuntime.isInvoking(this, methodName);
    }

    public static void print(java.lang.Object message) {
        Debug.Log(message);
    }
}
