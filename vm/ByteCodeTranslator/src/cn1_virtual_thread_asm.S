/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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

/*
 * Symbol naming: Mach-O prefixes C symbols with an underscore and ELF does not,
 * so every name here goes through CN1_SYM. Getting this wrong does not warn --
 *
 * APPLE TARGETS DO NOT BUILD THIS FILE YET, and the reason is the build system
 * rather than the assembly. This is the first .S the translator has ever emitted,
 * and three separate places classify sources by extension without knowing it:
 *
 *   1. WatchNativeBuilder copied watch sources by extension (.m .c .swift .mm
 *      .cpp .cc) and dropped the .S entirely -- FIXED, it now copies .S/.s and
 *      the watch target links.
 *   2. The Xcode project generator files a .S as `lastKnownFileType = file` and
 *      puts it in the RESOURCES phase, so the phone target ships it as a resource
 *      and never assembles it. `nm` on the build output shows the watch target
 *      with `T _cn1VirtualThreadSwitch` and the phone target with no asm object
 *      at all. IPhoneBuilder already strips a similar misfiling for Swift
 *      (`removeLinesContaining(pbx, ".swift in Resources", ...)`), so the same
 *      treatment is the shape of the fix -- except a Resources entry has to be
 *      MOVED to Sources, not just deleted.
 *   3. Nothing else in the repo emits assembly, so no existing gate covers it.
 *
 * The alternative worth weighing before doing (2): drop this file and put the
 * same instructions in a top-level __asm__ block inside cn1_virtual_thread.c.
 * That removes the new file TYPE from the pipeline entirely, so every builder
 * that already compiles the .c gets the switch for free and none of them need to
 * learn about .S. It needs re-verifying on Linux (musl and glibc, both arches)
 * and on Apple, which is why it is written down here rather than done in haste.
 * it fails to link, or worse, links against nothing on a platform where the
 * caller is also assembly.
 */
/* This is preprocessed assembly, so it cannot include cn1_virtual_thread.h -- the
 * header's C declarations would not assemble. The predicate is therefore repeated
 * here and MUST stay identical to the one in that header: if the two disagree the C
 * side calls a switch this file did not define, and the link fails naming
 * _cn1VirtualThreadSwitch. */
#if !defined(CN1_VIRTUAL_THREADS) && !defined(CN1_DISABLE_VIRTUAL_THREADS) \
        && !defined(_WIN32) && (defined(__aarch64__) || defined(__x86_64__))
#define CN1_VIRTUAL_THREADS 1
#endif

#ifdef CN1_VIRTUAL_THREADS

#if defined(__APPLE__)
#define CN1_SYM(name) _##name
#else
#define CN1_SYM(name) name
#endif

/*
 * The entire architecture-specific surface of the virtual thread mechanism: save the
 * callee-saved registers, swap the stack pointer, restore. Caller-saved
 * registers are not touched because the compiler already treats a call as
 * clobbering them, and every entry point here is reached by an ordinary call.
 *
 * Three symbols per architecture:
 *   cn1VirtualThreadSwitch(void** saveSp, void* newSp)  suspend here, resume there
 *   cn1VirtualThreadPrime(high, co, trampoline)         build the first frame
 *   cn1VirtualThreadTrampoline                          where that frame returns to
 */

#if defined(__aarch64__)

    .text
    .align 2
    .globl CN1_SYM(cn1VirtualThreadSwitch)
CN1_SYM(cn1VirtualThreadSwitch):
    /* x19-x28 are callee-saved, d8-d15 are the callee-saved halves of the FP
     * registers, x29/x30 are the frame pointer and link register. */
    stp x29, x30, [sp, #-160]!
    stp x19, x20, [sp, #16]
    stp x21, x22, [sp, #32]
    stp x23, x24, [sp, #48]
    stp x25, x26, [sp, #64]
    stp x27, x28, [sp, #80]
    stp d8,  d9,  [sp, #96]
    stp d10, d11, [sp, #112]
    stp d12, d13, [sp, #128]
    stp d14, d15, [sp, #144]
    mov x2, sp
    str x2, [x0]                /* *saveSp = sp */
    mov sp, x1                  /* sp = newSp */
    ldp d14, d15, [sp, #144]
    ldp d12, d13, [sp, #128]
    ldp d10, d11, [sp, #112]
    ldp d8,  d9,  [sp, #96]
    ldp x27, x28, [sp, #80]
    ldp x25, x26, [sp, #64]
    ldp x23, x24, [sp, #48]
    ldp x21, x22, [sp, #32]
    ldp x19, x20, [sp, #16]
    ldp x29, x30, [sp], #160
    ret

    .align 2
    .globl CN1_SYM(cn1VirtualThreadPrime)
CN1_SYM(cn1VirtualThreadPrime):
    /* x0 = stack high, x1 = virtual thread, x2 = trampoline.
     * Build a frame cn1VirtualThreadSwitch can restore: the virtual thread travels in
     * x19 (callee-saved, so the trampoline still has it) and the link register
     * points at the trampoline. */
    and x0, x0, #~15            /* the ABI wants 16-byte alignment */
    sub x0, x0, #160
    stp xzr, x2,  [x0]          /* x29 = 0 ends any backtrace, x30 = trampoline */
    stp x1,  xzr, [x0, #16]     /* x19 = virtual thread */
    stp xzr, xzr, [x0, #32]
    stp xzr, xzr, [x0, #48]
    stp xzr, xzr, [x0, #64]
    stp xzr, xzr, [x0, #80]
    stp xzr, xzr, [x0, #96]
    stp xzr, xzr, [x0, #112]
    stp xzr, xzr, [x0, #128]
    stp xzr, xzr, [x0, #144]
    ret

    .align 2
    .globl CN1_SYM(cn1VirtualThreadTrampoline)
CN1_SYM(cn1VirtualThreadTrampoline):
    mov x0, x19                 /* the virtual thread cn1VirtualThreadPrime parked here */
    bl  CN1_SYM(cn1VirtualThreadMain)
    brk #0                      /* cn1VirtualThreadMain never returns */

#elif defined(__x86_64__)

    .text
    .globl CN1_SYM(cn1VirtualThreadSwitch)
CN1_SYM(cn1VirtualThreadSwitch):
    /* rbx, rbp, r12-r15 are callee-saved in the SysV ABI. */
    pushq %rbp
    pushq %rbx
    pushq %r12
    pushq %r13
    pushq %r14
    pushq %r15
    movq %rsp, (%rdi)           /* *saveSp = rsp */
    movq %rsi, %rsp             /* rsp = newSp */
    popq %r15
    popq %r14
    popq %r13
    popq %r12
    popq %rbx
    popq %rbp
    ret

    .globl CN1_SYM(cn1VirtualThreadPrime)
CN1_SYM(cn1VirtualThreadPrime):
    /* rdi = stack high, rsi = virtual thread, rdx = trampoline. */
    andq $-16, %rdi
    subq $8, %rdi               /* so that rsp+8 is 16-aligned after the ret */
    movq %rdx, (%rdi)           /* the address cn1VirtualThreadSwitch's ret jumps to */
    subq $48, %rdi
    movq $0,   (%rdi)           /* r15 */
    movq $0,  8(%rdi)           /* r14 */
    movq $0, 16(%rdi)           /* r13 */
    movq $0, 24(%rdi)           /* r12 */
    movq %rsi, 32(%rdi)         /* rbx = virtual thread */
    movq $0, 40(%rdi)           /* rbp = 0 ends any backtrace */
    movq %rdi, %rax
    ret

    .globl CN1_SYM(cn1VirtualThreadTrampoline)
CN1_SYM(cn1VirtualThreadTrampoline):
    movq %rbx, %rdi             /* the virtual thread cn1VirtualThreadPrime parked here */
    call CN1_SYM(cn1VirtualThreadMain)
    ud2                         /* cn1VirtualThreadMain never returns */

#else
#error "cn1_virtual thread: no switch implementation for this architecture"
#endif

#if defined(__linux__) && defined(__ELF__)
/* Do not ask for an executable stack on account of this file. */
.section .note.GNU-stack,"",%progbits
#endif

#endif /* CN1_VIRTUAL_THREADS */
