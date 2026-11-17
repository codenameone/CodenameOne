for _, i in ipairs({2, 3, 4, 1}) do
    go(workspace["Gate" .. i])
end
check(ls.Laps.Value == 1, "lap " .. ls.Laps.Value)
check(ls.Best.Value > 0, "best time")
print("DONE")
