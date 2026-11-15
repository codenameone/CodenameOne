local gem = workspace.Gems:GetChildren()[1]
go(gem)
check(ls.Gems.Value == 1, "collect gem " .. ls.Gems.Value)
go(workspace.SellPad)
check(ls.Coins.Value == 2 and ls.Gems.Value == 0, "sell")
print("DONE")
