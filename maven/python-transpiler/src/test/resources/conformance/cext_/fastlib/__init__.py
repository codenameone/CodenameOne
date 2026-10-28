from ._core import fast_sum, scale, VERSION

try:
    from ._speedups import escape
except ImportError:
    def escape(s):
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")


def total(xs):
    return fast_sum(xs) * 10


def scaled(xs, k):
    return list(scale(xs, k))


def info(d):
    from . import _core
    return _core.describe(d)
