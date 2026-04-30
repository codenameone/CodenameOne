public class JsInterfaceObjectBridgeApp {
    static final class Snapshot {
        private final java.util.Queue<Op> upcoming = new java.util.LinkedList<Op>();

        void submit(Op op) {
            upcoming.add(op);
        }

        java.util.List<Op> flush() {
            java.util.List<Op> out = new java.util.ArrayList<Op>(upcoming.size());
            out.addAll(upcoming);
            upcoming.clear();
            return out;
        }
    }

    interface Op {
        int value();
    }

    static final class FillRect implements Op {
        private final int value;

        FillRect(int value) {
            this.value = value;
        }

        @Override
        public int value() {
            return value;
        }
    }

    interface Factory {
        Object create(int value);
    }

    static final class FactoryImpl implements Factory {
        @Override
        public FillRect create(int value) {
            return new FillRect(value);
        }
    }

    interface Sink {
        void submit(Object op);
    }

    static final class SinkImpl implements Sink {
        private final Snapshot snapshot = new Snapshot();
        Object last;
        int count;

        @Override
        public void submit(Object op) {
            last = op;
            count++;
            snapshot.submit((Op) op);
        }

        java.util.List<Op> flush() {
            return snapshot.flush();
        }
    }

    static final class Adapter {
        private final Factory factory;
        private final Sink sink;

        Adapter(Factory factory, Sink sink) {
            this.factory = factory;
            this.sink = sink;
        }

        void draw(int value) {
            sink.submit(factory.create(value));
        }
    }

    static int result;

    public static void main(String[] args) {
        SinkImpl sink = new SinkImpl();
        Adapter adapter = new Adapter(new FactoryImpl(), sink);
        int score = 0;

        adapter.draw(7);
        if (sink.last instanceof FillRect) {
            score |= 1;
        }
        if (((FillRect) sink.last).value() == 7) {
            score |= 2;
        }
        if (sink.count == 1) {
            score |= 4;
        }

        adapter.draw(11);
        if (sink.last instanceof FillRect) {
            score |= 8;
        }
        if (((FillRect) sink.last).value() == 11) {
            score |= 16;
        }
        if (sink.count == 2) {
            score |= 32;
        }
        java.util.List<Op> flushed = sink.flush();
        if (flushed.size() == 2) {
            score |= 64;
        }
        if (flushed.get(0) instanceof FillRect && flushed.get(0).value() == 7) {
            score |= 128;
        }
        if (flushed.get(1) instanceof FillRect && flushed.get(1).value() == 11) {
            score |= 256;
        }

        result = score;
        System.exit(score);
    }
}
