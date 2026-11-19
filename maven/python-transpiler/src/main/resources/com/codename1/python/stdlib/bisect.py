# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""Bisection algorithms over sorted lists."""


from _cn1bisect import bisect_left, bisect_right, insort_left, insort_right

bisect = bisect_right
insort = insort_right
