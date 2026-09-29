/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.bench;

/**
 * objectAllocation, alone, so it can be profiled densely.
 *
 * <p>In the shared runner this workload is a few hundred milliseconds of a
 * multi-second process, so a sample lands ~50 hits on it -- enough to name the
 * top frame and nothing more. Every Node escapes into the list, so NEITHER VM can
 * stack-allocate: this is allocation throughput plus whatever the collector does
 * about 8M objects per rep that die almost immediately.</p>
 */
public class AllocOnly {
    static final class Node {
        int v;
        Node next;
        Node(int v, Node next) { this.v = v; this.next = next; }
    }

    public static void main(String[] args) {
        long checksum = 0;
        for (int rep = 0; rep < 60; rep++) {
            Node head = null;
            for (int i = 0; i < 8000000; i++) {
                head = new Node(i, head);
                if ((i & 511) == 0) {
                    Node p = head;
                    int steps = 0;
                    while (p != null && steps < 48) { checksum += p.v; p = p.next; steps++; }
                    head = null;
                }
            }
        }
        System.out.println("DONE " + checksum);
    }
}
