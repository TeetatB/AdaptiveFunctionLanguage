public class MLComputation {
    public static int compute(int x) {
        return (x * 2) + (x + 3) - (x / 2); // Example computation
    }

    public static void simulate() {
        long start = System.nanoTime();
        for (int i = 0; i < 1000000; i++) { // Simulate updates
            compute(i);
        }
        long end = System.nanoTime();
        System.out.println("ML Time: " + (end - start) + " ns");
    }

    public static void main(String[] args) {
//        long start = System.nanoTime();
//        for (int i = 0; i < 1000000; i++) { // Simulate updates
//            compute(i);
//        }
//        long end = System.nanoTime();
//        System.out.println("ML Time: " + (end - start) + " ns");
    }
}
