# gc: the calls programs make around a memory measurement. How many objects a
# collection found and the generation counts are the collector's business, so
# only their shapes are printed.
import gc

data = [str(i) * 3 for i in range(10000)]
del data
n = gc.collect()
print(type(n).__name__, n >= 0)
print(gc.isenabled())
gc.disable()
print(gc.isenabled())
gc.enable()
print(gc.isenabled())
c = gc.get_count()
print(type(c).__name__, len(c), all(isinstance(x, int) for x in c))
t = gc.get_threshold()
print(type(t).__name__, len(t))
print(gc.garbage, type(gc.callbacks).__name__)
