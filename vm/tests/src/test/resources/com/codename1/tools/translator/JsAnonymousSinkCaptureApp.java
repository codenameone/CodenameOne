public class JsAnonymousSinkCaptureApp {
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
        private final java.util.ArrayList<Op> upcoming = new java.util.ArrayList<Op>();
        private int submitCount;
        private Op lastSeen;

        private void debug(Op op) {
            lastSeen = op;
            submitCount++;
        }

        private final Sink<Op> sink = new Sink<Op>() {
            @Override
            public void submit(Op op) {
                debug(op);
                upcoming.add(op);
            }
        };

        java.util.List<Op> flush() {
            java.util.ArrayList<Op> out = new java.util.ArrayList<Op>(upcoming.size());
            for (int i = 0; i < upcoming.size(); i++) {
                out.add(upcoming.get(i));
            }
            upcoming.clear();
            return out;
        }
    }

    static final class Adapter {
        private final Factory<Op> factory;
        private final Snapshot snapshot;

        Adapter(Factory<Op> factory, Snapshot snapshot) {
            this.factory = factory;
            this.snapshot = snapshot;
        }

        void draw(int value) {
            snapshot.sink.submit(factory.create(value));
        }
    }

    static int result;

    public static void main(String[] args) {
        Snapshot snapshot = new Snapshot();
        Adapter adapter = new Adapter(new FactoryImpl(), snapshot);
        int score = 0;

        adapter.draw(7);
        adapter.draw(11);

        if (snapshot.submitCount == 2) {
            score |= 1;
        }
        if (snapshot.lastSeen instanceof FillRect) {
            score |= 2;
        }
        if (snapshot.lastSeen != null && snapshot.lastSeen.value() == 11) {
            score |= 4;
        }
        if (snapshot.upcoming.size() == 2) {
            score |= 8;
        }
        if (snapshot.upcoming.get(0) instanceof FillRect) {
            score |= 16;
        }
        if (snapshot.upcoming.get(0) != null && snapshot.upcoming.get(0).value() == 7) {
            score |= 32;
        }
        if (snapshot.upcoming.get(1) instanceof FillRect) {
            score |= 64;
        }
        if (snapshot.upcoming.get(1) != null && snapshot.upcoming.get(1).value() == 11) {
            score |= 128;
        }

        java.util.List<Op> flushed = snapshot.flush();
        if (flushed.size() == 2) {
            score |= 256;
        }
        if (flushed.get(0) instanceof FillRect && flushed.get(0).value() == 7) {
            score |= 512;
        }
        if (flushed.get(1) instanceof FillRect && flushed.get(1).value() == 11) {
            score |= 1024;
        }

        result = score;
        System.exit(score);
    }
}
