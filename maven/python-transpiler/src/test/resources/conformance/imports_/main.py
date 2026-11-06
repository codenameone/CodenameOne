print("main", __name__)
import pkg
print(pkg.__name__, pkg.VALUE, pkg.helper(2))
import pkg.sub.b
print(pkg.sub.b.__name__, pkg.sub.b.combined())
import pkg.sub.b as bb
print(bb is pkg.sub.b, bb.NAME)
from pkg import a, VALUE as V
print(a.twice(V), a.__name__)
from pkg.sub import b
print(b.combined())
from pkg.star import *
print(public_one, public_two)
try:
    print(_hidden)
except NameError as e:
    print("hidden not exported")
from pkg.allstar import *
print(listed)
try:
    print(unlisted)
except NameError:
    print("unlisted not exported")
import pkg.a
print(pkg.a.counter, pkg.a is a)
try:
    import pkg.missing
except ImportError as e:
    print(type(e).__name__)
try:
    from pkg import nothing_here
except ImportError as e:
    print("ImportError from")
print(sorted(k for k in dir(pkg) if not k.startswith("__")))
