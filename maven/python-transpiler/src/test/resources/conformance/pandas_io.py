import io
import os
import pandas as pd

text = "id,name,price,qty\n1,pen,1.5,10\n2,book,12,\n3,lamp,30.25,2\n"
df = pd.read_csv(io.StringIO(text))
print(df)
print(df.dtypes)
print(df["qty"].isna().tolist())
print(df.fillna(0))
print(df.dropna())
print(df.to_csv(index=False))
print(df.to_csv())
print(df.to_json(orient="records"))
back = pd.read_json(io.StringIO('[{"a": 1, "b": "x"}, {"a": 2, "b": "y"}]'))
print(back)
print(back.dtypes)
path = "cn1_pandas_io_test.csv"
df.to_csv(path, index=False)
again = pd.read_csv(path)
print(again.equals(df), again.shape)
os.remove(path)
print(df["price"] * df["qty"])
print((df["price"] * df["qty"]).sum())
