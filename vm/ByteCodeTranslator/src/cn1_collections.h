/* Native collection layouts. No Java cursor or temporary array is materialized. */
#ifndef CN1_COLLECTIONS_H
#define CN1_COLLECTIONS_H
#include "cn1_globals.h"
#include <string.h>
#if defined(__has_include)
#if __has_include("java_util_ArrayList.h")
#include "java_util_ArrayList.h"
#define CN1_COLL_ARRAYLIST 1
#endif
#if __has_include("java_util_Arrays_ArrayList.h")
#include "java_util_Arrays_ArrayList.h"
#define CN1_COLL_ARRAY_VIEW 1
#endif
#if __has_include("java_util_HashMap.h")
#include "java_util_HashMap.h"
#define CN1_COLL_HASHMAP 1
#endif
#if __has_include("java_util_LinkedHashMap.h")
#include "java_util_LinkedHashMap.h"
#define CN1_COLL_LINKED 1
#endif
#if __has_include("java_util_HashMap_KeySet.h")
#include "java_util_HashMap_KeySet.h"
#define CN1_COLL_KEYS 1
#endif
#if __has_include("java_util_HashMap_Values.h")
#include "java_util_HashMap_Values.h"
#define CN1_COLL_VALUES 1
#endif
#if __has_include("java_util_HashSet.h")
#include "java_util_HashSet.h"
#define CN1_COLL_SET 1
#endif
#if __has_include("java_util_LinkedHashSet.h")
#include "java_util_LinkedHashSet.h"
#define CN1_COLL_ORDERED_SET 1
#endif
#if __has_include("java_util_IdentityHashMap.h")
#include "java_util_IdentityHashMap.h"
#define CN1_COLL_IDENTITY 1
#endif
#if __has_include("java_util_IdentityHashMap_KeySet.h")
#include "java_util_IdentityHashMap_KeySet.h"
#define CN1_COLL_IDENTITY_KEYS 1
#endif
#if __has_include("java_util_IdentityHashMap_Values.h")
#include "java_util_IdentityHashMap_Values.h"
#define CN1_COLL_IDENTITY_VALUES 1
#endif
#if __has_include("java_util_Collections_SetFromMap.h")
#include "java_util_Collections_SetFromMap.h"
#define CN1_COLL_SET_FROM_MAP 1
#endif
#endif

enum { CN1_COLL_DENSE = 1, CN1_COLL_HASH = 2, CN1_COLL_ORDERED = 3, CN1_COLL_ID = 4 };
typedef struct CN1CollectionView {
    JAVA_OBJECT owner, nullObject;
    JAVA_OBJECT* data;
    JAVA_INT* metadata;
    JAVA_INT* links;
    JAVA_INT kind, count, capacity, first, valueOffset;
} CN1CollectionView;

static inline int cn1CollectionMap(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT owner,
                                    int values, CN1CollectionView* out) {
    // Every branch below reads owner->__codenameOneParentClsReference, so a null owner
    // is a crash rather than a miss. 0 means "no layout I recognise", which is exactly
    // what a caller should do with one.
    if(owner == JAVA_NULL) return 0;
#ifdef CN1_COLL_HASHMAP
    if(owner->__codenameOneParentClsReference == &class__java_util_HashMap
#ifdef CN1_COLL_LINKED
       || owner->__codenameOneParentClsReference == &class__java_util_LinkedHashMap
#endif
    ) {
        struct obj__java_util_HashMap* map = (struct obj__java_util_HashMap*)owner;
        out->owner = owner;
        JAVA_LONG table = map->java_util_HashMap_cn1KeysBlock;
        out->data = (JAVA_OBJECT*)(uintptr_t)cn1TablePart(table, values ? 1 : 0);
        out->metadata = (JAVA_INT*)(uintptr_t)cn1TablePart(table, 2);
        out->capacity = map->java_util_HashMap_cn1Cap;
        out->count = map->java_util_HashMap_elementCount;
        out->kind = CN1_COLL_HASH;
#ifdef CN1_COLL_LINKED
        if(owner->__codenameOneParentClsReference == &class__java_util_LinkedHashMap) {
            struct obj__java_util_LinkedHashMap* ordered = (struct obj__java_util_LinkedHashMap*)owner;
            out->kind = CN1_COLL_ORDERED;
            out->first = ordered->java_util_LinkedHashMap_cn1Head;
            out->links = (JAVA_INT*)(uintptr_t)cn1TablePart(table, 4);
        }
#endif
        return 1;
    }
#endif
#ifdef CN1_COLL_IDENTITY
    if(owner->__codenameOneParentClsReference == &class__java_util_IdentityHashMap) {
        struct obj__java_util_IdentityHashMap* map = (struct obj__java_util_IdentityHashMap*)owner;
        JAVA_LONG block = map->java_util_IdentityHashMap_elementData;
        out->owner = owner;
        out->data = (JAVA_OBJECT*)(uintptr_t)block;
        out->capacity = cn1RefBlockCount(block);
        out->count = map->java_util_IdentityHashMap_size;
        out->nullObject = get_static_java_util_IdentityHashMap_NULL_OBJECT();
        out->valueOffset = values;
        out->kind = CN1_COLL_ID;
        return 1;
    }
#endif
    return 0;
}

// Exact implementation guards preserve overridden Collection/toArray contracts.
// The returned pointers are borrowed; the caller must keep the source owner live.
static inline int cn1CollectionOpen(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT collection,
                                    CN1CollectionView* out) {
    if(collection == JAVA_NULL) return 0;
#ifdef CN1_COLL_SET_FROM_MAP
    if(collection->__codenameOneParentClsReference == &class__java_util_Collections_SetFromMap)
        collection = ((struct obj__java_util_Collections_SetFromMap*)collection)->java_util_Collections_SetFromMap_backingSet;
#endif
    if(collection == JAVA_NULL) return 0;
#ifdef CN1_COLL_ARRAYLIST
    if(collection->__codenameOneParentClsReference == &class__java_util_ArrayList) {
        struct obj__java_util_ArrayList* list = (struct obj__java_util_ArrayList*)collection;
        out->owner = collection;
        out->kind = CN1_COLL_DENSE;
        out->data = (JAVA_OBJECT*)(uintptr_t)list->java_util_ArrayList_cn1Storage;
        out->count = list->java_util_ArrayList_size;
        return 1;
    }
#endif
#ifdef CN1_COLL_ARRAY_VIEW
    if(collection->__codenameOneParentClsReference == &class__java_util_Arrays_ArrayList) {
        JAVA_ARRAY array = (JAVA_ARRAY)((struct obj__java_util_Arrays_ArrayList*)collection)->java_util_Arrays_ArrayList_a;
        out->owner = collection;
        out->kind = CN1_COLL_DENSE;
        out->data = (JAVA_OBJECT*)CN1_ARRAY_DATA(array);
        out->count = array->length;
        return 1;
    }
#endif
#ifdef CN1_COLL_KEYS
    if(collection->__codenameOneParentClsReference == &class__java_util_HashMap_KeySet)
        return cn1CollectionMap(threadStateData, ((struct obj__java_util_HashMap_KeySet*)collection)->java_util_HashMap_KeySet_map, 0, out);
#endif
#ifdef CN1_COLL_VALUES
    if(collection->__codenameOneParentClsReference == &class__java_util_HashMap_Values)
        return cn1CollectionMap(threadStateData, ((struct obj__java_util_HashMap_Values*)collection)->java_util_HashMap_Values_map, 1, out);
#endif
#ifdef CN1_COLL_IDENTITY_KEYS
    if(collection->__codenameOneParentClsReference == &class__java_util_IdentityHashMap_KeySet)
        return cn1CollectionMap(threadStateData, ((struct obj__java_util_IdentityHashMap_KeySet*)collection)->java_util_IdentityHashMap_KeySet_map, 0, out);
#endif
#ifdef CN1_COLL_IDENTITY_VALUES
    if(collection->__codenameOneParentClsReference == &class__java_util_IdentityHashMap_Values)
        return cn1CollectionMap(threadStateData, ((struct obj__java_util_IdentityHashMap_Values*)collection)->java_util_IdentityHashMap_Values_map, 1, out);
#endif
#ifdef CN1_COLL_SET
    if(collection->__codenameOneParentClsReference == &class__java_util_HashSet) {
        // A plain HashSet owns a keys+meta table of its own -- it stopped renting a
        // HashMap -- and that table is the SAME shape cn1CollectionMap describes for a
        // map: an occupied slot is one whose marker has the sign bit set, which is the
        // single encoding cn1HmMarker produces for both. So it is opened directly here
        // rather than reported as an unknown layout.
        //
        // It used to be exactly that, an unknown layout, because the branch was
        // written when a HashSet still delegated and the field it read was gone. The
        // cost was a real iterator object per traversal: 517,043 HashSetIterator
        // allocations over a translation of the 5,326-class corpus, for loops that
        // need no object at all.
        //
        // A set has keys only; cn1CollectionOpen has no values mode to answer.
        struct obj__java_util_HashSet* set = (struct obj__java_util_HashSet*)collection;
        out->owner = collection;
        out->data = (JAVA_OBJECT*)(uintptr_t)set->java_util_HashSet_cn1KeysBlock;
        out->metadata = (JAVA_INT*)(uintptr_t)cn1SetTableMeta(set->java_util_HashSet_cn1KeysBlock);
        out->capacity = set->java_util_HashSet_cn1Cap;
        out->count = set->java_util_HashSet_cn1Size;
        out->kind = CN1_COLL_HASH;
        // A set that has never had an element added has no table at all. Report it
        // as an unrecognised layout rather than a view over null pointers -- there is
        // nothing to walk, and every caller already handles the miss.
        if(out->data == NULL || out->metadata == NULL) return 0;
        return 1;
    }
#endif
#ifdef CN1_COLL_ORDERED_SET
    if(collection->__codenameOneParentClsReference == &class__java_util_LinkedHashSet) {
        JAVA_OBJECT backing = ((struct obj__java_util_LinkedHashSet*)collection)->java_util_LinkedHashSet_backingMap;
        if(backing == JAVA_NULL) return 0;
        return cn1CollectionMap(threadStateData, backing, 0, out);
    }
#endif
    return 0;
}

static inline void cn1CollectionCopy(const CN1CollectionView* source, JAVA_OBJECT* destination) {
    int count = source->count;
    if(count == 0) return;
    switch(source->kind) {
        case CN1_COLL_DENSE:
            memcpy(destination, source->data, (size_t)count * sizeof(JAVA_OBJECT));
            break;
        case CN1_COLL_HASH:
            for(int slot = 0, n = 0; slot < source->capacity && n < count; slot++)
                if(source->metadata[slot] < 0) destination[n++] = source->data[slot];
            break;
        case CN1_COLL_ORDERED:
            for(int slot = source->first, n = 0; n < count; slot = source->links[slot])
                destination[n++] = source->data[slot];
            break;
        case CN1_COLL_ID:
            for(int slot = 0, n = 0; slot < source->capacity && n < count; slot += 2) {
                if(source->data[slot] != JAVA_NULL) {
                    JAVA_OBJECT value = source->data[slot + source->valueOffset];
                    destination[n++] = value == source->nullObject ? JAVA_NULL : value;
                }
            }
            break;
    }
}
#endif
