import shapes
from collections import namedtuple
print(shapes.Point.__module__)
print(shapes.Pair.__module__)
print(shapes.Color.__module__)
print(shapes.Box.__module__)
print(shapes.Pt3.__module__)
print(repr(shapes.Point(1, 2)))
Local = namedtuple('Local', 'a')
print(Local.__module__)
import pickle
print(pickle.loads(pickle.dumps(shapes.Point(3, 4))))
