local gem = workspace.Gems:GetChildren()[1]
go(gem)
-- The field is dense enough that landing on one gem can brush a neighbour.
local got = ls.Gems.Value
check(got >= 1, "collect gem " .. got)
go(workspace.SellPad)
check(ls.Coins.Value == 2 * got and ls.Gems.Value == 0, "sell")
print("DONE")
