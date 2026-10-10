/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
using System;
using System.Collections.Generic;
using UnityEngine;

// A board read from a text file under Resources into an array of two
// dimensions, the way a tile game loads a level: the file is found by name
// when the scene starts, split into rows and cells and parsed. Each marked
// cell becomes an object made in code, and where each one stands is kept in a
// dictionary keyed by the object.
public class Board : MonoBehaviour
{
    public string levelName;
    public TextAsset table;
    public Sprite wedge;
    public KeyCode[] keys;
    public int[] weights;

    private int[,] cells;
    private readonly Dictionary<GameObject, Vector2> placed = new Dictionary<GameObject, Vector2>();

    private void Start()
    {
        TextAsset level = Resources.Load<TextAsset>(levelName);
        string[] lines = level.text.Split(new[] { '\r', '\n' }, StringSplitOptions.RemoveEmptyEntries);
        int rows = lines.Length;
        int columns = lines[0].Split(new[] { ',' }).Length;
        cells = new int[rows, columns];
        int unreadable = 0;
        for (int i = 0; i < rows; i++)
        {
            string[] row = lines[i].Split(new[] { ',' });
            for (int j = 0; j < columns; j++)
            {
                int value;
                if (int.TryParse(row[j], out value))
                {
                    cells[i, j] = value;
                }
                else
                {
                    cells[i, j] = -1;
                    unreadable++;
                }
            }
        }
        Debug.Log("board " + level.name + " " + cells.GetLength(0) + "x" + cells.GetLength(1) + " of " + cells.Length
            + " unreadable=" + unreadable + " corner=" + cells[rows - 1, columns - 1] + " chars=" + level.text.Length);

        Sprite dot = Resources.Load<Sprite>("Icons/dot");
        for (int i = 0; i < rows; i++)
        {
            for (int j = 0; j < columns; j++)
            {
                if (cells[i, j] != 2)
                {
                    continue;
                }
                GameObject marker = new GameObject("marker" + i + "_" + j);
                marker.transform.position = new Vector2(j * 0.75f - 5.5f, 4.25f - i * 0.75f);
                SpriteRenderer renderer = marker.AddComponent<SpriteRenderer>();
                renderer.sprite = i == 0 ? dot : wedge;
                renderer.color = i == 0 ? Color.red : Color.black;
                renderer.sortingOrder = 2;
                placed.Add(marker, new Vector2(i, j));
            }
        }
        Vector2 where;
        string walked = "";
        GameObject last = null;
        foreach (KeyValuePair<GameObject, Vector2> pair in placed)
        {
            walked += pair.Key.name + "@" + (int)pair.Value.x + "," + (int)pair.Value.y + " ";
            last = pair.Key;
        }
        placed[last] = new Vector2(9, 9);
        bool found = placed.TryGetValue(last, out where);
        Debug.Log("placed " + walked + found + " " + (int)where.x + (int)where.y + " " + placed.Count + " dot="
            + dot.name + " wedge=" + wedge.name);

        // The same asset however it is asked for; a path is not a file name
        // and letter case does not matter, and the type asked for does.
        TextAsset again = Resources.Load("levels/BOARD") as TextAsset;
        UnityEngine.Object typed = Resources.Load("Levels/board", typeof(TextAsset));
        byte[] blob = Resources.Load<TextAsset>("Levels/blob").bytes;
        int sum = 0;
        foreach (byte b in blob)
        {
            sum += b;
        }
        Debug.Log("resources same=" + (again == level) + (typed == level) + " missing="
            + (Resources.Load("Levels/nothing") == null) + (Resources.Load<Sprite>("Levels/board") == null)
            + (Resources.Load("Levels/board.txt") == null) + (Resources.Load<TextAsset>("Levels/notes") == null)
            + " blob=" + blob.Length + "/" + sum + "/" + blob[9] + " bytes=" + level.bytes.Length);
        string[] header = table.text.Split('\n')[0].Split(',');
        Debug.Log("table " + table.name + " " + header[0] + "+" + header[1] + " keys=" + keys.Length + ":" + keys[0] + ","
            + keys[1] + " weights=" + weights[0] + "," + weights[1] + "," + weights[2]);
    }
}
