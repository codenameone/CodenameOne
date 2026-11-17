task.wait(8)
check(ls.Cash.Value > 0, "drops reach the collector, cash " .. ls.Cash.Value)
print("cash", ls.Cash.Value)
print("DONE")
