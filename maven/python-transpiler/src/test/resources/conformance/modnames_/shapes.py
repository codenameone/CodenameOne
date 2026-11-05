from collections import namedtuple
from typing import NamedTuple
import enum, dataclasses
Point = namedtuple('Point', 'x y')
Pair = NamedTuple('Pair', [('a', int), ('b', int)])
Color = enum.Enum('Color', 'RED GREEN')
Box = dataclasses.make_dataclass('Box', ['w', 'h'])
class Pt3(NamedTuple):
    x: int
    y: int
    z: int
