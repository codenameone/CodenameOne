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
using Cinemachine;
using TMPro;
using UnityEngine;
using UnityEngine.Tilemaps;

// Runs along a tilemap floor, jumps, runs through a trigger and into a wall
// that it built itself: an Animator driven by a parameter of each kind, with
// animation events; a Tilemap read and changed from a script; a composite
// collider whose seams must not catch a sliding box; a particle system played
// on landing; TextMesh Pro text; and a Cinemachine camera that follows.
public class Hero : MonoBehaviour
{
    public float speed = 4f;
    public float jumpSpeed = 9f;
    public ParticleSystem dust;
    public Tilemap ground;
    public TileBase block;
    public TMP_Text steps;
    public TextMeshPro sign;
    public CinemachineVirtualCamera follow;
    public Transform hat;

    private static readonly string[] States = { "Idle", "Run", "Jump", "Hurt", "Proud" };

    private Rigidbody2D body;
    private Animator animator;
    private SpriteRenderer shown;
    private float move;
    private int touching;
    private int stepCount;
    private int jumps;
    private string state = "none";
    private int slowest = 100000;
    private bool ran;
    private bool blocked;
    private int dustFrame = -100;
    private int hurtFrames;

    private void Start()
    {
        body = GetComponent<Rigidbody2D>();
        animator = GetComponent<Animator>();
        shown = GetComponent<SpriteRenderer>();
        Vector3Int under = ground.WorldToCell(transform.position + Vector3.down);
        Vector3Int wall = new Vector3Int(8, -2, 0);
        Vector3Int post = new Vector3Int(-9, -1, 0);
        Debug.Log("hero over cell " + under.x + "," + under.y + " tile="
            + (ground.HasTile(under) ? ground.GetTile(under).name : "none") + " wall=" + ground.HasTile(wall)
            + " post=" + ground.HasTile(post));
        ground.SetTile(wall, block);
        ground.SetTile(post, null);
        Vector3 at = ground.GetCellCenterWorld(wall);
        Vector3 corner = ground.CellToWorld(wall);
        Debug.Log("wall=" + ground.HasTile(wall) + " centre " + Cm(at.x) + "," + Cm(at.y) + " corner " + Cm(corner.x)
            + "," + Cm(corner.y) + " post=" + ground.HasTile(post) + " sign=" + sign.text + " follows="
            + follow.Follow.name);
    }

    private void Update()
    {
        move = Input.GetAxisRaw("Horizontal");
        bool grounded = touching > 0;
        if (Input.GetKeyDown(KeyCode.Space) && grounded)
        {
            body.velocity = new Vector2(body.velocity.x, jumpSpeed);
            jumps++;
            Debug.Log("frame " + Time.frameCount + " jump " + jumps + " from " + Cm(transform.position.x));
        }
        animator.SetFloat("speed", Mathf.Abs(move));
        animator.SetBool("grounded", grounded);
        animator.SetInteger("jumps", jumps);
        AnimatorStateInfo info = animator.GetCurrentAnimatorStateInfo(0);
        string now = "other";
        for (int i = 0; i < States.Length; i++)
        {
            if (info.IsName(States[i]))
            {
                now = States[i];
            }
        }
        if (now != state)
        {
            Debug.Log("frame " + Time.frameCount + " state " + state + " -> " + now + " sprite=" + shown.sprite.name);
            state = now;
            hurtFrames = 0;
        }
        if (state == "Hurt")
        {
            // The clip's curves: the tint dips to red and the hat, a child, is squashed; then both come back.
            hurtFrames++;
            if (hurtFrames % 6 == 0)
            {
                Debug.Log("hurt " + hurtFrames + " green=" + Cm(shown.color.g) + " red=" + Cm(shown.color.r)
                    + " hat=" + Cm(hat.localScale.y));
            }
        }
        if (Time.frameCount == dustFrame + 4 || Time.frameCount == dustFrame + 40)
        {
            Debug.Log("frame " + Time.frameCount + " dust particles=" + dust.particleCount + " playing="
                + dust.isPlaying);
        }
    }

    private void FixedUpdate()
    {
        if (move > 0f && ran && touching > 0 && !blocked)
        {
            int v = Cm(body.velocity.x);
            if (v < 100)
            {
                blocked = true;
                Debug.Log("frame " + Time.frameCount + " blocked at " + Cm(transform.position.x) + " slowest before="
                    + slowest + " green=" + Cm(shown.color.g) + " hat=" + Cm(hat.localScale.y));
            }
            else if (v < slowest)
            {
                slowest = v;
            }
        }
        body.velocity = new Vector2(move * speed, body.velocity.y);
        ran = move > 0f;
    }

    private void LateUpdate()
    {
        if (Time.frameCount % 60 == 1)
        {
            Vector3 eye = Camera.main.transform.position;
            Debug.Log("frame " + Time.frameCount + " hero " + Cm(transform.position.x) + "," + Cm(transform.position.y)
                + " camera " + Cm(eye.x) + "," + Cm(eye.y) + " size=" + Cm(Camera.main.orthographicSize));
        }
    }

    private void OnCollisionEnter2D(Collision2D collision)
    {
        touching++;
        if (collision.relativeVelocity.y > 1f || collision.relativeVelocity.y < -1f)
        {
            dust.Play();
            dustFrame = Time.frameCount;
            Debug.Log("frame " + Time.frameCount + " landed on " + collision.gameObject.name + " at "
                + Cm(transform.position.x));
        }
    }

    private void OnCollisionExit2D(Collision2D collision)
    {
        touching--;
    }

    private void OnTriggerEnter2D(Collider2D other)
    {
        animator.SetTrigger("hit");
        Debug.Log("frame " + Time.frameCount + " ran into " + other.name);
    }

    // Called by an event of the Run clip, once a loop.
    private void Step()
    {
        stepCount++;
        steps.text = "Steps " + stepCount;
    }

    // Called by an event of the Proud clip, which plays once after a jump.
    private void Bowed()
    {
        Debug.Log("frame " + Time.frameCount + " bowed after " + jumps + " steps=" + stepCount);
        jumps = 0;
    }

    private static int Cm(float value)
    {
        return Mathf.RoundToInt(value * 100f);
    }
}
