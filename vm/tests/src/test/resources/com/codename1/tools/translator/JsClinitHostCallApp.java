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
import com.codename1.impl.platform.js.VMHost;

/**
 * Regression fixture for issue #5774: a &lt;clinit&gt; that makes a
 * worker-to-host round trip.
 *
 * Class initialization used to be driven ONLY by a synchronous
 * run-to-completion loop that stepped the clinit generator with
 * {@code result.next()} and threw the yielded op away. A {@code HOST_CALL}
 * was therefore never posted to the host and the {@code yield} it came from
 * resumed with {@code undefined}, so {@code VALUE} landed as a non-answer and
 * the failure surfaced much later somewhere else entirely (in the reported
 * case, an NPE inside {@code LocalForage.<init>} because
 * {@code Window.current()} had answered {@code undefined}).
 *
 * The emitter now routes class-init guards inside generator methods through
 * {@code yield* _Ig(...)}, which drives the clinit on the real trampoline.
 * {@code VALUE} must be the host's answer, 42.
 */
public class JsClinitHostCallApp {
    static final class HostCallInClinit {
        static final int VALUE;

        static {
            VALUE = VMHost.echoInt(41);
        }
    }

    /**
     * Second shape, and the reason the guard owner has to be RESOLVED rather
     * than read off the instruction. {@code VALUE} is declared here, but javac
     * writes the access below as {@code getstatic Implementor.VALUE} -- naming
     * the implementing CLASS, not the declaring interface (verified with
     * javap: {@code Fieldref Q$Impl.VALUE}).
     *
     * <p>An analysis reading that raw owner sees the caller's own class,
     * concludes the class is already initialized by the time its code runs, and
     * drops the edge -- while the emitter resolves the owner to this interface
     * and guards it. {@code read()} then stays a plain function and its guard
     * runs on the synchronous run-to-completion path, so this host call is
     * never sent. That is the #5774 failure again, one resolution step away.
     */
    interface InterfaceSeed {
        int VALUE = VMHost.echoInt(20);
    }

    static final class Implementor implements InterfaceSeed {
        static int read() {
            return VALUE;
        }
    }

    public static void main(String[] args) {
        // 42 + 21: both shapes have to round-trip, and summing them means
        // either one failing changes the answer.
        System.exit(HostCallInClinit.VALUE + Implementor.read());
    }
}
