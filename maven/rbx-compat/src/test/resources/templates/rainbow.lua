go(workspace.Checkpoint3)
check(ls.Stage.Value == 3, "checkpoint stage " .. ls.Stage.Value)
go(workspace.Finish, 2)
check(ls.Wins.Value == 1, "finish win")
-- Walk off the lobby's edge: the fall to the baseplate kills, and the
-- player comes back.
local died = false
c.Humanoid.Died:Connect(function() died = true end)
root.CFrame = CFrame.new(0, 3, -9)
task.wait(4)
check(died, "falling to the ground kills")
check(p.Character ~= c, "respawns after the fall")
print("DONE")
