from typing import List, Optional

from pydantic import ValidationError
from pydantic.dataclasses import dataclass


@dataclass
class Pt:
    x: int
    y: float = 0.0
    tags: Optional[List[str]] = None


p = Pt("3", "1.5", ["a"])
print(p, p.x, p.y)
print(Pt(x=1) == Pt(1, 0.0))
try:
    Pt("z", [])
except ValidationError as e:
    print([(d["type"], d["loc"], d["msg"]) for d in e.errors()])
