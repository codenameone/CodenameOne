from holder import Holder, Other
from poke import poke, poke_other

h = Holder(a=1)
print(h.bump(), h.describe())
poke(h)
print(repr(h.count), repr(h.name))
try:
    h.bump()
except TypeError as e:
    print("TypeError", type(h.count).__name__)
try:
    print(h.describe())
except TypeError:
    print("TypeError describe")
o = Other()
poke_other(o)
print(o.count, Holder().describe())
