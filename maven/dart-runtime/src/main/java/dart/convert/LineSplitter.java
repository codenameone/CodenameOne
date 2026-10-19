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
package dart.convert;

import dart.core.DartList;

/** {@code LineSplitter}: splits on \n, \r\n and \r, dropping the terminators. */
public final class LineSplitter implements dart.async.StreamTransformer<String, String> {

    public static DartList<String> split(String lines) {
        return ByteCodec.splitLines(lines);
    }

    public DartList<String> convert(String data) {
        return ByteCodec.splitLines(data);
    }

    /** {@code stream.transform(const LineSplitter())}: a line split across chunks is joined. */
    @Override
    public dart.async.Stream<String> bind(final dart.async.Stream<String> source) {
        return dart.async.AsyncStar.asyncStar(new dart.runtime.Funcs.VoidFunc1<dart.async.AsyncStar.Sink<String>>() {
            @Override
            public void call(dart.async.AsyncStar.Sink<String> sink) {
                dart.async.StreamIterator<String> it = new dart.async.StreamIterator<String>(source);
                StringBuilder pending = new StringBuilder();
                try {
                    while (it.next()) {
                        String chunk = it.current();
                        if (chunk == null) {
                            continue;
                        }
                        pending.append(chunk);
                        int start = 0;
                        for (int i = 0; i < pending.length(); i++) {
                            char c = pending.charAt(i);
                            if (c == '\n' || c == '\r') {
                                if (c == '\r' && i + 1 == pending.length()) {
                                    // A CR at the end of a chunk may be half of a CRLF.
                                    break;
                                }
                                sink.yield$(pending.substring(start, i));
                                if (c == '\r' && pending.charAt(i + 1) == '\n') {
                                    i++;
                                }
                                start = i + 1;
                            }
                        }
                        pending.delete(0, start);
                    }
                    String rest = pending.toString();
                    if (rest.endsWith("\r")) {
                        rest = rest.substring(0, rest.length() - 1);
                        sink.yield$(rest);
                    } else if (rest.length() > 0) {
                        sink.yield$(rest);
                    }
                } finally {
                    it.cancel();
                }
            }
        });
    }
}
