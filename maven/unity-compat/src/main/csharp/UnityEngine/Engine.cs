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
// Reference-only signatures of the UnityEngine classes the runtime implements
// in Java. Written from the public scripting reference; every body is
// `throw null` and nothing here is ever executed.
using System;
using System.Collections;
using System.Collections.Generic;

namespace UnityEngine
{
    public class Object
    {
        public string name { get { throw null; } set { throw null; } }
        public int GetInstanceID() { throw null; }
        public static void Destroy(Object obj) { throw null; }
        public static void Destroy(Object obj, float t) { throw null; }
        public static void DestroyImmediate(Object obj) { throw null; }
        public static void DontDestroyOnLoad(Object target) { throw null; }
        public static Object Instantiate(Object original) { throw null; }
        public static Object Instantiate(Object original, Transform parent) { throw null; }
        public static Object Instantiate(Object original, Transform parent, bool instantiateInWorldSpace) { throw null; }
        public static Object Instantiate(Object original, Vector3 position, Quaternion rotation) { throw null; }
        public static Object Instantiate(Object original, Vector3 position, Quaternion rotation, Transform parent) { throw null; }
        public static T Instantiate<T>(T original) where T : Object { throw null; }
        public static T Instantiate<T>(T original, Transform parent) where T : Object { throw null; }
        public static T Instantiate<T>(T original, Transform parent, bool worldPositionStays) where T : Object { throw null; }
        public static T Instantiate<T>(T original, Vector3 position, Quaternion rotation) where T : Object { throw null; }
        public static T Instantiate<T>(T original, Vector3 position, Quaternion rotation, Transform parent) where T : Object { throw null; }
        public static T FindObjectOfType<T>() where T : Object { throw null; }
        public static T[] FindObjectsOfType<T>() where T : Object { throw null; }
        public static implicit operator bool(Object exists) { throw null; }
        public static bool operator ==(Object x, Object y) { throw null; }
        public static bool operator !=(Object x, Object y) { throw null; }
    }

    public sealed class GameObject : Object
    {
        public GameObject() { throw null; }
        public GameObject(string name) { throw null; }
        public Transform transform { get { throw null; } }
        public GameObject gameObject { get { throw null; } }
        public bool activeSelf { get { throw null; } }
        public bool activeInHierarchy { get { throw null; } }
        public string tag { get { throw null; } set { throw null; } }
        public int layer { get { throw null; } set { throw null; } }
        public bool CompareTag(string tag) { throw null; }
        public void SetActive(bool value) { throw null; }
        public T GetComponent<T>() { throw null; }
        public T GetComponentInChildren<T>() { throw null; }
        public T GetComponentInParent<T>() { throw null; }
        public T[] GetComponents<T>() { throw null; }
        public T[] GetComponentsInChildren<T>() { throw null; }
        public T AddComponent<T>() where T : Component { throw null; }
        public static GameObject Find(string name) { throw null; }
        public static GameObject FindWithTag(string tag) { throw null; }
        public static GameObject FindGameObjectWithTag(string tag) { throw null; }
        public static GameObject[] FindGameObjectsWithTag(string tag) { throw null; }
    }

    public class Component : Object
    {
        public GameObject gameObject { get { throw null; } }
        public Transform transform { get { throw null; } }
        public string tag { get { throw null; } set { throw null; } }
        public bool CompareTag(string tag) { throw null; }
        public T GetComponent<T>() { throw null; }
        public T GetComponentInChildren<T>() { throw null; }
        public T GetComponentInParent<T>() { throw null; }
        public T[] GetComponents<T>() { throw null; }
        public T[] GetComponentsInChildren<T>() { throw null; }
    }

    public class Transform : Component, IEnumerable
    {
        public Vector3 position { get { throw null; } set { throw null; } }
        public Vector3 localPosition { get { throw null; } set { throw null; } }
        public Quaternion rotation { get { throw null; } set { throw null; } }
        public Quaternion localRotation { get { throw null; } set { throw null; } }
        public Vector3 eulerAngles { get { throw null; } set { throw null; } }
        public Vector3 localEulerAngles { get { throw null; } set { throw null; } }
        public Vector3 localScale { get { throw null; } set { throw null; } }
        public Vector3 lossyScale { get { throw null; } }
        public Vector3 up { get { throw null; } set { throw null; } }
        public Vector3 right { get { throw null; } set { throw null; } }
        public Vector3 forward { get { throw null; } }
        public Transform parent { get { throw null; } set { throw null; } }
        public Transform root { get { throw null; } }
        public int childCount { get { throw null; } }
        public Transform GetChild(int index) { throw null; }
        public Transform Find(string n) { throw null; }
        public void SetParent(Transform parent) { throw null; }
        public void SetParent(Transform parent, bool worldPositionStays) { throw null; }
        public void DetachChildren() { throw null; }
        public bool IsChildOf(Transform parent) { throw null; }
        public void Rotate(Vector3 eulers) { throw null; }
        public void Rotate(float xAngle, float yAngle, float zAngle) { throw null; }
        public void Translate(Vector3 translation) { throw null; }
        public void Translate(float x, float y, float z) { throw null; }
        public void Translate(Vector3 translation, Space relativeTo) { throw null; }
        public Vector3 TransformPoint(Vector3 position) { throw null; }
        public Vector3 InverseTransformPoint(Vector3 position) { throw null; }
        public Vector3 TransformDirection(Vector3 direction) { throw null; }
        public Vector3 InverseTransformDirection(Vector3 direction) { throw null; }
        public void SetPositionAndRotation(Vector3 position, Quaternion rotation) { throw null; }
        public int GetSiblingIndex() { throw null; }
        public void SetSiblingIndex(int index) { throw null; }
        public void SetAsFirstSibling() { throw null; }
        public void SetAsLastSibling() { throw null; }
        public IEnumerator GetEnumerator() { throw null; }
    }

    public sealed class RectTransform : Transform
    {
        public Vector2 anchorMin { get { throw null; } set { throw null; } }
        public Vector2 anchorMax { get { throw null; } set { throw null; } }
        public Vector2 anchoredPosition { get { throw null; } set { throw null; } }
        public Vector2 sizeDelta { get { throw null; } set { throw null; } }
        public Vector2 pivot { get { throw null; } set { throw null; } }
    }

    public class Behaviour : Component
    {
        public bool enabled { get { throw null; } set { throw null; } }
        public bool isActiveAndEnabled { get { throw null; } }
    }

    public class MonoBehaviour : Behaviour
    {
        public Coroutine StartCoroutine(IEnumerator routine) { throw null; }
        public void StopCoroutine(Coroutine routine) { throw null; }
        public void StopAllCoroutines() { throw null; }
        public void Invoke(string methodName, float time) { throw null; }
        public void InvokeRepeating(string methodName, float time, float repeatRate) { throw null; }
        public void CancelInvoke() { throw null; }
        public void CancelInvoke(string methodName) { throw null; }
        public bool IsInvoking() { throw null; }
        public bool IsInvoking(string methodName) { throw null; }
        public static void print(object message) { throw null; }
    }

    public class YieldInstruction
    {
    }

    public sealed class Coroutine : YieldInstruction
    {
    }

    public sealed class WaitForSeconds : YieldInstruction
    {
        public WaitForSeconds(float seconds) { throw null; }
    }

    public sealed class WaitForFixedUpdate : YieldInstruction
    {
        public WaitForFixedUpdate() { throw null; }
    }

    public sealed class WaitForEndOfFrame : YieldInstruction
    {
        public WaitForEndOfFrame() { throw null; }
    }

    public static class Time
    {
        public static float deltaTime { get { throw null; } }
        public static float fixedDeltaTime { get { throw null; } set { throw null; } }
        public static float unscaledDeltaTime { get { throw null; } }
        public static float time { get { throw null; } }
        public static float fixedTime { get { throw null; } }
        public static float unscaledTime { get { throw null; } }
        public static float timeSinceLevelLoad { get { throw null; } }
        public static float timeScale { get { throw null; } set { throw null; } }
        public static int frameCount { get { throw null; } }
    }

    public static class Debug
    {
        public static void Log(object message) { throw null; }
        public static void LogWarning(object message) { throw null; }
        public static void LogError(object message) { throw null; }
    }

    public static class Application
    {
        public static int targetFrameRate { get { throw null; } set { throw null; } }
        public static bool isPlaying { get { throw null; } }
        public static bool isEditor { get { throw null; } }
        public static bool isMobilePlatform { get { throw null; } }
        public static RuntimePlatform platform { get { throw null; } }
        public static void Quit() { throw null; }
    }

    public static class Random
    {
        public static float value { get { throw null; } }
        public static Vector2 insideUnitCircle { get { throw null; } }
        public static float Range(float minInclusive, float maxInclusive) { throw null; }
        public static int Range(int minInclusive, int maxExclusive) { throw null; }
        public static void InitState(int seed) { throw null; }
    }

    public static class Input
    {
        public static bool GetMouseButtonDown(int button) { throw null; }
        public static bool GetMouseButtonUp(int button) { throw null; }
        public static bool GetMouseButton(int button) { throw null; }
        public static Vector3 mousePosition { get { throw null; } }
        public static bool anyKey { get { throw null; } }
        public static bool anyKeyDown { get { throw null; } }
        public static bool GetKey(KeyCode key) { throw null; }
        public static bool GetKeyDown(KeyCode key) { throw null; }
        public static bool GetKeyUp(KeyCode key) { throw null; }
        public static bool GetKey(string name) { throw null; }
        public static bool GetKeyDown(string name) { throw null; }
        public static bool GetKeyUp(string name) { throw null; }
        public static float GetAxis(string axisName) { throw null; }
        public static float GetAxisRaw(string axisName) { throw null; }
        public static bool GetButton(string buttonName) { throw null; }
        public static bool GetButtonDown(string buttonName) { throw null; }
        public static bool GetButtonUp(string buttonName) { throw null; }
        public static int touchCount { get { throw null; } }
        public static Touch[] touches { get { throw null; } }
        public static Touch GetTouch(int index) { throw null; }
        public static bool touchSupported { get { throw null; } }
        public static bool multiTouchEnabled { get { throw null; } set { throw null; } }
        public static bool simulateMouseWithTouches { get { throw null; } set { throw null; } }
        public static bool mousePresent { get { throw null; } }
    }

    // A set of layers, one bit each.
    public struct LayerMask
    {
        internal int m_Bits;
        public int value { get { throw null; } set { throw null; } }
        public static implicit operator int(LayerMask mask) { throw null; }
        public static implicit operator LayerMask(int intVal) { throw null; }
        public static int GetMask(params string[] layerNames) { throw null; }
        public static int NameToLayer(string layerName) { throw null; }
        public static string LayerToName(int layer) { throw null; }
    }

    // What a query of the physics world found.
    public struct RaycastHit2D
    {
        public Vector2 centroid { get { throw null; } set { throw null; } }
        public Vector2 point { get { throw null; } set { throw null; } }
        public Vector2 normal { get { throw null; } set { throw null; } }
        public float distance { get { throw null; } set { throw null; } }
        public float fraction { get { throw null; } set { throw null; } }
        public Collider2D collider { get { throw null; } }
        public Rigidbody2D rigidbody { get { throw null; } }
        public Transform transform { get { throw null; } }
        public static implicit operator bool(RaycastHit2D hit) { throw null; }
    }

    // Which colliders a query of the physics world is to report.
    public struct ContactFilter2D
    {
        public bool useTriggers;
        public bool useLayerMask;
        public bool useDepth;
        public bool useOutsideDepth;
        public bool useNormalAngle;
        public bool useOutsideNormalAngle;
        public LayerMask layerMask;
        public float minDepth;
        public float maxDepth;
        public float minNormalAngle;
        public float maxNormalAngle;
        public const float NormalAngleUpperLimit = 359.9999f;
        public bool isFiltering { get { throw null; } }
        public ContactFilter2D NoFilter() { throw null; }
        public void SetLayerMask(LayerMask layerMask) { throw null; }
        public void ClearLayerMask() { throw null; }
        public void SetDepth(float minDepth, float maxDepth) { throw null; }
        public void ClearDepth() { throw null; }
        public void SetNormalAngle(float minNormalAngle, float maxNormalAngle) { throw null; }
        public void ClearNormalAngle() { throw null; }
        public bool IsFilteringTrigger(Collider2D collider) { throw null; }
        public bool IsFilteringLayerMask(GameObject obj) { throw null; }
        public bool IsFilteringDepth(GameObject obj) { throw null; }
        public bool IsFilteringNormalAngle(Vector2 normal) { throw null; }
        public bool IsFilteringNormalAngle(float angle) { throw null; }
    }

    public static class Physics2D
    {
        public static Vector2 gravity { get { throw null; } set { throw null; } }
        public static void IgnoreLayerCollision(int layer1, int layer2) { throw null; }
        public static void IgnoreLayerCollision(int layer1, int layer2, bool ignore) { throw null; }
        public static bool GetIgnoreLayerCollision(int layer1, int layer2) { throw null; }
        public const int IgnoreRaycastLayer = 4;
        public const int DefaultRaycastLayers = -5;
        public const int AllLayers = -1;
        public static bool queriesHitTriggers { get { throw null; } set { throw null; } }
        public static bool queriesStartInColliders { get { throw null; } set { throw null; } }
        public static bool autoSyncTransforms { get { throw null; } set { throw null; } }
        public static void SyncTransforms() { throw null; }
        public static bool IsTouching(Collider2D collider1, Collider2D collider2) { throw null; }
        public static bool IsTouchingLayers(Collider2D collider, int layerMask = AllLayers) { throw null; }
        public static RaycastHit2D Raycast(Vector2 origin, Vector2 direction, float distance = Mathf.Infinity, int layerMask = DefaultRaycastLayers, float minDepth = -Mathf.Infinity, float maxDepth = Mathf.Infinity) { throw null; }
        public static int Raycast(Vector2 origin, Vector2 direction, ContactFilter2D contactFilter, RaycastHit2D[] results, float distance = Mathf.Infinity) { throw null; }
        public static int Raycast(Vector2 origin, Vector2 direction, ContactFilter2D contactFilter, List<RaycastHit2D> results, float distance = Mathf.Infinity) { throw null; }
        public static RaycastHit2D[] RaycastAll(Vector2 origin, Vector2 direction, float distance = Mathf.Infinity, int layerMask = DefaultRaycastLayers, float minDepth = -Mathf.Infinity, float maxDepth = Mathf.Infinity) { throw null; }
        public static int RaycastNonAlloc(Vector2 origin, Vector2 direction, RaycastHit2D[] results, float distance = Mathf.Infinity, int layerMask = DefaultRaycastLayers, float minDepth = -Mathf.Infinity, float maxDepth = Mathf.Infinity) { throw null; }
        public static RaycastHit2D CircleCast(Vector2 origin, float radius, Vector2 direction, float distance = Mathf.Infinity, int layerMask = DefaultRaycastLayers, float minDepth = -Mathf.Infinity, float maxDepth = Mathf.Infinity) { throw null; }
        public static int CircleCast(Vector2 origin, float radius, Vector2 direction, ContactFilter2D contactFilter, RaycastHit2D[] results, float distance = Mathf.Infinity) { throw null; }
        public static int CircleCast(Vector2 origin, float radius, Vector2 direction, ContactFilter2D contactFilter, List<RaycastHit2D> results, float distance = Mathf.Infinity) { throw null; }
        public static RaycastHit2D[] CircleCastAll(Vector2 origin, float radius, Vector2 direction, float distance = Mathf.Infinity, int layerMask = DefaultRaycastLayers, float minDepth = -Mathf.Infinity, float maxDepth = Mathf.Infinity) { throw null; }
        public static int CircleCastNonAlloc(Vector2 origin, float radius, Vector2 direction, RaycastHit2D[] results, float distance = Mathf.Infinity, int layerMask = DefaultRaycastLayers, float minDepth = -Mathf.Infinity, float maxDepth = Mathf.Infinity) { throw null; }
        public static RaycastHit2D BoxCast(Vector2 origin, Vector2 size, float angle, Vector2 direction, float distance = Mathf.Infinity, int layerMask = DefaultRaycastLayers, float minDepth = -Mathf.Infinity, float maxDepth = Mathf.Infinity) { throw null; }
        public static int BoxCast(Vector2 origin, Vector2 size, float angle, Vector2 direction, ContactFilter2D contactFilter, RaycastHit2D[] results, float distance = Mathf.Infinity) { throw null; }
        public static int BoxCast(Vector2 origin, Vector2 size, float angle, Vector2 direction, ContactFilter2D contactFilter, List<RaycastHit2D> results, float distance = Mathf.Infinity) { throw null; }
        public static RaycastHit2D[] BoxCastAll(Vector2 origin, Vector2 size, float angle, Vector2 direction, float distance = Mathf.Infinity, int layerMask = DefaultRaycastLayers, float minDepth = -Mathf.Infinity, float maxDepth = Mathf.Infinity) { throw null; }
        public static int BoxCastNonAlloc(Vector2 origin, Vector2 size, float angle, Vector2 direction, RaycastHit2D[] results, float distance = Mathf.Infinity, int layerMask = DefaultRaycastLayers, float minDepth = -Mathf.Infinity, float maxDepth = Mathf.Infinity) { throw null; }
        public static RaycastHit2D CapsuleCast(Vector2 origin, Vector2 size, CapsuleDirection2D capsuleDirection, float angle, Vector2 direction, float distance = Mathf.Infinity, int layerMask = DefaultRaycastLayers, float minDepth = -Mathf.Infinity, float maxDepth = Mathf.Infinity) { throw null; }
        public static int CapsuleCast(Vector2 origin, Vector2 size, CapsuleDirection2D capsuleDirection, float angle, Vector2 direction, ContactFilter2D contactFilter, RaycastHit2D[] results, float distance = Mathf.Infinity) { throw null; }
        public static int CapsuleCast(Vector2 origin, Vector2 size, CapsuleDirection2D capsuleDirection, float angle, Vector2 direction, ContactFilter2D contactFilter, List<RaycastHit2D> results, float distance = Mathf.Infinity) { throw null; }
        public static RaycastHit2D[] CapsuleCastAll(Vector2 origin, Vector2 size, CapsuleDirection2D capsuleDirection, float angle, Vector2 direction, float distance = Mathf.Infinity, int layerMask = DefaultRaycastLayers, float minDepth = -Mathf.Infinity, float maxDepth = Mathf.Infinity) { throw null; }
        public static int CapsuleCastNonAlloc(Vector2 origin, Vector2 size, CapsuleDirection2D capsuleDirection, float angle, Vector2 direction, RaycastHit2D[] results, float distance = Mathf.Infinity, int layerMask = DefaultRaycastLayers, float minDepth = -Mathf.Infinity, float maxDepth = Mathf.Infinity) { throw null; }
        public static RaycastHit2D Linecast(Vector2 start, Vector2 end, int layerMask = DefaultRaycastLayers, float minDepth = -Mathf.Infinity, float maxDepth = Mathf.Infinity) { throw null; }
        public static int Linecast(Vector2 start, Vector2 end, ContactFilter2D contactFilter, RaycastHit2D[] results) { throw null; }
        public static int Linecast(Vector2 start, Vector2 end, ContactFilter2D contactFilter, List<RaycastHit2D> results) { throw null; }
        public static RaycastHit2D[] LinecastAll(Vector2 start, Vector2 end, int layerMask = DefaultRaycastLayers, float minDepth = -Mathf.Infinity, float maxDepth = Mathf.Infinity) { throw null; }
        public static int LinecastNonAlloc(Vector2 start, Vector2 end, RaycastHit2D[] results, int layerMask = DefaultRaycastLayers, float minDepth = -Mathf.Infinity, float maxDepth = Mathf.Infinity) { throw null; }
        public static Collider2D OverlapPoint(Vector2 point, int layerMask = DefaultRaycastLayers, float minDepth = -Mathf.Infinity, float maxDepth = Mathf.Infinity) { throw null; }
        public static int OverlapPoint(Vector2 point, ContactFilter2D contactFilter, Collider2D[] results) { throw null; }
        public static int OverlapPoint(Vector2 point, ContactFilter2D contactFilter, List<Collider2D> results) { throw null; }
        public static Collider2D[] OverlapPointAll(Vector2 point, int layerMask = DefaultRaycastLayers, float minDepth = -Mathf.Infinity, float maxDepth = Mathf.Infinity) { throw null; }
        public static int OverlapPointNonAlloc(Vector2 point, Collider2D[] results, int layerMask = DefaultRaycastLayers, float minDepth = -Mathf.Infinity, float maxDepth = Mathf.Infinity) { throw null; }
        public static Collider2D OverlapCircle(Vector2 point, float radius, int layerMask = DefaultRaycastLayers, float minDepth = -Mathf.Infinity, float maxDepth = Mathf.Infinity) { throw null; }
        public static int OverlapCircle(Vector2 point, float radius, ContactFilter2D contactFilter, Collider2D[] results) { throw null; }
        public static int OverlapCircle(Vector2 point, float radius, ContactFilter2D contactFilter, List<Collider2D> results) { throw null; }
        public static Collider2D[] OverlapCircleAll(Vector2 point, float radius, int layerMask = DefaultRaycastLayers, float minDepth = -Mathf.Infinity, float maxDepth = Mathf.Infinity) { throw null; }
        public static int OverlapCircleNonAlloc(Vector2 point, float radius, Collider2D[] results, int layerMask = DefaultRaycastLayers, float minDepth = -Mathf.Infinity, float maxDepth = Mathf.Infinity) { throw null; }
        public static Collider2D OverlapBox(Vector2 point, Vector2 size, float angle, int layerMask = DefaultRaycastLayers, float minDepth = -Mathf.Infinity, float maxDepth = Mathf.Infinity) { throw null; }
        public static int OverlapBox(Vector2 point, Vector2 size, float angle, ContactFilter2D contactFilter, Collider2D[] results) { throw null; }
        public static int OverlapBox(Vector2 point, Vector2 size, float angle, ContactFilter2D contactFilter, List<Collider2D> results) { throw null; }
        public static Collider2D[] OverlapBoxAll(Vector2 point, Vector2 size, float angle, int layerMask = DefaultRaycastLayers, float minDepth = -Mathf.Infinity, float maxDepth = Mathf.Infinity) { throw null; }
        public static int OverlapBoxNonAlloc(Vector2 point, Vector2 size, float angle, Collider2D[] results, int layerMask = DefaultRaycastLayers, float minDepth = -Mathf.Infinity, float maxDepth = Mathf.Infinity) { throw null; }
        public static Collider2D OverlapArea(Vector2 pointA, Vector2 pointB, int layerMask = DefaultRaycastLayers, float minDepth = -Mathf.Infinity, float maxDepth = Mathf.Infinity) { throw null; }
        public static int OverlapArea(Vector2 pointA, Vector2 pointB, ContactFilter2D contactFilter, Collider2D[] results) { throw null; }
        public static int OverlapArea(Vector2 pointA, Vector2 pointB, ContactFilter2D contactFilter, List<Collider2D> results) { throw null; }
        public static Collider2D[] OverlapAreaAll(Vector2 pointA, Vector2 pointB, int layerMask = DefaultRaycastLayers, float minDepth = -Mathf.Infinity, float maxDepth = Mathf.Infinity) { throw null; }
        public static int OverlapAreaNonAlloc(Vector2 pointA, Vector2 pointB, Collider2D[] results, int layerMask = DefaultRaycastLayers, float minDepth = -Mathf.Infinity, float maxDepth = Mathf.Infinity) { throw null; }
        public static Collider2D OverlapCapsule(Vector2 point, Vector2 size, CapsuleDirection2D direction, float angle, int layerMask = DefaultRaycastLayers, float minDepth = -Mathf.Infinity, float maxDepth = Mathf.Infinity) { throw null; }
        public static int OverlapCapsule(Vector2 point, Vector2 size, CapsuleDirection2D direction, float angle, ContactFilter2D contactFilter, Collider2D[] results) { throw null; }
        public static int OverlapCapsule(Vector2 point, Vector2 size, CapsuleDirection2D direction, float angle, ContactFilter2D contactFilter, List<Collider2D> results) { throw null; }
        public static Collider2D[] OverlapCapsuleAll(Vector2 point, Vector2 size, CapsuleDirection2D direction, float angle, int layerMask = DefaultRaycastLayers, float minDepth = -Mathf.Infinity, float maxDepth = Mathf.Infinity) { throw null; }
        public static int OverlapCapsuleNonAlloc(Vector2 point, Vector2 size, CapsuleDirection2D direction, float angle, Collider2D[] results, int layerMask = DefaultRaycastLayers, float minDepth = -Mathf.Infinity, float maxDepth = Mathf.Infinity) { throw null; }
    }

    public sealed class Rigidbody2D : Component
    {
        public Vector2 velocity { get { throw null; } set { throw null; } }
        public float angularVelocity { get { throw null; } set { throw null; } }
        public Vector2 position { get { throw null; } set { throw null; } }
        public float rotation { get { throw null; } set { throw null; } }
        public float mass { get { throw null; } set { throw null; } }
        public float drag { get { throw null; } set { throw null; } }
        public float angularDrag { get { throw null; } set { throw null; } }
        public float gravityScale { get { throw null; } set { throw null; } }
        public float inertia { get { throw null; } }
        public RigidbodyType2D bodyType { get { throw null; } set { throw null; } }
        public bool isKinematic { get { throw null; } set { throw null; } }
        public bool simulated { get { throw null; } set { throw null; } }
        public bool freezeRotation { get { throw null; } set { throw null; } }
        public RigidbodyConstraints2D constraints { get { throw null; } set { throw null; } }
        public CollisionDetectionMode2D collisionDetectionMode { get { throw null; } set { throw null; } }
        public void AddForce(Vector2 force) { throw null; }
        public void AddForce(Vector2 force, ForceMode2D mode) { throw null; }
        public void AddTorque(float torque) { throw null; }
        public void AddTorque(float torque, ForceMode2D mode) { throw null; }
        public void MovePosition(Vector2 position) { throw null; }
        public void MoveRotation(float angle) { throw null; }
        public void Sleep() { throw null; }
        public void WakeUp() { throw null; }
        public bool IsSleeping() { throw null; }
        public bool OverlapPoint(Vector2 point) { throw null; }
        public bool IsTouching(Collider2D collider) { throw null; }
        public bool IsTouchingLayers(int layerMask = Physics2D.AllLayers) { throw null; }
        public int Cast(Vector2 direction, RaycastHit2D[] results, float distance = Mathf.Infinity) { throw null; }
    }

    public class Collider2D : Behaviour
    {
        public Rigidbody2D attachedRigidbody { get { throw null; } }
        public bool isTrigger { get { throw null; } set { throw null; } }
        public Vector2 offset { get { throw null; } set { throw null; } }
        public Bounds bounds { get { throw null; } }
        public bool OverlapPoint(Vector2 point) { throw null; }
        public bool IsTouching(Collider2D collider) { throw null; }
        public bool IsTouchingLayers(int layerMask = Physics2D.AllLayers) { throw null; }
        public int Cast(Vector2 direction, RaycastHit2D[] results, float distance = Mathf.Infinity, bool ignoreSiblingColliders = true) { throw null; }
        public int OverlapCollider(ContactFilter2D contactFilter, Collider2D[] results) { throw null; }
    }

    public sealed class BoxCollider2D : Collider2D
    {
        public Vector2 size { get { throw null; } set { throw null; } }
    }

    public sealed class CircleCollider2D : Collider2D
    {
        public float radius { get { throw null; } set { throw null; } }
    }

    public sealed class CapsuleCollider2D : Collider2D
    {
        public Vector2 size { get { throw null; } set { throw null; } }
        public CapsuleDirection2D direction { get { throw null; } set { throw null; } }
    }

    public sealed class EdgeCollider2D : Collider2D
    {
        public Vector2[] points { get { throw null; } set { throw null; } }
        public int pointCount { get { throw null; } }
        public int edgeCount { get { throw null; } }
    }

    public sealed class PolygonCollider2D : Collider2D
    {
        public Vector2[] points { get { throw null; } set { throw null; } }
        public int pathCount { get { throw null; } set { throw null; } }
        public Vector2[] GetPath(int index) { throw null; }
        public void SetPath(int index, Vector2[] points) { throw null; }
        public int GetTotalPointCount() { throw null; }
    }

    public class Collision2D
    {
        public GameObject gameObject { get { throw null; } }
        public Transform transform { get { throw null; } }
        public Collider2D collider { get { throw null; } }
        public Collider2D otherCollider { get { throw null; } }
        public Rigidbody2D rigidbody { get { throw null; } }
        public Rigidbody2D otherRigidbody { get { throw null; } }
        public Vector2 relativeVelocity { get { throw null; } }
        public int contactCount { get { throw null; } }
        public ContactPoint2D GetContact(int index) { throw null; }
    }

    public class Renderer : Component
    {
        public bool enabled { get { throw null; } set { throw null; } }
        public int sortingOrder { get { throw null; } set { throw null; } }
        public int sortingLayerID { get { throw null; } set { throw null; } }
    }

    public sealed class Sprite : Object
    {
        public float pixelsPerUnit { get { throw null; } }
        public Vector2 pivot { get { throw null; } }
        public Texture2D texture { get { throw null; } }
        public Bounds bounds { get { throw null; } }
    }

    public class Texture : Object
    {
        public virtual int width { get { throw null; } set { throw null; } }
        public virtual int height { get { throw null; } set { throw null; } }
    }

    public sealed class Texture2D : Texture
    {
    }

    public class ScriptableObject : Object
    {
    }

    // Drawn in the editor's scene view only; nothing is drawn in a player.
    public static class Gizmos
    {
        public static Color color { get { throw null; } set { throw null; } }
        public static void DrawLine(Vector3 from, Vector3 to) { throw null; }
        public static void DrawRay(Vector3 from, Vector3 direction) { throw null; }
        public static void DrawCube(Vector3 center, Vector3 size) { throw null; }
        public static void DrawWireCube(Vector3 center, Vector3 size) { throw null; }
        public static void DrawSphere(Vector3 center, float radius) { throw null; }
        public static void DrawWireSphere(Vector3 center, float radius) { throw null; }
    }

    public class RuntimeAnimatorController : Object
    {
        public AnimationClip[] animationClips { get { throw null; } }
    }

    public sealed class AnimationClip : Object
    {
        public float length { get { throw null; } }
        public float frameRate { get { throw null; } }
        public bool isLooping { get { throw null; } }
    }

    public struct AnimatorStateInfo
    {
        public int shortNameHash { get { throw null; } }
        public int fullPathHash { get { throw null; } }
        public float normalizedTime { get { throw null; } }
        public float length { get { throw null; } }
        public float speed { get { throw null; } }
        public bool loop { get { throw null; } }
        public bool IsName(string name) { throw null; }
    }

    public sealed class Animator : Behaviour
    {
        public RuntimeAnimatorController runtimeAnimatorController { get { throw null; } set { throw null; } }
        public float speed { get { throw null; } set { throw null; } }
        public static int StringToHash(string name) { throw null; }
        public void SetFloat(string name, float value) { throw null; }
        public void SetFloat(int id, float value) { throw null; }
        public float GetFloat(string name) { throw null; }
        public float GetFloat(int id) { throw null; }
        public void SetBool(string name, bool value) { throw null; }
        public void SetBool(int id, bool value) { throw null; }
        public bool GetBool(string name) { throw null; }
        public bool GetBool(int id) { throw null; }
        public void SetInteger(string name, int value) { throw null; }
        public void SetInteger(int id, int value) { throw null; }
        public int GetInteger(string name) { throw null; }
        public int GetInteger(int id) { throw null; }
        public void SetTrigger(string name) { throw null; }
        public void SetTrigger(int id) { throw null; }
        public void ResetTrigger(string name) { throw null; }
        public void ResetTrigger(int id) { throw null; }
        public void Play(string stateName) { throw null; }
        public void Play(string stateName, int layer) { throw null; }
        public void Play(string stateName, int layer, float normalizedTime) { throw null; }
        public void Play(int stateNameHash) { throw null; }
        public void Play(int stateNameHash, int layer) { throw null; }
        public void Play(int stateNameHash, int layer, float normalizedTime) { throw null; }
        public void CrossFade(string stateName, float normalizedTransitionDuration) { throw null; }
        public void CrossFade(string stateName, float normalizedTransitionDuration, int layer) { throw null; }
        public AnimatorStateInfo GetCurrentAnimatorStateInfo(int layerIndex) { throw null; }
        public bool IsInTransition(int layerIndex) { throw null; }
        public void Rebind() { throw null; }
    }

    public class Effector2D : Behaviour
    {
        public bool useColliderMask { get { throw null; } set { throw null; } }
        public int colliderMask { get { throw null; } set { throw null; } }
    }

    public sealed class PlatformEffector2D : Effector2D
    {
        public bool useOneWay { get { throw null; } set { throw null; } }
        public bool useOneWayGrouping { get { throw null; } set { throw null; } }
        public float surfaceArc { get { throw null; } set { throw null; } }
        public float rotationalOffset { get { throw null; } set { throw null; } }
        public bool useSideFriction { get { throw null; } set { throw null; } }
        public bool useSideBounce { get { throw null; } set { throw null; } }
        public float sideArc { get { throw null; } set { throw null; } }
    }

    public sealed class CompositeCollider2D : Collider2D
    {
        public int pathCount { get { throw null; } }
        public int pointCount { get { throw null; } }
        public int GetPathPointCount(int index) { throw null; }
        public int GetPath(int index, Vector2[] points) { throw null; }
        public void GenerateGeometry() { throw null; }
    }

    public class GridLayout : Behaviour
    {
        public Vector3 cellSize { get { throw null; } }
        public Vector3 cellGap { get { throw null; } }
        public Vector3Int WorldToCell(Vector3 worldPosition) { throw null; }
        public Vector3 CellToWorld(Vector3Int cellPosition) { throw null; }
        public Vector3Int LocalToCell(Vector3 localPosition) { throw null; }
        public Vector3 CellToLocal(Vector3Int cellPosition) { throw null; }
    }

    public sealed class Grid : GridLayout
    {
        public Vector3 GetCellCenterWorld(Vector3Int position) { throw null; }
        public Vector3 GetCellCenterLocal(Vector3Int position) { throw null; }
    }

    public sealed class ParticleSystem : Component
    {
        public bool isPlaying { get { throw null; } }
        public bool isEmitting { get { throw null; } }
        public bool isStopped { get { throw null; } }
        public bool isPaused { get { throw null; } }
        public int particleCount { get { throw null; } }
        public float time { get { throw null; } set { throw null; } }
        public MainModule main { get { throw null; } }
        public EmissionModule emission { get { throw null; } }
        public VelocityOverLifetimeModule velocityOverLifetime { get { throw null; } }
        public void Play() { throw null; }
        public void Play(bool withChildren) { throw null; }
        public void Stop() { throw null; }
        public void Stop(bool withChildren) { throw null; }
        public void Stop(bool withChildren, ParticleSystemStopBehavior stopBehavior) { throw null; }
        public void Pause() { throw null; }
        public void Pause(bool withChildren) { throw null; }
        public void Clear() { throw null; }
        public void Clear(bool withChildren) { throw null; }
        public void Emit(int count) { throw null; }

        public struct MinMaxCurve
        {
            public MinMaxCurve(float constant) { throw null; }
            public MinMaxCurve(float min, float max) { throw null; }
            public ParticleSystemCurveMode mode { get { throw null; } set { throw null; } }
            public float constant { get { throw null; } set { throw null; } }
            public float constantMin { get { throw null; } set { throw null; } }
            public float constantMax { get { throw null; } set { throw null; } }
            public static implicit operator MinMaxCurve(float constant) { throw null; }
        }

        public struct MainModule
        {
            public float duration { get { throw null; } set { throw null; } }
            public bool loop { get { throw null; } set { throw null; } }
            public bool playOnAwake { get { throw null; } set { throw null; } }
            public MinMaxCurve startLifetime { get { throw null; } set { throw null; } }
            public MinMaxCurve startSpeed { get { throw null; } set { throw null; } }
            public MinMaxCurve startSize { get { throw null; } set { throw null; } }
            public MinMaxCurve gravityModifier { get { throw null; } set { throw null; } }
            public int maxParticles { get { throw null; } set { throw null; } }
            public float simulationSpeed { get { throw null; } set { throw null; } }
        }

        public struct EmissionModule
        {
            public bool enabled { get { throw null; } set { throw null; } }
            public MinMaxCurve rateOverTime { get { throw null; } set { throw null; } }
        }

        public struct VelocityOverLifetimeModule
        {
            public bool enabled { get { throw null; } set { throw null; } }
            public MinMaxCurve x { get { throw null; } set { throw null; } }
            public MinMaxCurve y { get { throw null; } set { throw null; } }
            public MinMaxCurve z { get { throw null; } set { throw null; } }
        }
    }

    public sealed class ParticleSystemRenderer : Renderer
    {
    }

    public sealed class SpriteRenderer : Renderer
    {
        public Sprite sprite { get { throw null; } set { throw null; } }
        public Color color { get { throw null; } set { throw null; } }
        public bool flipX { get { throw null; } set { throw null; } }
        public bool flipY { get { throw null; } set { throw null; } }
    }

    public sealed class Camera : Behaviour
    {
        public static Camera main { get { throw null; } }
        public bool orthographic { get { throw null; } set { throw null; } }
        public float orthographicSize { get { throw null; } set { throw null; } }
        public Color backgroundColor { get { throw null; } set { throw null; } }
        public float aspect { get { throw null; } }
        public int pixelWidth { get { throw null; } }
        public int pixelHeight { get { throw null; } }
        public Vector3 WorldToScreenPoint(Vector3 position) { throw null; }
        public Vector3 ScreenToWorldPoint(Vector3 position) { throw null; }
        public Vector3 WorldToViewportPoint(Vector3 position) { throw null; }
        public Vector3 ViewportToWorldPoint(Vector3 position) { throw null; }
    }

    public sealed class Canvas : Behaviour
    {
        public RenderMode renderMode { get { throw null; } set { throw null; } }
        public Camera worldCamera { get { throw null; } set { throw null; } }
        public int sortingOrder { get { throw null; } set { throw null; } }
        public float scaleFactor { get { throw null; } set { throw null; } }
        public float planeDistance { get { throw null; } set { throw null; } }
    }

    public sealed class CanvasRenderer : Component
    {
    }

    public class TextAsset : Object
    {
        public string text { get { throw null; } }
        public byte[] bytes { get { throw null; } }
        public override string ToString() { throw null; }
    }

    public sealed class Resources
    {
        public static Object Load(string path) { throw null; }
        public static Object Load(string path, Type systemTypeInstance) { throw null; }
        public static T Load<T>(string path) where T : Object { throw null; }
        public static void UnloadAsset(Object assetToUnload) { throw null; }
    }

    public sealed class AudioListener : Behaviour
    {
        public static float volume { get { throw null; } set { throw null; } }
        public static bool pause { get { throw null; } set { throw null; } }
    }

    public sealed class AudioClip : Object
    {
        public float length { get { throw null; } }
        public int samples { get { throw null; } }
        public int channels { get { throw null; } }
        public int frequency { get { throw null; } }
        public bool LoadAudioData() { throw null; }
        public bool UnloadAudioData() { throw null; }
    }

    public sealed class AudioSource : Behaviour
    {
        public AudioClip clip { get { throw null; } set { throw null; } }
        public float volume { get { throw null; } set { throw null; } }
        public float pitch { get { throw null; } set { throw null; } }
        public bool loop { get { throw null; } set { throw null; } }
        public bool mute { get { throw null; } set { throw null; } }
        public bool playOnAwake { get { throw null; } set { throw null; } }
        public bool isPlaying { get { throw null; } }
        public float time { get { throw null; } set { throw null; } }
        public void Play() { throw null; }
        public void PlayDelayed(float delay) { throw null; }
        public void Stop() { throw null; }
        public void Pause() { throw null; }
        public void UnPause() { throw null; }
        public void PlayOneShot(AudioClip clip) { throw null; }
        public void PlayOneShot(AudioClip clip, float volumeScale) { throw null; }
        public static void PlayClipAtPoint(AudioClip clip, Vector3 position) { throw null; }
        public static void PlayClipAtPoint(AudioClip clip, Vector3 position, float volume) { throw null; }
    }

    public static class PlayerPrefs
    {
        public static void SetInt(string key, int value) { throw null; }
        public static int GetInt(string key) { throw null; }
        public static int GetInt(string key, int defaultValue) { throw null; }
        public static void SetFloat(string key, float value) { throw null; }
        public static float GetFloat(string key) { throw null; }
        public static float GetFloat(string key, float defaultValue) { throw null; }
        public static void SetString(string key, string value) { throw null; }
        public static string GetString(string key) { throw null; }
        public static string GetString(string key, string defaultValue) { throw null; }
        public static bool HasKey(string key) { throw null; }
        public static void DeleteKey(string key) { throw null; }
        public static void DeleteAll() { throw null; }
        public static void Save() { throw null; }
    }

    public static class Screen
    {
        public static int width { get { throw null; } }
        public static int height { get { throw null; } }
    }

    // Attributes the editor reads. A build has no use for any of them except
    // SerializeField, which the scene compiler and Instantiate both honour.
    [AttributeUsage(AttributeTargets.Field)]
    public sealed class SerializeField : Attribute
    {
    }

    [AttributeUsage(AttributeTargets.Field)]
    public sealed class HideInInspector : Attribute
    {
    }

    [AttributeUsage(AttributeTargets.Field, AllowMultiple = true)]
    public sealed class HeaderAttribute : Attribute
    {
        public HeaderAttribute(string header) { }
    }

    [AttributeUsage(AttributeTargets.Field)]
    public sealed class TooltipAttribute : Attribute
    {
        public TooltipAttribute(string tooltip) { }
    }

    [AttributeUsage(AttributeTargets.Field, AllowMultiple = true)]
    public sealed class SpaceAttribute : Attribute
    {
        public SpaceAttribute() { }
        public SpaceAttribute(float height) { }
    }

    [AttributeUsage(AttributeTargets.Field)]
    public sealed class RangeAttribute : Attribute
    {
        public RangeAttribute(float min, float max) { }
    }

    [AttributeUsage(AttributeTargets.Field)]
    public sealed class MinAttribute : Attribute
    {
        public MinAttribute(float min) { }
    }

    [AttributeUsage(AttributeTargets.Field)]
    public sealed class TextAreaAttribute : Attribute
    {
        public TextAreaAttribute() { }
        public TextAreaAttribute(int minLines, int maxLines) { }
    }

    [AttributeUsage(AttributeTargets.Class, AllowMultiple = true)]
    public sealed class RequireComponent : Attribute
    {
        public RequireComponent(Type requiredComponent) { }
        public RequireComponent(Type requiredComponent, Type requiredComponent2) { }
        public RequireComponent(Type requiredComponent, Type requiredComponent2, Type requiredComponent3) { }
    }

    [AttributeUsage(AttributeTargets.Class)]
    public sealed class DisallowMultipleComponent : Attribute
    {
    }

    [AttributeUsage(AttributeTargets.Class)]
    public sealed class AddComponentMenu : Attribute
    {
        public AddComponentMenu(string menuName) { }
    }

    [AttributeUsage(AttributeTargets.Class)]
    public sealed class DefaultExecutionOrder : Attribute
    {
        public DefaultExecutionOrder(int order) { }
    }
}

namespace UnityEngine.EventSystems
{
    public abstract class UIBehaviour : MonoBehaviour
    {
    }

    public class EventSystem : UIBehaviour
    {
        public static EventSystem current { get { throw null; } }
        public bool IsPointerOverGameObject() { throw null; }
        public GameObject currentSelectedGameObject { get { throw null; } }
        public void SetSelectedGameObject(GameObject selected) { throw null; }
    }
}

namespace UnityEngine.Events
{
    public delegate void UnityAction();

    public abstract class UnityEventBase
    {
        public void RemoveAllListeners() { throw null; }
        public int GetPersistentEventCount() { throw null; }
    }

    public class UnityEvent : UnityEventBase
    {
        public void AddListener(UnityAction call) { throw null; }
        public void RemoveListener(UnityAction call) { throw null; }
        public void Invoke() { throw null; }
    }
}

namespace UnityEngine.UI
{
    public class Selectable : UnityEngine.EventSystems.UIBehaviour
    {
        public bool interactable { get { throw null; } set { throw null; } }
        public bool IsInteractable() { throw null; }
    }

    public class Button : Selectable
    {
        public class ButtonClickedEvent : UnityEngine.Events.UnityEvent
        {
        }

        public ButtonClickedEvent onClick { get { throw null; } set { throw null; } }
    }

    public class GraphicRaycaster : UnityEngine.EventSystems.UIBehaviour
    {
    }

    public class Image : MaskableGraphic
    {
        public Sprite sprite { get { throw null; } set { throw null; } }
        public float fillAmount { get { throw null; } set { throw null; } }
        public bool preserveAspect { get { throw null; } set { throw null; } }
    }

    public abstract class Graphic : UnityEngine.EventSystems.UIBehaviour
    {
        public virtual bool raycastTarget { get { throw null; } set { throw null; } }
        public virtual Color color { get { throw null; } set { throw null; } }
        public Canvas canvas { get { throw null; } }
        public RectTransform rectTransform { get { throw null; } }
    }

    public abstract class MaskableGraphic : Graphic
    {
    }

    public class Text : MaskableGraphic
    {
        public virtual string text { get { throw null; } set { throw null; } }
        public int fontSize { get { throw null; } set { throw null; } }
        public TextAnchor alignment { get { throw null; } set { throw null; } }
        public FontStyle fontStyle { get { throw null; } set { throw null; } }
        public bool resizeTextForBestFit { get { throw null; } set { throw null; } }
        public int resizeTextMinSize { get { throw null; } set { throw null; } }
        public int resizeTextMaxSize { get { throw null; } set { throw null; } }
    }

    public class CanvasScaler : UnityEngine.EventSystems.UIBehaviour
    {
        public float scaleFactor { get { throw null; } set { throw null; } }
        public Vector2 referenceResolution { get { throw null; } set { throw null; } }
        public float matchWidthOrHeight { get { throw null; } set { throw null; } }
    }
}

namespace UnityEngine.SceneManagement
{
    public static class SceneManager
    {
        public static int sceneCount { get { throw null; } }
        public static int sceneCountInBuildSettings { get { throw null; } }
        public static void LoadScene(string sceneName) { throw null; }
        public static void LoadScene(int sceneBuildIndex) { throw null; }
        public static Scene GetActiveScene() { throw null; }
    }
}

namespace UnityEngine.Assertions
{
    // Unity compiles these calls out of a release player (they carry
    // [Conditional("UNITY_ASSERTIONS")]); here they run, as they do in the
    // editor and in a development build, and a failed one is logged.
    public static class Assert
    {
        public static void IsTrue(bool condition) { throw null; }
        public static void IsTrue(bool condition, string message) { throw null; }
        public static void IsFalse(bool condition) { throw null; }
        public static void IsFalse(bool condition, string message) { throw null; }
        public static void IsNull<T>(T value) where T : class { throw null; }
        public static void IsNull<T>(T value, string message) where T : class { throw null; }
        public static void IsNotNull<T>(T value) where T : class { throw null; }
        public static void IsNotNull<T>(T value, string message) where T : class { throw null; }
        public static void AreEqual<T>(T expected, T actual) { throw null; }
        public static void AreEqual<T>(T expected, T actual, string message) { throw null; }
        public static void AreNotEqual<T>(T expected, T actual) { throw null; }
        public static void AreNotEqual<T>(T expected, T actual, string message) { throw null; }
        public static void AreApproximatelyEqual(float expected, float actual) { throw null; }
        public static void AreApproximatelyEqual(float expected, float actual, string message) { throw null; }
        public static void AreApproximatelyEqual(float expected, float actual, float tolerance) { throw null; }
        public static void AreNotApproximatelyEqual(float expected, float actual) { throw null; }
        public static void AreNotApproximatelyEqual(float expected, float actual, string message) { throw null; }
        public static void AreNotApproximatelyEqual(float expected, float actual, float tolerance) { throw null; }
    }
}

namespace UnityEngine.Serialization
{
    [AttributeUsage(AttributeTargets.Field, AllowMultiple = true)]
    public sealed class FormerlySerializedAsAttribute : Attribute
    {
        public FormerlySerializedAsAttribute(string oldName) { }
    }
}

namespace UnityEngine.Tilemaps
{
    public class TileBase : ScriptableObject
    {
    }

    public class Tile : TileBase
    {
        public Sprite sprite { get { throw null; } set { throw null; } }
        public Color color { get { throw null; } set { throw null; } }
    }

    public sealed class Tilemap : GridLayout
    {
        public Grid layoutGrid { get { throw null; } }
        public Color color { get { throw null; } set { throw null; } }
        public Vector3Int origin { get { throw null; } }
        public Vector3Int size { get { throw null; } }
        public TileBase GetTile(Vector3Int position) { throw null; }
        public T GetTile<T>(Vector3Int position) where T : TileBase { throw null; }
        public Sprite GetSprite(Vector3Int position) { throw null; }
        public bool HasTile(Vector3Int position) { throw null; }
        public void SetTile(Vector3Int position, TileBase tile) { throw null; }
        public void ClearAllTiles() { throw null; }
        public Vector3 GetCellCenterWorld(Vector3Int position) { throw null; }
        public Vector3 GetCellCenterLocal(Vector3Int position) { throw null; }
        public void RefreshAllTiles() { throw null; }
        public void CompressBounds() { throw null; }
    }

    public sealed class TilemapRenderer : Renderer
    {
    }

    public sealed class TilemapCollider2D : Collider2D
    {
    }
}

// Namespaces a script names in a `using` and takes nothing from. A `using`
// of a namespace no referenced assembly has is an error, so each exists.
namespace UnityEngine.Rendering
{
    internal static class NamespacePlaceholder
    {
    }
}

namespace UnityEngine.PlayerLoop
{
    internal static class NamespacePlaceholder
    {
    }
}

namespace UnityEngine.TextCore.Text
{
    internal static class NamespacePlaceholder
    {
    }
}

// The camera package. Written from its public manual: a virtual camera says
// where the real one should be, and the brain on the real camera puts it there.
namespace Cinemachine
{
    public struct LensSettings
    {
        public float FieldOfView;
        public float OrthographicSize;
        public float NearClipPlane;
        public float FarClipPlane;
        public float Dutch;
    }

    public abstract class CinemachineVirtualCameraBase : UnityEngine.MonoBehaviour
    {
        public int m_Priority;
        public int Priority { get { throw null; } set { throw null; } }
        public abstract UnityEngine.Transform LookAt { get; set; }
        public abstract UnityEngine.Transform Follow { get; set; }
    }

    public class CinemachineVirtualCamera : CinemachineVirtualCameraBase
    {
        public UnityEngine.Transform m_LookAt;
        public UnityEngine.Transform m_Follow;
        public LensSettings m_Lens;
        public override UnityEngine.Transform LookAt { get { throw null; } set { throw null; } }
        public override UnityEngine.Transform Follow { get { throw null; } set { throw null; } }
    }

    public abstract class CinemachineComponentBase : UnityEngine.MonoBehaviour
    {
    }

    public class CinemachineTransposer : CinemachineComponentBase
    {
    }

    public class CinemachineFramingTransposer : CinemachineComponentBase
    {
    }

    public class CinemachineBrain : UnityEngine.MonoBehaviour
    {
        public UnityEngine.Camera OutputCamera { get { throw null; } }
    }
}

// The text package. Written from its public manual.
namespace TMPro
{
    public abstract class TMP_Text : UnityEngine.UI.MaskableGraphic
    {
        public virtual string text { get { throw null; } set { throw null; } }
        public float fontSize { get { throw null; } set { throw null; } }
        public bool enableAutoSizing { get { throw null; } set { throw null; } }
        public float fontSizeMin { get { throw null; } set { throw null; } }
        public float fontSizeMax { get { throw null; } set { throw null; } }
        public TextAlignmentOptions alignment { get { throw null; } set { throw null; } }
        public FontStyles fontStyle { get { throw null; } set { throw null; } }
        public bool enableWordWrapping { get { throw null; } set { throw null; } }
        public void SetText(string sourceText) { throw null; }
    }

    public class TextMeshProUGUI : TMP_Text
    {
    }

    public class TextMeshPro : TMP_Text
    {
        public int sortingOrder { get { throw null; } set { throw null; } }
        public int sortingLayerID { get { throw null; } set { throw null; } }
    }
}

namespace UnityEditor
{
    // A script that says `using UnityEditor;` and then uses nothing of it
    // compiles in the editor, and is the commonest leftover in a project
    // that was never built as a player. The namespace exists so that such a
    // script compiles here too; it has nothing in it to call.
    internal static class NamespacePlaceholder
    {
    }
}
