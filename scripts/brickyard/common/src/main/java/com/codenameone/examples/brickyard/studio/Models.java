/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.brickyard.studio;

import com.codename1.ui.FontImage;

/// The toolbox's ready-made models and the script editor's snippets.
///
/// Each model is plain Luau, run through the same `run_luau` tool an AI
/// agent uses, so what a creator inserts is also a lesson in building with
/// `Instance.new`: Output shows the code, and the parts it made are
/// ordinary parts the creator can move and edit.
public final class Models {
    /// Toolbox entries as {menu label, Luau body}. The body sees `at`, the
    /// Vector3 the model is placed on, and `folder`, its new Model.
    private static final String[][] MODELS = {
        {"Tree", ""
            + "local trunk = part(folder, \"Trunk\", Vector3.new(8, 1.6, 1.6), at + Vector3.new(0, 4, 0))\n"
            + "trunk.Shape = Enum.PartType.Cylinder\n"
            + "trunk.CFrame = CFrame.new(trunk.Position) * CFrame.Angles(0, 0, math.rad(90))\n"
            + "trunk.Color = Color3.fromRGB(120, 78, 44)\n"
            + "trunk.Material = Enum.Material.Wood\n"
            + "for i, offset in { Vector3.new(0, 9, 0), Vector3.new(1.8, 7.6, 0.8), Vector3.new(-1.6, 7.8, -1) } do\n"
            + "    local leaves = part(folder, \"Leaves\", Vector3.new(6 - i, 6 - i, 6 - i), at + offset)\n"
            + "    leaves.Shape = Enum.PartType.Ball\n"
            + "    leaves.Color = Color3.fromRGB(64, 160 + i * 12, 72)\n"
            + "    leaves.Material = Enum.Material.Grass\n"
            + "end\n"},
        {"Torch", ""
            + "local post = part(folder, \"Post\", Vector3.new(0.8, 6, 0.8), at + Vector3.new(0, 3, 0))\n"
            + "post.Color = Color3.fromRGB(92, 64, 40)\n"
            + "post.Material = Enum.Material.Wood\n"
            + "local bowl = part(folder, \"Bowl\", Vector3.new(1.6, 0.8, 1.6), at + Vector3.new(0, 6.4, 0))\n"
            + "bowl.Color = Color3.fromRGB(60, 60, 66)\n"
            + "bowl.Material = Enum.Material.Metal\n"
            + "local flame = Instance.new(\"Fire\")\n"
            + "flame.Size = 4\n"
            + "flame.Heat = 9\n"
            + "flame.Parent = bowl\n"
            + "local glow = Instance.new(\"PointLight\")\n"
            + "glow.Color = Color3.fromRGB(255, 170, 80)\n"
            + "glow.Range = 18\n"
            + "glow.Parent = bowl\n"},
        {"Coin", ""
            + "local coin = part(folder, \"Coin\", Vector3.new(0.4, 3, 3), at + Vector3.new(0, 2.5, 0))\n"
            + "coin.Shape = Enum.PartType.Cylinder\n"
            + "coin.Color = Color3.fromRGB(255, 200, 40)\n"
            + "coin.Material = Enum.Material.Neon\n"
            + "coin.CanCollide = false\n"
            + "-- A cylinder's axis is X: turn it to face the player.\n"
            + "coin.CFrame = coin.CFrame * CFrame.Angles(0, math.rad(90), 0)\n"
            + "Instance.new(\"Sparkles\").Parent = coin\n"
            + "-- Touch it to collect it: the coin hides, and comes back later.\n"
            + "local pickup = Instance.new(\"Script\")\n"
            + "pickup.Name = \"Pickup\"\n"
            + "pickup.Source = [[\n"
            + "local coin = script.Parent:FindFirstChild(\"Coin\")\n"
            + "local taken = false\n"
            + "task.spawn(function()\n"
            + "    while coin.Parent do\n"
            + "        coin.CFrame = coin.CFrame * CFrame.Angles(0, math.rad(3), 0)\n"
            + "        task.wait()\n"
            + "    end\n"
            + "end)\n"
            + "coin.Touched:Connect(function(hit)\n"
            + "    local player = game.Players:GetPlayerFromCharacter(hit.Parent)\n"
            + "    if player and not taken then\n"
            + "        taken = true\n"
            + "        coin.Transparency = 1\n"
            + "        local stats = player:FindFirstChild(\"leaderstats\")\n"
            + "        local coins = stats and stats:FindFirstChild(\"Coins\")\n"
            + "        if coins then coins.Value += 1 end\n"
            + "        task.wait(5)\n"
            + "        coin.Transparency = 0\n"
            + "        taken = false\n"
            + "    end\n"
            + "end)\n"
            + "]]\n"
            + "pickup.Parent = folder\n"},
        {"Lamp post", ""
            + "local pole = part(folder, \"Pole\", Vector3.new(0.6, 10, 0.6), at + Vector3.new(0, 5, 0))\n"
            + "pole.Color = Color3.fromRGB(40, 44, 56)\n"
            + "pole.Material = Enum.Material.Metal\n"
            + "local bulb = part(folder, \"Bulb\", Vector3.new(1.8, 1.8, 1.8), at + Vector3.new(0, 10.6, 0))\n"
            + "bulb.Shape = Enum.PartType.Ball\n"
            + "bulb.Color = Color3.fromRGB(255, 236, 180)\n"
            + "bulb.Material = Enum.Material.Neon\n"
            + "local light = Instance.new(\"PointLight\")\n"
            + "light.Range = 24\n"
            + "light.Parent = bulb\n"},
        {"Jump pad", ""
            + "local pad = part(folder, \"Pad\", Vector3.new(6, 0.6, 6), at + Vector3.new(0, 0.3, 0))\n"
            + "pad.Color = Color3.fromRGB(40, 220, 255)\n"
            + "pad.Material = Enum.Material.Neon\n"
            + "local boost = Instance.new(\"Script\")\n"
            + "boost.Name = \"Boost\"\n"
            + "boost.Source = [[\n"
            + "local pad = script.Parent:FindFirstChild(\"Pad\")\n"
            + "pad.Touched:Connect(function(hit)\n"
            + "    local root = hit.Parent:FindFirstChild(\"HumanoidRootPart\")\n"
            + "    if root then\n"
            + "        root.AssemblyLinearVelocity = Vector3.new(0, 90, 0)\n"
            + "    end\n"
            + "end)\n"
            + "]]\n"
            + "boost.Parent = folder\n"},
        {"Spinner", ""
            + "local hub = part(folder, \"Hub\", Vector3.new(2, 2, 2), at + Vector3.new(0, 1, 0))\n"
            + "hub.Color = Color3.fromRGB(80, 80, 96)\n"
            + "local arm = part(folder, \"Arm\", Vector3.new(16, 1, 1), at + Vector3.new(0, 1.5, 0))\n"
            + "arm.Color = Color3.fromRGB(255, 70, 90)\n"
            + "arm.Material = Enum.Material.Neon\n"
            + "local spin = Instance.new(\"Script\")\n"
            + "spin.Name = \"Spin\"\n"
            + "spin.Source = [[\n"
            + "local arm = script.Parent:FindFirstChild(\"Arm\")\n"
            + "local RunService = game:GetService(\"RunService\")\n"
            + "RunService.Heartbeat:Connect(function(dt)\n"
            + "    arm.CFrame = arm.CFrame * CFrame.Angles(0, dt * 2, 0)\n"
            + "end)\n"
            + "]]\n"
            + "spin.Parent = folder\n"},
        {"Water pool", ""
            + "local rim = part(folder, \"Rim\", Vector3.new(20, 1, 20), at + Vector3.new(0, 0.5, 0))\n"
            + "rim.Color = Color3.fromRGB(200, 196, 184)\n"
            + "rim.Material = Enum.Material.Concrete\n"
            + "local water = part(folder, \"Water\", Vector3.new(18, 0.4, 18), at + Vector3.new(0, 1.1, 0))\n"
            + "water.Color = Color3.fromRGB(40, 140, 220)\n"
            + "water.Material = Enum.Material.Water\n"
            + "water.Transparency = 0.25\n"
            + "water.CanCollide = false\n"},
        {"Checkpoint", ""
            + "local pad = Instance.new(\"SpawnLocation\")\n"
            + "pad.Name = \"Checkpoint\"\n"
            + "pad.Size = Vector3.new(8, 1, 8)\n"
            + "pad.Position = at + Vector3.new(0, 0.5, 0)\n"
            + "pad.Anchored = true\n"
            + "pad.Color = Color3.fromRGB(90, 220, 120)\n"
            + "pad.Material = Enum.Material.Neon\n"
            + "pad.Parent = folder\n"},
    };

    /// Script editor snippets as {menu label, Luau}. Each is a complete
    /// script that runs as written, so it is the shortest route to seeing
    /// the API work.
    private static final String[][] SNIPPETS = {
        {"Kill brick", ""
            + "-- Put this script inside a part: touching the part resets the player.\n"
            + "local brick = script.Parent\n"
            + "brick.Touched:Connect(function(hit)\n"
            + "    local humanoid = hit.Parent:FindFirstChild(\"Humanoid\")\n"
            + "    if humanoid then\n"
            + "        humanoid.Health = 0\n"
            + "    end\n"
            + "end)\n"},
        {"Leaderstats", ""
            + "-- Every player gets a Coins counter on the player list.\n"
            + "game.Players.PlayerAdded:Connect(function(player)\n"
            + "    local stats = Instance.new(\"Folder\")\n"
            + "    stats.Name = \"leaderstats\"\n"
            + "    stats.Parent = player\n"
            + "    local coins = Instance.new(\"IntValue\")\n"
            + "    coins.Name = \"Coins\"\n"
            + "    coins.Value = 0\n"
            + "    coins.Parent = stats\n"
            + "end)\n"},
        {"Goal and win", ""
            + "-- The game's goal, on every player's screen: collect 10 coins to win.\n"
            + "local GOAL = 10\n"
            + "game.Players.PlayerAdded:Connect(function(player)\n"
            + "    local gui = Instance.new(\"ScreenGui\")\n"
            + "    gui.ResetOnSpawn = false -- keep it when the character respawns\n"
            + "    gui.Parent = player:WaitForChild(\"PlayerGui\")\n"
            + "    local label = Instance.new(\"TextLabel\")\n"
            + "    label.Size = UDim2.new(0, 360, 0, 44)\n"
            + "    label.Position = UDim2.new(0.5, -180, 0, 12)\n"
            + "    label.BackgroundColor3 = Color3.fromRGB(22, 27, 46)\n"
            + "    label.TextColor3 = Color3.fromRGB(255, 196, 60)\n"
            + "    label.TextSize = 22\n"
            + "    label.Text = \"Goal: collect \" .. GOAL .. \" coins\"\n"
            + "    label.Parent = gui\n"
            + "    -- Waits for the Leaderstats script's counter; the goal shows meanwhile.\n"
            + "    local coins = player:WaitForChild(\"leaderstats\"):WaitForChild(\"Coins\")\n"
            + "    local function update()\n"
            + "        if coins.Value >= GOAL then\n"
            + "            label.Text = \"You win! \" .. coins.Value .. \" coins\"\n"
            + "        else\n"
            + "            label.Text = \"Goal: collect \" .. GOAL .. \" coins (\" .. coins.Value .. \"/\" .. GOAL .. \")\"\n"
            + "        end\n"
            + "    end\n"
            + "    coins.Changed:Connect(update)\n"
            + "    update()\n"
            + "end)\n"},
        {"Day and night", ""
            + "-- A full day every two minutes.\n"
            + "local Lighting = game:GetService(\"Lighting\")\n"
            + "while true do\n"
            + "    Lighting.ClockTime = (Lighting.ClockTime + 0.2) % 24\n"
            + "    task.wait(1)\n"
            + "end\n"},
        {"Moving platform", ""
            + "-- Slides the part back and forth forever.\n"
            + "local TweenService = game:GetService(\"TweenService\")\n"
            + "local platform = script.Parent\n"
            + "local info = TweenInfo.new(3, Enum.EasingStyle.Sine, Enum.EasingDirection.InOut, -1, true)\n"
            + "TweenService:Create(platform, info, { Position = platform.Position + Vector3.new(0, 0, 20) }):Play()\n"},
        {"Welcome message", ""
            + "-- Greets every player who joins.\n"
            + "game.Players.PlayerAdded:Connect(function(player)\n"
            + "    print(\"Welcome to the game, \" .. player.Name .. \"!\")\n"
            + "end)\n"},
        {"Countdown", ""
            + "-- Counts down, then opens the gate.\n"
            + "local gate = workspace:FindFirstChild(\"Gate\")\n"
            + "for i = 5, 1, -1 do\n"
            + "    print(\"Starting in \" .. i)\n"
            + "    task.wait(1)\n"
            + "end\n"
            + "if gate then\n"
            + "    gate.Transparency = 1\n"
            + "    gate.CanCollide = false\n"
            + "end\n"
            + "print(\"Go!\")\n"},
    };

    private Models() {
    }

    private static final char[] MODEL_ICONS = {FontImage.MATERIAL_PARK, FontImage.MATERIAL_LOCAL_FIRE_DEPARTMENT,
        FontImage.MATERIAL_MONETIZATION_ON, FontImage.MATERIAL_LIGHT, FontImage.MATERIAL_ARROW_UPWARD,
        FontImage.MATERIAL_CACHED, FontImage.MATERIAL_POOL, FontImage.MATERIAL_FLAG};

    /// Model `i`'s toolbox icon, a material glyph.
    public static char icon(int i) {
        return i < MODEL_ICONS.length ? MODEL_ICONS[i] : FontImage.MATERIAL_WIDGETS;
    }

    /// How many toolbox models there are.
    public static int count() {
        return MODELS.length;
    }

    /// Model `i`'s menu label.
    public static String name(int i) {
        return MODELS[i][0];
    }

    /// Model `i` as one Luau chunk that builds it at (x, y, z) in a Model
    /// named after it, under Workspace.
    public static String luau(int i, double x, double y, double z) {
        return "-- Toolbox: " + MODELS[i][0] + "\n"
                + "local function part(parent, name, size, position)\n"
                + "    local p = Instance.new(\"Part\")\n"
                + "    p.Name = name\n"
                + "    p.Size = size\n"
                + "    p.Position = position\n"
                + "    p.Anchored = true\n"
                + "    p.Parent = parent\n"
                + "    return p\n"
                + "end\n"
                + "local at = Vector3.new(" + x + ", " + y + ", " + z + ")\n"
                + "local folder = Instance.new(\"Model\")\n"
                + "folder.Name = \"" + pascal(MODELS[i][0]) + "\"\n"
                + "folder.Parent = workspace\n"
                + MODELS[i][1];
    }

    /// How many snippets there are.
    public static int snippetCount() {
        return SNIPPETS.length;
    }

    /// Snippet `i`'s menu label.
    public static String snippetName(int i) {
        return SNIPPETS[i][0];
    }

    /// Snippet `i`'s source.
    public static String snippet(int i) {
        return SNIPPETS[i][1];
    }

    /// "Lamp post" as an instance name: "LampPost".
    static String pascal(String words) {
        StringBuilder b = new StringBuilder();
        boolean up = true;
        for (int i = 0; i < words.length(); i++) {
            char c = words.charAt(i);
            if (c == ' ') {
                up = true;
            } else {
                b.append(up && c >= 'a' && c <= 'z' ? (char) (c - 32) : c);
                up = false;
            }
        }
        return b.toString();
    }
}
