/*****************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one                *
 * or more contributor license agreements.  See the NOTICE file              *
 * distributed with this work for additional information                     *
 * regarding copyright ownership.  The ASF licenses this file                *
 * to you under the Apache License, Version 2.0 (the                         *
 * "License"); you may not use this file except in compliance                *
 * with the License.  You may obtain a copy of the License at                *
 *                                                                           *
 *     http://www.apache.org/licenses/LICENSE-2.0                            *
 *                                                                           *
 * Unless required by applicable law or agreed to in writing,                *
 * software distributed under the License is distributed on an               *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY                    *
 * KIND, either express or implied.  See the License for the                 *
 * specific language governing permissions and limitations                   *
 * under the License.                                                        *
 *****************************************************************************/

package bsh;

import java.util.Hashtable;
import java.util.Map;

/**
 * CN1-safe capability checks. The playground build does not support the Java SE
 * accessibility toggles used by upstream BeanShell, so those features are
 * reduced to simple opt-in flags instead of reflective probing.
 */
public final class Capabilities {
    private static final Map<String, Class<?>> classes = new Hashtable<String, Class<?>>();
    private static boolean accessibility;

    private Capabilities() {
    }

    public static boolean haveSwing() {
        return false;
    }

    public static boolean haveAccessibility() {
        return accessibility;
    }

    public static void setAccessibility(boolean enabled) {
        accessibility = enabled;
        BshClassManager.memberCache.clear();
    }

    public static boolean classExists(String name) {
        if (!classes.containsKey(name)) {
            try {
                classes.put(name, Class.forName(name));
            } catch (Throwable t) {
                classes.put(name, null);
            }
        }
        return classes.get(name) != null;
    }

    public static Class<?> getExisting(String name) {
        classExists(name);
        return classes.get(name);
    }

    public static class Unavailable extends UtilEvalError {
        public Unavailable(String s) {
            super(s);
        }

        public Unavailable(String s, Throwable cause) {
            super(s, cause);
        }
    }
}
