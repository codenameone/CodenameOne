/** Copyright 2022 Nick nickl- Lombard
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. */
package bsh;

/** Implementation of the Safe Navigate operator ?. abort on null. In
 * order for the safe navigate operator to function, and stop evaluating
 * further nodes, we have to throw an exception on detecting a null.
 * This gets caught again in BSHAssignment where a null value is returned. */
public class SafeNavigate extends RuntimeException {
    private static final SafeNavigate abortException = new SafeNavigate();

    private SafeNavigate() { }

    public static SafeNavigate doAbort() {
        return abortException;
    }
}
