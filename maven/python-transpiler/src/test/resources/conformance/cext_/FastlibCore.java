package com.codename1.python.ext;

import com.codename1.python.runtime.NativeModule;
import com.codename1.python.runtime.Py;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class FastlibCore {
    private FastlibCore() {
    }

    public static void install() {
        new NativeModule("fastlib._core")
                .constant("VERSION", "1.0")
                .function("fast_sum", args -> {
                    if (!(args[0] instanceof List)) {
                        throw Py.error(Py.TYPE_ERROR, "fast_sum() needs a list");
                    }
                    long s = 0;
                    for (Object o : (List<?>) args[0]) {
                        s += ((Number) o).longValue();
                    }
                    return s;
                })
                .function("scale", args -> {
                    double k = ((Number) args[1]).doubleValue();
                    List<Object> out = new ArrayList<Object>();
                    for (Object o : (List<?>) args[0]) {
                        out.add(((Number) o).doubleValue() * k);
                    }
                    return out.toArray();
                })
                .function("join_words", args -> String.join("-", (List<String>) args[0]))
                .function("describe", args -> {
                    List<Object> out = new ArrayList<Object>();
                    for (Map.Entry<?, ?> e : new TreeMap<Object, Object>((Map<?, ?>) args[0]).entrySet()) {
                        out.add(new Object[] {e.getKey(), e.getValue()});
                    }
                    return out;
                })
                .install();
    }
}
