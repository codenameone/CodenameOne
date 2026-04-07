/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package java.lang;

/// This interface should be implemented by all classes that wish to define a
/// *natural order* of their instances.
/// `java.util.Collections#sort` and `java.util.Arrays#sort` can then
/// be used to automatically sort lists of classes that implement this interface.
///
/// The order rule must be both transitive (if `x.compareTo(y) < 0` and
/// `y.compareTo(z) < 0`, then `x.compareTo(z) < 0` must hold) and
/// invertible (the sign of the result of x.compareTo(y) must be equal to the
/// negation of the sign of the result of y.compareTo(x) for all combinations of
/// x and y).
///
/// In addition, it is recommended (but not required) that if and only if the
/// result of x.compareTo(y) is zero, then the result of x.equals(y) should be
/// `true`.
public interface Comparable<T> {
    
    /// Compares this object to the specified object to determine their relative
    /// order.
    ///
    /// #### Parameters
    ///
    /// - `another`: the object to compare to this instance.
    ///
    /// #### Returns
    ///
    /// @return a negative integer if this instance is less than `another`;
    /// a positive integer if this instance is greater than
    /// `another`; 0 if this instance has the same order as
    /// `another`.
    ///
    /// #### Throws
    ///
    /// - `ClassCastException`: @throws ClassCastException
    /// if `another` cannot be converted into something
    /// comparable to `this` instance.
    int compareTo(T another);
}
