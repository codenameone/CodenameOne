# pandas: the truncated repr of long and wide frames and series with the
# default display options of a non-interactive run (max_rows 60, min_rows 10,
# max_columns 20), including the "[N rows x M columns]" footer.
import pandas as pd

print(pd.Series(range(100)))
print(pd.Series([i * 1.5 for i in range(61)], name="half"))
print(pd.Series(range(60)).tail(3))
print(pd.Series(["s%d" % i for i in range(70)], index=["k%d" % i for i in range(70)]))
long = pd.DataFrame({"a": range(200), "b": [i * 0.25 for i in range(200)], "c": ["x%d" % i for i in range(200)]})
print(long)
print(long.head(60))
print(pd.DataFrame({"n": range(61)}))
wide = pd.DataFrame({"c%d" % i: [i, i * 2, i * 3] for i in range(25)})
print(wide)
both = pd.DataFrame({"col%d" % i: [j * i for j in range(80)] for i in range(30)})
print(both)
print(pd.DataFrame({"v%d" % i: [i] for i in range(20)}))
