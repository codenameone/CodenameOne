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
 *
 * ---------------------------------------------------------------------------
 * Object-reference validation for the on-device debugger.
 *
 * No reference reaching the debugger is trustworthy. The IDE echoes back
 * objectIDs it was handed earlier — which may since have been collected — and
 * a local slot can hold whatever the frame left there on a branch that never
 * ran. Dereferencing one of those crashes the app in the middle of a debugging
 * session, which is the "We had a signal 11" in issue #5333. A debugger is
 * allowed to say "unavailable"; it is not allowed to take the process down.
 *
 * Kept as plain C, separate from cn1_debugger.m, so the policy can be compiled
 * and exercised on a host rather than only on a device.
 * ---------------------------------------------------------------------------
 */

#include "cn1_globals.h"

#ifdef CN1_ON_DEVICE_DEBUG

#include <pthread.h>
#include <stdlib.h>
#include <string.h>
#include <stdint.h>
#include <mach/mach.h>

#define CN1_CLASS_REG_INITIAL_CAP 2048

static struct clazz** g_classById = NULL;
static int g_classByIdCap = 0;
static pthread_mutex_t g_classRegMutex = PTHREAD_MUTEX_INITIALIZER;

void cn1_debugger_register_class(int classId, struct clazz* cls) {
    if (classId < 0 || cls == NULL) return;
    pthread_mutex_lock(&g_classRegMutex);
    if (classId >= g_classByIdCap) {
        int newCap = g_classByIdCap == 0 ? CN1_CLASS_REG_INITIAL_CAP : g_classByIdCap * 2;
        while (classId >= newCap) newCap *= 2;
        struct clazz** n = (struct clazz**)realloc(g_classById,
                newCap * sizeof(struct clazz*));
        if (!n) { pthread_mutex_unlock(&g_classRegMutex); return; }
        memset(n + g_classByIdCap, 0,
               (newCap - g_classByIdCap) * sizeof(struct clazz*));
        g_classById = n;
        g_classByIdCap = newCap;
    }
    g_classById[classId] = cls;
    pthread_mutex_unlock(&g_classRegMutex);
}

/**
 * Copies len bytes from a possibly-invalid address, returning 0 instead of
 * faulting when the address is not mapped.
 *
 * vm_read_overwrite has the kernel perform the copy, so an unmapped or
 * protected page comes back as a kern_return_t rather than a signal on this
 * thread. The cheap rejections come first — the null page and misaligned
 * pointers cover the overwhelmingly common garbage, which is small integers
 * read out of a slot that actually holds a primitive — so the syscall is only
 * paid for candidates that could plausibly be objects.
 */
int cn1_debugger_safe_read(const void* addr, void* dst, size_t len) {
    if (addr == NULL || dst == NULL || len == 0) return 0;
    uintptr_t a = (uintptr_t)addr;
    if (a < 0x1000) return 0;
    if ((a & (sizeof(void*) - 1)) != 0) return 0;
    vm_size_t got = 0;
    kern_return_t kr = vm_read_overwrite(mach_task_self(),
                                         (vm_address_t)a,
                                         (vm_size_t)len,
                                         (vm_address_t)dst,
                                         &got);
    return kr == KERN_SUCCESS && got == (vm_size_t)len;
}

/** Whether cls is the exact clazz registered for the classId it reports. */
static int cn1_is_registered_class(const struct clazz* cls, int classId) {
    return cls != NULL
        && classId >= 0
        && classId < g_classByIdCap
        && g_classById != NULL
        && g_classById[classId] == cls;
}

/**
 * Returns obj's clazz, or NULL when obj is not a plausible Java object.
 *
 * Reads the class word without dereferencing obj, then requires that word to
 * point at a clazz that identifies itself. A fabricated pointer would have to
 * address readable memory whose first word addresses a readable clazz that
 * agrees with the registry about its own id — which random stack or heap bytes
 * do not.
 *
 * Array classes take the second branch. They are synthesised per dimension and
 * the primitive ones live in the runtime rather than in generated code, so
 * none of them run the registration constructor. An array clazz still names
 * its component type, and that component IS a registered class — which makes
 * the check just as exact without a second registry.
 */
struct clazz* cn1_debugger_class_of(JAVA_OBJECT obj) {
    if (obj == JAVA_NULL) return NULL;
    // A tagged int is a value, not an address: Integer.valueOf() returns
    // (v << 1) | 1 on every 64-bit target, which is the shipping iOS shape.
    // It has no header to read, and the alignment rejection below would
    // otherwise discard every boxed Integer the debugger was asked about.
    if (CN1_IS_TAGGED(obj)) {
        return &class__java_lang_Integer;
    }
    struct clazz* cls = NULL;
    if (!cn1_debugger_safe_read(&obj->__codenameOneParentClsReference, &cls, sizeof(cls))) {
        return NULL;
    }
    if (cls == NULL) return NULL;
    // Copied into raw bytes rather than a "struct clazz" local because the
    // struct has const-qualified members, which a local cannot be filled in
    // through.
    _Alignas(struct clazz) unsigned char raw[sizeof(struct clazz)];
    if (!cn1_debugger_safe_read(cls, raw, sizeof(raw))) return NULL;
    const struct clazz* header = (const struct clazz*)raw;
    if (cn1_is_registered_class(cls, header->classId)) {
        return cls;
    }
    if (header->isArray && header->arrayType != NULL) {
        _Alignas(struct clazz) unsigned char componentRaw[sizeof(struct clazz)];
        if (!cn1_debugger_safe_read(header->arrayType, componentRaw, sizeof(componentRaw))) {
            return NULL;
        }
        const struct clazz* component = (const struct clazz*)componentRaw;
        if (cn1_is_registered_class(header->arrayType, component->classId)) {
            return cls;
        }
    }
    return NULL;
}

int cn1_debugger_is_valid_object(JAVA_OBJECT obj) {
    return cn1_debugger_class_of(obj) != NULL;
}

/**
 * The value carried by a tagged int, for callers that must not treat it as an
 * address. Returns 0 for anything else, so a caller that has already checked
 * {@code cn1_debugger_is_tagged_int} reads the real value.
 */
int cn1_debugger_is_tagged_int(JAVA_OBJECT obj) {
    return obj != JAVA_NULL && CN1_IS_TAGGED(obj);
}

JAVA_INT cn1_debugger_tagged_int_value(JAVA_OBJECT obj) {
#if CN1_TAGGED_ACTIVE
    return CN1_IS_TAGGED(obj) ? CN1_UNTAG_INT(obj) : 0;
#else
    // Tagged ints are compiled out on 32-bit targets and under
    // -DCN1_DISABLE_TAGGED_INT, which also leaves CN1_UNTAG_INT undefined.
    // Every reference is then a real object and no caller reaches this.
    (void)obj;
    return 0;
#endif
}


/* --------------------------------------------------------------------- */
/* Object ids the debugger has handed out during the current suspension.  */
/*                                                                        */
/* A registered class word proves a pointer LOOKS like an object of a     */
/* known class. It does not prove the allocation is still live: full-page */
/* BiBOP reclamation resets the page cursor without clearing class words, */
/* and a legacy object goes to free() with its header intact. An IDE that */
/* holds an objectID across a resume and asks about it after a collection */
/* would therefore pass validation and be read anyway.                    */
/*                                                                        */
/* Every reference handed to the proxy is recorded here, and the set is   */
/* cleared whenever a thread resumes. A wire id that is not in it is one  */
/* the debugger did not issue this time round, so it is refused rather    */
/* than dereferenced.                                                     */
/*                                                                        */
/* This bounds the reported window; it is not a liveness proof. Objects   */
/* the IDE is shown come from live frames, fields and arrays and so are   */
/* reachable, but the concurrent collector can still run while a thread   */
/* is parked. Closing that completely means rooting debugger-issued ids   */
/* until the IDE disposes them, which is a change to the collector rather */
/* than to this file.                                                     */
/* --------------------------------------------------------------------- */

#define CN1_ISSUED_INITIAL_CAP 4096u   /* power of two; open addressing */
#define CN1_ISSUED_MAX_OWNERS 4

/*
 * One entry per reference the debugger has handed to the proxy, carrying the
 * set of suspended threads it was obtained for.
 *
 * A set rather than a single owner because two stopped threads can expose the
 * same reference -- a shared singleton in both their locals, say. Keeping only
 * the first meant resuming that thread dropped the id while the other was
 * still parked and inspecting through it.
 *
 * Owner 0 means "not tied to a suspension" -- the java.lang.Thread objects the
 * thread list hands over -- and those survive until every thread runs again.
 */
struct issued_entry {
    uintptr_t key;
    int64_t owners[CN1_ISSUED_MAX_OWNERS];
    unsigned char ownerCount;
    /*
     * Set when more owners appeared than there are slots. Such an entry is
     * dropped as soon as any owner resumes, because we can no longer prove
     * another still holds it: reporting a reference as unavailable is a
     * display problem, accepting one whose object may have been reclaimed is
     * a read of freed memory.
     */
    unsigned char ownerOverflow;
};

static struct issued_entry* g_issued = NULL;
static unsigned g_issuedCap = 0;
static unsigned g_issuedCount = 0;
static pthread_mutex_t g_issuedMutex = PTHREAD_MUTEX_INITIALIZER;

static unsigned issued_slot(uintptr_t key, unsigned cap) {
    /* Pointers are aligned, so the low bits carry no entropy. */
    uintptr_t h = key >> 3;
    h ^= h >> 13;
    return (unsigned)(h & (cap - 1));
}

/* Adds an owner to an entry, ignoring duplicates. */
static void entry_add_owner(struct issued_entry* e, int64_t owner) {
    if (owner == 0) return;   /* unowned: nothing to record */
    for (unsigned i = 0; i < e->ownerCount; i++) {
        if (e->owners[i] == owner) return;
    }
    if (e->ownerCount < CN1_ISSUED_MAX_OWNERS) {
        e->owners[e->ownerCount++] = owner;
    } else {
        e->ownerOverflow = 1;
    }
}

/* Finds or creates the entry for key in a table known to have room. */
static struct issued_entry* issued_put(struct issued_entry* table, unsigned cap,
                                       uintptr_t key, int* created) {
    unsigned i = issued_slot(key, cap);
    for (unsigned n = 0; n < cap; n++) {
        unsigned at = (i + n) & (cap - 1);
        if (table[at].key == key) { *created = 0; return &table[at]; }
        if (table[at].key == 0) {
            memset(&table[at], 0, sizeof(table[at]));
            table[at].key = key;
            *created = 1;
            return &table[at];
        }
    }
    *created = 0;
    return NULL;
}

/* Grows past a 3/4 load factor. Returns 0 only if the allocation fails. */
static int issued_reserve(void) {
    if (g_issued != NULL && (g_issuedCount + 1) * 4 < g_issuedCap * 3) {
        return 1;
    }
    unsigned newCap = g_issuedCap ? g_issuedCap * 2 : CN1_ISSUED_INITIAL_CAP;
    struct issued_entry* grown =
        (struct issued_entry*)calloc(newCap, sizeof(struct issued_entry));
    if (!grown) return 0;
    for (unsigned i = 0; i < g_issuedCap; i++) {
        if (g_issued[i].key != 0) {
            int created = 0;
            struct issued_entry* moved =
                issued_put(grown, newCap, g_issued[i].key, &created);
            if (moved) *moved = g_issued[i];
        }
    }
    free(g_issued);
    g_issued = grown;
    g_issuedCap = newCap;
    return 1;
}

int cn1_debugger_note_issued_for(JAVA_OBJECT obj, int64_t owner) {
    if (obj == JAVA_NULL) return 1;   /* null needs no record */
    int ok;
    pthread_mutex_lock(&g_issuedMutex);
    ok = issued_reserve();
    if (ok) {
        int created = 0;
        struct issued_entry* e =
            issued_put(g_issued, g_issuedCap, (uintptr_t)obj, &created);
        if (e) {
            entry_add_owner(e, owner);
            g_issuedCount += (unsigned)created;
        } else {
            ok = 0;
        }
    }
    pthread_mutex_unlock(&g_issuedMutex);
    /* The table grows, so this only fails when the allocation does. A caller
     * that cannot record a reference must report null rather than hand over an
     * id that every later request would refuse -- a reference the IDE can see
     * but cannot expand is worse than one it never saw. */
    return ok;
}

int cn1_debugger_note_issued(JAVA_OBJECT obj) {
    return cn1_debugger_note_issued_for(obj, 0);
}

/* Locates an entry, or NULL. Caller holds the mutex. */
static struct issued_entry* issued_find(uintptr_t key) {
    if (g_issued == NULL) return NULL;
    unsigned i = issued_slot(key, g_issuedCap);
    for (unsigned n = 0; n < g_issuedCap; n++) {
        unsigned at = (i + n) & (g_issuedCap - 1);
        if (g_issued[at].key == key) return &g_issued[at];
        if (g_issued[at].key == 0) return NULL;
    }
    return NULL;
}

int cn1_debugger_was_issued(JAVA_OBJECT obj) {
    if (obj == JAVA_NULL) return 0;
    pthread_mutex_lock(&g_issuedMutex);
    int found = issued_find((uintptr_t)obj) != NULL;
    pthread_mutex_unlock(&g_issuedMutex);
    return found;
}

int64_t cn1_debugger_owner_of(JAVA_OBJECT obj) {
    if (obj == JAVA_NULL) return 0;
    pthread_mutex_lock(&g_issuedMutex);
    struct issued_entry* e = issued_find((uintptr_t)obj);
    int64_t owner = (e != NULL && e->ownerCount > 0) ? e->owners[0] : 0;
    pthread_mutex_unlock(&g_issuedMutex);
    return owner;
}

void cn1_debugger_forget_issued(void) {
    pthread_mutex_lock(&g_issuedMutex);
    if (g_issued != NULL) {
        memset(g_issued, 0, g_issuedCap * sizeof(struct issued_entry));
    }
    g_issuedCount = 0;
    pthread_mutex_unlock(&g_issuedMutex);
}

/* Whether an entry survives the given owner resuming. */
static int entry_survives_resume(struct issued_entry* e, int64_t owner) {
    if (e->ownerCount == 0) return 1;      /* unowned: only a full clear drops it */
    if (e->ownerOverflow) return 0;        /* cannot prove another owner remains */
    unsigned remaining = 0;
    for (unsigned i = 0; i < e->ownerCount; i++) {
        if (e->owners[i] != owner) {
            e->owners[remaining++] = e->owners[i];
        }
    }
    e->ownerCount = (unsigned char)remaining;
    return remaining > 0;                  /* another suspension still holds it */
}

void cn1_debugger_forget_issued_for(int64_t owner) {
    if (owner == 0) return;
    pthread_mutex_lock(&g_issuedMutex);
    if (g_issued != NULL) {
        /* Rebuilt rather than tombstoned: deletion from a linear probe would
         * otherwise cut the chains that later lookups walk. */
        unsigned cap = g_issuedCap;
        struct issued_entry* kept =
            (struct issued_entry*)calloc(cap, sizeof(struct issued_entry));
        if (kept) {
            unsigned keptCount = 0;
            for (unsigned i = 0; i < cap; i++) {
                if (g_issued[i].key == 0) continue;
                if (!entry_survives_resume(&g_issued[i], owner)) continue;
                int created = 0;
                struct issued_entry* moved =
                    issued_put(kept, cap, g_issued[i].key, &created);
                if (moved) { *moved = g_issued[i]; keptCount += (unsigned)created; }
            }
            free(g_issued);
            g_issued = kept;
            g_issuedCount = keptCount;
        } else {
            /* Out of memory: drop everything rather than keep ids we can no
             * longer prove belong to a still-parked thread. */
            memset(g_issued, 0, cap * sizeof(struct issued_entry));
            g_issuedCount = 0;
        }
    }
    pthread_mutex_unlock(&g_issuedMutex);
}

/**
 * Resolves an objectID that arrived from the IDE.
 *
 * Stricter than cn1_debugger_class_of, which answers "is this shaped like a
 * live object" for a value the runtime just read itself. A wire id has also to
 * be one this debugger issued since the last resume.
 */
struct clazz* cn1_debugger_class_of_wire_id(JAVA_OBJECT obj) {
    if (obj == JAVA_NULL) return NULL;
    /* A tagged int is a value; there is no allocation to outlive anything. */
    if (CN1_IS_TAGGED(obj)) return cn1_debugger_class_of(obj);
    if (!cn1_debugger_was_issued(obj)) return NULL;
    return cn1_debugger_class_of(obj);
}

/**
 * Whether a local is in scope at a given source line.
 *
 * {0, 0} is "always live" — the class file carried no scope for it, or the
 * translator synthesised it from a store opcode. A zero endLine means the
 * scope runs to the end of the method. A line of 0 (the frame has not recorded
 * one yet) shows everything rather than nothing, so a stop before the first
 * line-number store still lists locals.
 *
 * Without this, a slot two disjoint scopes share reports both occupants at
 * every breakpoint, and the one the code has not reached displays whatever the
 * other scope left in its own storage.
 */
int cn1_debugger_var_in_scope(const struct cn1_var_entry* v, int line) {
    if (v == NULL) return 0;
    if (v->startLine <= 0) return 1;
    if (line <= 0) return 1;
    if (line < v->startLine) return 0;
    if (v->endLine > 0 && line >= v->endLine) return 0;
    return 1;
}

#endif // CN1_ON_DEVICE_DEBUG
