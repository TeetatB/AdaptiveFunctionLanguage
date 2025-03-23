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
            Time modTime = new Time();
            Time.insertAfter(currentTime, modTime);
            m.timestamp = modTime;
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

                if (edge.start.next == null || edge.end.prev == null || edge.start.next != edge.end) {
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
        List<Integer> list = RandomNumberList.listGen(100);
        List<Integer> nlist = new ArrayList<Integer>(list);

        // Normal quicksort timing
        List<Integer> normalList = new ArrayList<>(list);
        long startNormal = System.nanoTime();
        normalList.sort(Integer::compareTo);
        long endNormal = System.nanoTime();
        System.out.println("Normal Quicksort: " + normalList + " Time: " + (endNormal - startNormal) + " ns");

        // Adaptive quicksort timing
        Modifiable<List<Integer>> input = AdaptiveEngine.mod(
                Comparator.comparing(List::toString),
                m -> m.write(new ArrayList<>(list))
        );

        Modifiable<List<Integer>> sortedList = AdaptiveEngine.mod(
                Comparator.comparing(List::toString),
                m -> {
                    List<Integer> sorted = new ArrayList<>(input.read());
                    sorted.sort(Integer::compareTo);
                    m.write(sorted);

                    AdaptiveEngine.read(input, val -> {
                        List<Integer> updated = new ArrayList<>(val);
                        updated.sort(Integer::compareTo);
                        m.write(updated);
                    });
                }
        );

        long startAdaptive = System.nanoTime();
        sortedList.read();
        long endAdaptive = System.nanoTime();
        System.out.println("Adaptive Quicksort: " + sortedList.read() + " Time: " + (endAdaptive - startAdaptive) + " ns");

        // Modify input and measure time
        nlist.add(200000000);
        input.write(nlist);
        long startUpdate = System.nanoTime();
        AdaptiveEngine.propagate();
        long endUpdate = System.nanoTime();
        System.out.println("Updated Adaptive Quicksort: " + sortedList.read() + " Time: " + (endUpdate - startUpdate) + " ns");
    }
}
