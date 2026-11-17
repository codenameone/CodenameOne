local Players = game:GetService("Players")
local DataStoreService = game:GetService("DataStoreService")
local store = DataStoreService:GetDataStore("Coins")
store:SetAsync("u1", {coins = 10})
assert(store:GetAsync("u1").coins == 10, "datastore")
store:UpdateAsync("u1", function(old) old.coins += 5; return old end)
assert(store:GetAsync("u1").coins == 15, "update")
assert(store:IncrementAsync("n", 3) == 3, "increment")
local ordered = DataStoreService:GetOrderedDataStore("Wins")
ordered:SetAsync("a", 5); ordered:SetAsync("b", 9); ordered:SetAsync("c", 1)
local page = ordered:GetSortedAsync(false, 2):GetCurrentPage()
assert(page[1].key == "b" and page[2].key == "a", "ordered")
local ms = game:GetService("MarketplaceService")
local granted = false
ms.ProcessReceipt = function(info)
    granted = info.ProductId == 77
    return Enum.ProductPurchaseDecision.PurchaseGranted
end
local plr = Players:GetPlayers()[1]
assert(plr, "a player is present")
local stats = Instance.new("Folder"); stats.Name = "leaderstats"; stats.Parent = plr
local coins = Instance.new("IntValue"); coins.Name = "Coins"; coins.Value = 3.7; coins.Parent = stats
assert(coins.Value == 3, "IntValue floors")
ms:PromptProductPurchase(plr, 77)
task.wait(0.1)
assert(granted, "ProcessReceipt")
local remote = Instance.new("RemoteEvent")
remote.Parent = game.ReplicatedStorage
local from
remote.OnServerEvent:Connect(function(p, x) from = {p, x} end)
remote:FireServer(12)
assert(from[1] == plr and from[2] == 12, "solo FireServer")
local rf = Instance.new("RemoteFunction")
rf.OnServerInvoke = function(p, a, b) task.wait(0.05); return a + b end
assert(rf:InvokeServer(2, 3) == 5, "InvokeServer yields")
local ps = game:GetService("PhysicsService")
ps:RegisterCollisionGroup("Ghosts")
ps:CollisionGroupSetCollidable("Ghosts", "Default", false)
assert(not ps:CollisionGroupsAreCollidable("Ghosts", "Default"), "collision groups")
game.Lighting.ClockTime = 6
assert(game.Lighting.TimeOfDay == "06:00:00", game.Lighting.TimeOfDay)
assert(game.Lighting:GetSunDirection().Y < 0.2, "sunrise is low")
local got
game:GetService("MessagingService"):SubscribeAsync("news", function(m) got = m.Data end)
game:GetService("MessagingService"):PublishAsync("news", "hi")
task.wait(0.05)
assert(got == "hi", "messaging loopback")
local guid = game:GetService("HttpService"):GenerateGUID(false)
assert(#guid == 36, "guid")
game:GetService("BrickyardService"):SetLeaderstat(plr, "Wins", 4)
local top = game:GetService("BrickyardService"):GetLeaderboardAsync("Wins")
assert(top[1].Value == 4, "platform leaderboard")
print("DONE")
