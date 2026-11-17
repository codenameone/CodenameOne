go(workspace.Checkpoint3)
check(ls.Stage.Value == 3, "checkpoint stage " .. ls.Stage.Value)
-- Fall off the course: the ground kills, and the new character starts on
-- the last checkpoint rather than in the lobby.
local died = false
c.Humanoid.Died:Connect(function() died = true end)
root.CFrame = CFrame.new(40, 3, -150)
task.wait(4)
check(died, "falling to the ground kills")
check(p.Character ~= c, "respawns after the fall")
local at = p.Character:WaitForChild("HumanoidRootPart").Position
check((at - workspace.Checkpoint3.Position).Magnitude < 6, "respawns on the checkpoint, not at " .. tostring(at))
c = p.Character
root = c.HumanoidRootPart
go(workspace.Finish, 2)
check(ls.Wins.Value == 1, "finish win")
print("DONE")
