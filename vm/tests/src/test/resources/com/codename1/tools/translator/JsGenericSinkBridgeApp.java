public class JsGenericSinkBridgeApp {
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

    interface Factory<T> {
        T create(int value);
    }

    static final class FactoryImpl implements Factory<Op> {
        @Override
        public FillRect create(int value) {
            return new FillRect(value);
        }
    }

    interface Sink<T> {
        void submit(T op);
    }

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

    static final class SinkImpl implements Sink<Op> {
        private final Snapshot snapshot = new Snapshot();
        Op last;
        int count;

        @Override
        public void submit(Op op) {
            last = op;
            count++;
            snapshot.submit(op);
        }

        java.util.List<Op> flush() {
            return snapshot.flush();
        }
    }

    static final class Adapter {
        private final Factory<Op> factory;
        private final Sink<Op> sink;

        Adapter(Factory<Op> factory, Sink<Op> sink) {
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
        if (sink.last.value() == 7) {
            score |= 2;
        }
        if (sink.count == 1) {
            score |= 4;
        }

        adapter.draw(11);
        if (sink.last instanceof FillRect) {
            score |= 8;
        }
        if (sink.last.value() == 11) {
            score |= 16;
        }
        if (sink.count == 2) {
            score |= 32;
        }

        java.util.List<Op> flushed = sink.flush();
        if (flushed.size() == 2) {
            score |= 64;
        }
        if (flushed.get(0) instanceof FillRect) {
            score |= 128;
        }
        if (flushed.get(0).value() == 7) {
            score |= 256;
        }
        if (flushed.get(1) instanceof FillRect) {
            score |= 512;
        }
        if (flushed.get(1).value() == 11) {
            score |= 1024;
        }

        result = score;
        System.exit(score);
    }
}
