# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""numpy.linalg: norms, determinants, inverses and solving, by LU with
partial pivoting."""

import math as _math
import numpy as _np


class LinAlgError(ValueError):
    pass


def norm(x, ord=None, axis=None):
    x = _np.asarray(x).astype(float)
    if axis is not None:
        return _np.sqrt(_np.sum(x * x, axis=axis))
    if ord is None or ord == "fro" or ord == 2 and x.ndim == 1:
        return _np.float64(_math.sqrt(float(_np.sum(x * x))))
    if ord == 1 and x.ndim == 1:
        return _np.float64(float(_np.sum(_np.absolute(x))))
    if ord == _np.inf and x.ndim == 1:
        return _np.float64(float(_np.amax(_np.absolute(x))))
    raise NotImplementedError("norm ord=%r" % (ord,))


def _lu(a):
    a = _np.asarray(a).astype(float)
    if a.ndim != 2 or a.shape[0] != a.shape[1]:
        raise LinAlgError("Last 2 dimensions of the array must be square")
    n = a.shape[0]
    m = a.tolist()
    perm = list(range(n))
    sign = 1.0
    for k in range(n):
        p = max(range(k, n), key=lambda i: abs(m[i][k]))
        if m[p][k] == 0:
            return m, perm, 0.0
        if p != k:
            m[k], m[p] = m[p], m[k]
            perm[k], perm[p] = perm[p], perm[k]
            sign = -sign
        for i in range(k + 1, n):
            f = m[i][k] / m[k][k]
            m[i][k] = f
            row_i, row_k = m[i], m[k]
            for j in range(k + 1, n):
                row_i[j] -= f * row_k[j]
    return m, perm, sign


def det(a):
    m, perm, sign = _lu(a)
    if sign == 0.0:
        return _np.float64(0.0)
    # As numpy does: the sign, times exp of the summed logs of the pivots.
    logdet = 0.0
    for i in range(len(m)):
        u = m[i][i]
        if u < 0:
            sign = -sign
            u = -u
        logdet += _math.log(u)
    return _np.float64(sign * _math.exp(logdet))


def _solve_lu(m, perm, b):
    n = len(m)
    y = [b[perm[i]] for i in range(n)]
    for i in range(n):
        for j in range(i):
            y[i] -= m[i][j] * y[j]
    for i in range(n - 1, -1, -1):
        for j in range(i + 1, n):
            y[i] -= m[i][j] * y[j]
        y[i] /= m[i][i]
    return y


def solve(a, b):
    m, perm, sign = _lu(a)
    if sign == 0.0:
        raise LinAlgError("Singular matrix")
    b = _np.asarray(b).astype(float)
    if b.ndim == 1:
        return _np.array(_solve_lu(m, perm, b.tolist()))
    cols = [_solve_lu(m, perm, b[:, j].tolist()) for j in range(b.shape[1])]
    return _np.array(cols).T.copy()


def inv(a):
    n = _np.asarray(a).shape[0]
    return solve(a, _np.eye(n))


def matrix_power(a, n):
    a = _np.asarray(a)
    r = _np.eye(a.shape[0], dtype=a.dtype)
    if n < 0:
        a, n = inv(a), -n
    while n:
        if n & 1:
            r = r @ a
        a = a @ a
        n >>= 1
    return r
