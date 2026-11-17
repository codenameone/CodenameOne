local Players = game:GetService("Players")
local p = Players:GetPlayers()[1]
local c = p.Character or p.CharacterAdded:Wait()
local root = c:WaitForChild("HumanoidRootPart")
local ls = p:WaitForChild("leaderstats")
local function go(part, dy)
    root.CFrame = CFrame.new(part.Position + Vector3.new(0, dy or 0, 0))
    task.wait(0.3)
end
local function check(cond, what)
    if not cond then error("FAILED " .. what) end
end
