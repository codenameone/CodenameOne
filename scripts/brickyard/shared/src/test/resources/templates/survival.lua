-- Short rounds, and a flood every time, so one round fits the test.
workspace:SetAttribute("Intermission", 1)
workspace:SetAttribute("RoundLength", 4)
workspace:SetAttribute("ForceDisaster", "Flood")
local status = game:GetService("ReplicatedStorage"):WaitForChild("RoundStatus")
local seen = {}
status.Changed:Connect(function() seen[#seen + 1] = status.Value end)
local seaY = workspace.Sea.Position.Y
while not string.find(status.Value, "Disaster") do task.wait(0.1) end
check(root.Position.Y < 20, "dropped on the island")
-- Climb the lighthouse, the one place a flood cannot reach.
go(workspace.LighthouseDeck, 3)
task.wait(2.5)
check(workspace.Sea.Position.Y > seaY + 5, "the sea rises")
while not string.find(status.Value, "survived") do task.wait(0.1) end
check(ls.Survivals.Value == 1 and ls.Coins.Value == 10, "survival counted " .. ls.Survivals.Value)
check(c.Humanoid.Health > 0 and root.Position.Y > 35, "back in the lobby")
check(math.abs(workspace.Sea.Position.Y - seaY) < 0.01, "the sea goes back down")
local label = p.PlayerGui.RoundGui.Status
check(label.Text == status.Value, "status shown on screen")
-- Next round, stay on the beach: the flood drowns whoever is low.
while not string.find(status.Value, "Disaster") do task.wait(0.1) end
local died = false
c.Humanoid.Died:Connect(function() died = true end)
task.wait(3)
check(died, "the flood drowns the low ground")
while not string.find(status.Value, "survived") do task.wait(0.1) end
check(ls.Survivals.Value == 1, "no survival for the drowned")
print("DONE")
