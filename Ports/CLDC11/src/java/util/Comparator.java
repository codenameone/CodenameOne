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

package java.util;

/// A `Comparator` is used to compare two objects to determine their ordering with
/// respect to each other. On a given `Collection`, a `Comparator` can be used to
/// obtain a sorted `Collection` which is *totally ordered*. For a `Comparator`
/// to be *consistent with equals*, its {code #compare(Object, Object)}
/// method has to return zero for each pair of elements (a,b) where a.equals(b)
/// holds true. It is recommended that a `Comparator` implements
/// `java.io.Serializable`.
///
/// #### Since
///
/// 1.2
public interface Comparator<T> {
    /// Compares the two specified objects to determine their relative ordering. The ordering
    /// implied by the return value of this method for all possible pairs of
    /// `(object1, object2)` should form an *equivalence relation*.
    /// This means that
    ///
    /// - `compare(a,a)` returns zero for all `a`
    ///
    /// - the sign of `compare(a,b)` must be the opposite of the sign of `compare(b,a)` for all pairs of (a,b)
    ///
    /// - From `compare(a,b) > 0` and `compare(b,c) > 0` it must
    /// follow `compare(a,c) > 0` for all possible combinations of `(a,b,c)`
    ///
    /// #### Parameters
    ///
    /// - `object1`: an `Object`.
    ///
    /// - `object2`: a second `Object` to compare with `object1`.
    ///
    /// #### Returns
    ///
    /// an integer  0 if `object1` is greater than `object2`.
    ///
    /// #### Throws
    ///
    /// - `ClassCastException`: if objects are not of the correct type.
    public int compare(T object1, T object2);

    /// Compares this `Comparator` with the specified `Object` and indicates whether they
    /// are equal. In order to be equal, `object` must represent the same object
    /// as this instance using a class-specific comparison.
    ///
    /// A `Comparator` never needs to override this method, but may choose so for
    /// performance reasons.
    ///
    /// #### Parameters
    ///
    /// - `object`: the `Object` to compare with this comparator.
    ///
    /// #### Returns
    ///
    /// @return boolean `true` if specified `Object` is the same as this
    /// `Object`, and `false` otherwise.
    ///
    /// #### See also
    ///
    /// - Object#hashCode
    ///
    /// - Object#equals
    public boolean equals(Object object);
}
