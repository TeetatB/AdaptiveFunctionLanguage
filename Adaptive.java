import java.util.*;
import java.util.function.Consumer;

public class Adaptive {


    public static class Time implements Comparable<Time> {
        private static int counter = 0;
        private final int id;
        private Time prev, next;

        private Time() { this.id = counter++; }
        public static Time init() { return new Time(); }

        public static void insertAfter(Time after, Time newTime) {
            newTime.prev = after;
            newTime.next = after.next;
            if (after.next != null) {
                after.next.prev = newTime;
            }
            after.next = newTime;
        }

        public static void spliceOut(Time start, Time end) {
//            if (start.next != end || end.prev != start) {
//                throw new IllegalArgumentException("start and end are not consecutive nodes");
//            }

            start.next = end.next;

            if (end.next != null) {
                end.next.prev = start;
            }

            end.prev = null;
        }

        @Override
        public int compareTo(Time other) {
            return Integer.compare(this.id, other.id);
        }

        @Override
        public String toString() {
            return "Time(" + id + ")";
        }
    }

    public static class Edge implements Comparable<Edge> {
        final Runnable reader;
        final Time start;
        final Time end;

        public Edge(Runnable reader, Time start, Time end) {
            this.reader = reader;
            this.start = start;
            this.end = end;
        }

        @Override
        public int compareTo(Edge other) {
            return this.start.compareTo(other.start);
        }
    }

    public static class Modifiable<T> {
        private T value;
        private final Comparator<T> comparator;
        private final List<Edge> outEdges = new ArrayList<>();
        private boolean initialized = false;
        private Time timestamp;

        public Modifiable(Comparator<T> comparator) {
            this.comparator = comparator;
        }

        public void write(T value) {
            if (initialized && comparator.compare(this.value, value) == 0) return;

            this.value = value;
            this.initialized = true;
            this.timestamp = AdaptiveEngine.currentTime();
            AdaptiveEngine.enqueueEdges(outEdges);
        }

        public T read() {
            if (!initialized) throw new IllegalStateException("Uninitialized modifiable");
            return value;
        }

        public void addEdge(Edge edge) {
            outEdges.add(edge);
        }
    }

    //--------------------- Adaptive Engine ----------------------//

    public static class AdaptiveEngine {
        private static final PriorityQueue<Edge> queue = new PriorityQueue<>();
        private static Time currentTime = Time.init();

        public static <T> Modifiable<T> mod(
                Comparator<T> cmp,
                Consumer<Modifiable<T>> initializer
        ) {
            Modifiable<T> m = new Modifiable<>(cmp);
            initializer.accept(m);
            return m;
        }

        public static <T> void read(Modifiable<T> source, Consumer<T> reader) {
            Time start = currentTime;
            Time end = new Time();
            Time.insertAfter(currentTime, end);
            currentTime = end;

            Edge edge = new Edge(() -> {
                T value = source.read();
                reader.accept(value);
            }, start, end);
            source.addEdge(edge);
        }

        public static void propagate() {
            while (!queue.isEmpty()) {
                Edge edge = queue.poll();

                if (edge.start.next == null || edge.end.prev == null ||
                        edge.start.next != edge.end) {
                    continue;
                }

                if (edge.start.prev == edge.end) continue;

                Time startTime = edge.start;
                Time endTime = edge.end;

                Time.spliceOut(startTime, endTime);

                Time originalCurrentTime = currentTime;

                currentTime = startTime;

                edge.reader.run();

                currentTime = originalCurrentTime;
            }
        }

        static void enqueueEdges(List<Edge> edges) {
            queue.addAll(edges);
        }

        static Time currentTime() {
            return currentTime;
        }
    }

    //----------------------- Usage Example -----------------------//

    public static void main(String[] args) {
        Modifiable<Integer> input = AdaptiveEngine.<Integer>mod(
                Comparator.naturalOrder(),
                m -> m.write(5)
        );

        Modifiable<Integer> result = AdaptiveEngine.<Integer>mod(
                Comparator.naturalOrder(),
                m -> {
                    // Initial value calculation
                    int initialValue = input.read() * 2;
                    m.write(initialValue);

                    // Reactive update setup
                    AdaptiveEngine.read(input, val -> {
                        m.write(val * 2);
                    });
                }
        );

        System.out.println("Initial result: " + result.read()); // Now works

        input.write(8);
//        AdaptiveEngine.propagate();
        input.write(10);
        AdaptiveEngine.propagate();
        System.out.println("Updated result: " + result.read()); // 16
    }
}