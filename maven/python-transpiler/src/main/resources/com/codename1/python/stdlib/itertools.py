# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""Iterator building blocks.

CPython implements this module in C; here it is Java (`_cn1itertools`).
"""

from _cn1itertools import (accumulate, batched, chain, combinations, combinations_with_replacement,
                           compress, count, cycle, dropwhile, filterfalse, groupby, islice, pairwise,
                           permutations, product, repeat, starmap, takewhile, tee, zip_longest)
