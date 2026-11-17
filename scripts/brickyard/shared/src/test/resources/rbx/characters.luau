local Players = game:GetService("Players")
local floor = Instance.new("Part")
floor.Anchored = true
floor.Size = Vector3.new(200, 1, 200)
floor.Position = Vector3.new(0, 0, 0)
floor.Parent = workspace
local spawn = Instance.new("SpawnLocation")
spawn.Anchored = true
spawn.Size = Vector3.new(6, 1, 6)
spawn.Position = Vector3.new(0, 1, 0)
spawn.Parent = workspace
local plr = Players:GetPlayers()[1]
plr:LoadCharacter()
local char = plr.Character
assert(char and char:FindFirstChild("HumanoidRootPart"), "rig built")
local hum = char:FindFirstChildOfClass("Humanoid")
task.wait(0.5)
local root = char.HumanoidRootPart
assert(math.abs(root.Position.Y - 4.5) < 0.6, "standing on spawn " .. tostring(root.Position))
hum:MoveTo(Vector3.new(20, 0, 0))
local reached = hum.MoveToFinished:Wait()
assert(reached and math.abs(root.Position.X - 20) < 1.5, "MoveTo " .. tostring(root.Position))
local lava = Instance.new("Part")
lava.Anchored = true
lava.Size = Vector3.new(4, 1, 4)
lava.Position = Vector3.new(30, 1, 0)
lava.Parent = workspace
lava.Touched:Connect(function(hit)
    local h = hit.Parent:FindFirstChildOfClass("Humanoid")
    if h then h.Health = 0 end
end)
local died = false
hum.Died:Connect(function() died = true end)
hum:MoveTo(Vector3.new(30, 0, 0))
task.wait(2)
assert(died, "lava kills")
local respawned = plr.CharacterAdded:Wait()
assert(respawned ~= char, "respawn")
local path = game:GetService("PathfindingService"):CreatePath()
path:ComputeAsync(Vector3.new(0, 1, 0), Vector3.new(40, 1, 40))
assert(path.Status == Enum.PathStatus.Success and #path:GetWaypoints() > 2, "path " .. tostring(path.Status))
print("DONE")
