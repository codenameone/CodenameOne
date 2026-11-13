---
title: "Real-Time Java: Control Loops the Garbage Collector Never Stops"
slug: real-time-java-control-loops
url: /blog/real-time-java-control-loops/
date: '2026-11-09'
author: Shai Almog
description: "What real time actually means, why a garbage collected language has trouble with it, and how Codename One runs a 1 kHz Java control loop on PREEMPT_RT Linux with a collector that never stops it and a build that fails when a cycle allocates."
feed_html: "<img src=\"https://www.codenameone.com/blog/real-time-java-control-loops.jpg\" alt=\"A stopwatch over a periodic timeline\" /> What real time means, why garbage collection makes it hard, and how Codename One runs a 1 kHz Java control loop that the collector never stops."
series: ["release-2026-11-06"]
---

![A stopwatch over a periodic timeline](/blog/real-time-java-control-loops.jpg)

In Friday's {{< post-link path="/blog/embedded-linux-ui-in-18-mb" text="embedded Linux post" >}} I promised the hardcore part. A device with a touchscreen is one thing. A device that drives a motor, reads a force sensor or balances something is another, because the code that does that has deadlines. Miss one and the motor stutters, the reading is stale, or the thing falls over.

The standard advice is that you can't write that code in Java. The garbage collector will stop your thread at the worst moment. This post explains what real time actually requires, why that advice is usually right, how real-time JVMs like JamaicaVM and the RTSJ got around it, and the approach we took because we own the VM.

## What "real time" means

Real time doesn't mean fast. It means *on time, every time*. A loop that usually takes 50 microseconds and occasionally takes 30 milliseconds is fast and useless. A loop that always finishes within 900 microseconds of a 1-millisecond period is slow and perfect.

A few terms you'll see everywhere:

- **Period.** How often the loop runs. A motor drive might run every millisecond (1 kHz).
- **Deadline.** When the work for one period has to be done, usually by the start of the next.
- **Release latency.** How late the thread actually woke up compared to when it was scheduled. This is what the operating system and the runtime add.
- **Jitter.** How much that latency varies. Steady lateness can be compensated for; jitter can't.
- **Worst case.** The only number that matters. An average of 5 microseconds with a worst case of 20 milliseconds is a 20 millisecond system.

Then the three flavors:

- **Hard real time.** A missed deadline is a failure, possibly a dangerous one. Airbags, flight controls, pacemakers. These are certified against standards like IEC 61508, ISO 26262 or DO-178C, and usually run on an RTOS.
- **Firm real time.** A missed deadline makes that result worthless, but the system survives. A motor controller that holds its last output for one cycle. Most industrial and robotics loops live here.
- **Soft real time.** Late results are worth less but still worth something. Audio, video, games.

We target **firm** real time on a PREEMPT_RT Linux kernel. That's an important line, so I'll say it plainly: this is not a certified hard real-time system, and a loop whose failure can hurt someone still needs an independent safety mechanism like a hardware watchdog or an interlock. If you need hard, certified real time, that's work we'd like to do with you, but it isn't what shipped this week.

## Why this is hard for a high-level language

A C programmer on PREEMPT_RT gets a lot for free. Set `SCHED_FIFO`, lock memory, pin a core, don't call `malloc` in the loop. The kernel does the rest, and since kernel 6.12 PREEMPT_RT is part of mainline Linux.

A managed language adds four problems on top of that:

1. **The garbage collector.** Most collectors stop every thread at some point, if only to read their stacks. For a few milliseconds your control loop simply doesn't exist.
2. **Allocation.** Every `new` adds work for the collector, and an allocating thread can be told to wait while it catches up. String concatenation, boxing an `int` into an `Integer` and collections of boxed numbers all allocate where you don't see it.
3. **The JIT.** On a JVM, the first few thousand runs of your loop are interpreted, then compiled, sometimes thrown away and compiled again. Each of those moments costs time you didn't budget for.
4. **Hidden slow paths.** Initializing a class on first use, building a stack trace for an exception, a page fault on memory touched for the first time. All legal Java, all disastrous in a 1 ms budget.

Then there's **priority inversion**, which hits every language: a low priority thread holds a lock the real-time thread needs, a medium priority thread preempts the holder, and the real-time thread waits on something that isn't even running.

## How we solve it

{{< mermaid >}}
flowchart TB
    A[Your RealtimeTask] --> B{Build time}
    B -->|new, arrays, boxing, string concat in cycle| X[Build fails, names method and line]
    B -->|clean| C[ParparVM: ahead-of-time C, no JIT]
    C --> D[SCHED_FIFO thread, pinned core, mlock]
    D --> E{Every period}
    E --> F[cycle runs on an absolute clock]
    G[Garbage collector] -.->|releases RT thread at stack scan, no stop-the-world phases| F
    F -->|allocation the build could not see| Y[RealtimeAllocationError, thread fails safely]
{{< /mermaid >}}

Each problem gets its own answer:

**No JIT.** Codename One already compiles your Java ahead of time to C through ParparVM. The code that runs in period one is the code that runs in period one million.

**The collector never stops a real-time thread.** An ordinary thread pauses for a moment while the collector reads its stack. A real-time thread is released immediately, and its references stay visible to the collector through the write barrier. While any real-time thread runs, the collector never enters its stop-the-world phases. This only works because we own the VM.

**Allocation fails the build.** The translator follows every call `cycle()` can make and fails the build on `new`, array creation, boxing and string concatenation, naming the method and the line. An allocation you mean to make, on a fault path for example, gets `@AllocationAllowed`. Anything the build can't see, like a call through an interface with several implementations, is caught at run time and stops the thread with a `RealtimeAllocationError`. Catching that error doesn't switch the check off.

**Priority inheritance.** Monitors use priority-inheriting mutexes on Linux, so a low priority lock holder is raised until it lets go. Better still, hand data across through preallocated buffers and `java.util.concurrent.atomic`, and don't share a lock at all.

**Memory and cores.** `Realtime.lockMemory()` locks the process into RAM so nothing pages out mid-cycle. Pinning a thread to a core moves the process's other threads off it by default. `Realtime.setMemoryLimit()` caps the process's resident memory. Real-time threads are exempt because they don't allocate, so a leak in the UI makes an ordinary allocation fail fast instead of slowing everything down while the collector struggles.

## The code

Here's a 1 kHz motor loop. Everything it needs is allocated before the first period, `setup()` may allocate, and `cycle()` may not:

```java {class="cn1-no-try"}
class MotorLoop extends RealtimeTask {
    private final double[] samples = new double[64];
    private final double[] gains = new double[64];
    private PwmChannel pwm;
    private boolean failed;

    public void setup() {
        try {
            pwm = Hardware.getInstance().openPwm(0, 0);
        } catch (HardwareException e) {
            failed = true;
        }
    }

    public void cycle() {
        if (failed) {
            return;
        }
        double out = 0;
        for (int i = 0; i < samples.length; i++) {
            out += samples[i] * gains[i];
        }
        try {
            pwm.setDutyCycle(out);
        } catch (HardwareException e) {
            failed = true;   // no allocation on this path
        }
    }

    public void periodsMissed(int count) {
        // the loop overran; hold the output, count it, alarm if it persists
    }
}

RealtimeThread loop = Realtime.getInstance().start("motor",
        new RealtimeParameters()
            .setPeriodNanos(1000000)   // 1 kHz
            .setPriority(80)           // SCHED_FIFO 1..99
            .setCpu(3),                // pin to core 3
        new MotorLoop());
```

Two details there are worth a second look. The period runs on an absolute clock, so it doesn't drift with the time `cycle()` takes. And when the loop overruns, you get `periodsMissed(n)` instead of a burst of late cycles trying to catch up, which is what you want from a motor: hold, don't lurch.

## Trust the numbers, not the promise

The guide's advice is the same as mine: don't believe a real-time claim until you've measured it on your board under load. Every thread keeps statistics for exactly that:

```java {class="cn1-no-try"}
RealtimeStatistics stats = loop.getStatistics();
Log.p("scheduling applied: " + loop.isSchedulingApplied());
Log.p("p99.99 latency ns: " + stats.getLatencyPercentileNanos(0.9999));
Log.p(stats.toString());
```

You get cycles, missed periods, maximum and mean release latency, execution time, allocation violations and a 16 bucket latency histogram. Log `isSchedulingApplied()` at startup too. A loop that silently runs at normal priority because the service lacks `CAP_SYS_NICE` looks perfect on the bench and misses deadlines in the field. If you'd rather fail loudly, `setSchedulingRequired(true)` does that.

I'm not publishing latency figures here on purpose. Latency on PREEMPT_RT is set by the kernel and the hardware, and a number from our CI runner says nothing about your board. CI does run a 1 kHz loop while the main thread allocates as hard as it can, and fails if a release is ever late beyond a bound. Run the same check (`vm/backend/demo/rtcheck` in the repository) on your own hardware and you'll have a number that means something.

## Where this goes next

On a phone, in the simulator or on desktop, the same task runs on an ordinary thread without guarantees, so you can develop and test the logic anywhere. On Linux with PREEMPT_RT and the right capabilities, the collector stops getting in the way. The kernel still sets the latency.

This is the lightweight end of real time: firm deadlines on a general purpose OS. The next level is hard real time on a proper RTOS, with certification. We don't see a blocker in the design, but hard real time with certification is a big investment, and it only makes sense with a customer behind it. If you build products that need it, I want to hear from you.

The [real-time chapter](/developer-guide/realtime/) has the full API, the parameters, what each platform supports and how to set up a board.

Tomorrow: {{< post-link path="/blog/codename-one-on-xbox" text="Packaging Java Apps for Xbox Developer Mode, and Why Consoles Need Help" >}}.

{{< giscus >}}
