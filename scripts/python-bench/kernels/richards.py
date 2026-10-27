# An operating system task scheduler: classes, attribute access, virtual calls.
from _harness import measure

IDLE, WORKER, HANDLER_A, HANDLER_B, DEVICE_A, DEVICE_B = range(6)


class Packet:
    def __init__(self, link, ident, kind):
        self.link = link
        self.ident = ident
        self.kind = kind
        self.datum = 0
        self.data = [0] * 4

    def append_to(self, queue):
        self.link = None
        if queue is None:
            return self
        p = queue
        while p.link is not None:
            p = p.link
        p.link = self
        return queue


class Task:
    def __init__(self, scheduler, ident, priority, queue, waiting):
        self.scheduler = scheduler
        self.ident = ident
        self.priority = priority
        self.queue = queue
        self.waiting = waiting
        self.held = False
        self.link = scheduler.tasks
        scheduler.tasks = self
        scheduler.table[ident] = self

    def runnable(self):
        return not self.held and not self.waiting

    def take(self):
        p = self.queue
        self.queue = p.link
        self.waiting = self.queue is not None and False
        return p

    def add_packet(self, packet, current):
        if self.queue is None:
            self.queue = packet
            self.waiting = False
            if self.priority > current.priority:
                return self
        else:
            self.queue = packet.append_to(self.queue)
        return current


class Idle(Task):
    def __init__(self, scheduler, count):
        Task.__init__(self, scheduler, IDLE, 0, None, False)
        self.control = 1
        self.count = count

    def run(self, packet):
        s = self.scheduler
        self.count -= 1
        if self.count == 0:
            self.held = True
            return s.next_of(self)
        if self.control & 1 == 0:
            self.control >>= 1
            return s.release(DEVICE_A, self)
        self.control = (self.control >> 1) ^ 0xD008
        return s.release(DEVICE_B, self)


class Worker(Task):
    def __init__(self, scheduler, queue):
        Task.__init__(self, scheduler, WORKER, 1000, queue, True)
        self.destination = HANDLER_A
        self.count = 0

    def run(self, packet):
        s = self.scheduler
        if packet is None:
            self.waiting = True
            return s.next_of(self)
        self.destination = HANDLER_B if self.destination == HANDLER_A else HANDLER_A
        packet.ident = self.destination
        packet.datum = 0
        for i in range(4):
            self.count += 1
            if self.count > 26:
                self.count = 1
            packet.data[i] = 64 + self.count
        return s.queue_packet(packet, self)


class Handler(Task):
    def __init__(self, scheduler, ident, queue):
        Task.__init__(self, scheduler, ident, 2000 if ident == HANDLER_A else 3000, queue, True)
        self.work = None
        self.devices = None

    def run(self, packet):
        s = self.scheduler
        if packet is not None:
            if packet.kind == 1:
                self.work = packet.append_to(self.work)
            else:
                self.devices = packet.append_to(self.devices)
        if self.work is not None:
            w = self.work
            count = w.datum
            if count < 4:
                if self.devices is not None:
                    d = self.devices
                    self.devices = d.link
                    d.datum = w.data[count]
                    w.datum = count + 1
                    return s.queue_packet(d, self)
            else:
                self.work = w.link
                return s.queue_packet(w, self)
        self.waiting = True
        return s.next_of(self)


class Device(Task):
    def __init__(self, scheduler, ident):
        Task.__init__(self, scheduler, ident, 4000 if ident == DEVICE_A else 5000, None, True)
        self.pending = None

    def run(self, packet):
        s = self.scheduler
        if packet is None:
            if self.pending is None:
                self.waiting = True
                return s.next_of(self)
            p = self.pending
            self.pending = None
            return s.queue_packet(p, self)
        self.pending = packet
        s.hold_count += 1
        self.held = True
        return s.next_of(self)


class Scheduler:
    def __init__(self):
        self.tasks = None
        self.table = [None] * 6
        self.queue_count = 0
        self.hold_count = 0

    def next_of(self, task):
        t = task.link
        while t is not None and not t.runnable() and not (t.queue is not None and not t.held):
            t = t.link
        return t if t is not None else self.first_ready()

    def first_ready(self):
        t = self.tasks
        while t is not None:
            if not t.held and (t.queue is not None or not t.waiting):
                return t
            t = t.link
        return None

    def release(self, ident, current):
        t = self.table[ident]
        t.held = False
        return t if t.priority > current.priority else current

    def queue_packet(self, packet, current):
        t = self.table[packet.ident]
        self.queue_count += 1
        packet.link = None
        packet.ident = current.ident
        return t.add_packet(packet, current)

    def run(self):
        t = self.tasks
        while t is not None:
            if t.held or (t.queue is None and t.waiting):
                t = t.link
                continue
            packet = None
            if t.queue is not None:
                packet = t.queue
                t.queue = packet.link
                if t.queue is None:
                    t.waiting = False
            t = t.run(packet)


def work(count):
    s = Scheduler()
    Idle(s, count)
    wq = Packet(None, 0, 1)
    wq = Packet(wq, 0, 1)
    Worker(s, wq)
    q = Packet(None, DEVICE_A, 0)
    q = Packet(q, DEVICE_A, 0)
    q = Packet(q, DEVICE_A, 0)
    Handler(s, HANDLER_A, q)
    q = Packet(None, DEVICE_B, 0)
    q = Packet(q, DEVICE_B, 0)
    q = Packet(q, DEVICE_B, 0)
    Handler(s, HANDLER_B, q)
    Device(s, DEVICE_A)
    Device(s, DEVICE_B)
    s.run()
    return "%d %d" % (s.queue_count, s.hold_count)


measure("richards", work, 400000, 5)
