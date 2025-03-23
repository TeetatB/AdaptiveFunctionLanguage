import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.text.NumberFormat;
import java.text.DecimalFormat;

public class AdaptiveSumTree {
    static class SumNode {
        Adaptive.Modifiable<Integer> sum;
        SumNode left;
        SumNode right;

        SumNode(Adaptive.Modifiable<Integer> value) {
            this.sum = value;
        }

        SumNode(SumNode left, SumNode right) {
            this.left = left;
            this.right = right;

            this.sum = Adaptive.AdaptiveEngine.<Integer>mod(
                    Comparator.naturalOrder(),
                    m -> {
                        int leftVal = left.sum.read();
                        int rightVal = right.sum.read();
                        m.write(leftVal + rightVal);

                        Adaptive.AdaptiveEngine.read(left.sum, leftValue -> {
                            int rightValue = right.sum.read();
                            m.write(leftValue + rightValue);
                        });

                        Adaptive.AdaptiveEngine.read(right.sum, rightValue -> {
                            int leftValue = left.sum.read();
                            m.write(leftValue + rightValue);
                        });
                    }
            );
        }
    }

    public static void main(String[] args) {
        int depth = 20;
        List<Adaptive.Modifiable<Integer>> leaves = new ArrayList<>();

        for (int i = 0; i < (1 << depth); i++) {
            Adaptive.Modifiable<Integer> leaf = Adaptive.AdaptiveEngine.<Integer>mod(
                    Comparator.naturalOrder(),
                    m -> m.write(1)
            );
            leaves.add(leaf);
        }

        List<SumNode> currentLevel = new ArrayList<>();
        for (Adaptive.Modifiable<Integer> leaf : leaves) {
            currentLevel.add(new SumNode(leaf));
        }

        for (int i = depth; i > 0; i--) {
            List<SumNode> nextLevel = new ArrayList<>();
            for (int j = 0; j < currentLevel.size(); j += 2) {
                SumNode left = currentLevel.get(j);
                SumNode right = currentLevel.get(j + 1);
                nextLevel.add(new SumNode(left, right));
            }
            currentLevel = nextLevel;
        }

        SumNode root = currentLevel.get(0);
        System.out.println("Initial root: " + root.sum.read());

        long start = System.nanoTime();
        leaves.get(0).write(2);
        Adaptive.AdaptiveEngine.propagate();
        long adaptiveTime = System.nanoTime() - start;
        System.out.println("Updated root: " + root.sum.read());
        System.out.println("Adaptive Time: " + adaptiveTime + " ns");
        NumberFormat scientificFormat = new DecimalFormat("0.###E0");

        String scientificNotation = scientificFormat.format(adaptiveTime);
        System.out.println("Adaptive Time in Scientific Notation: " + scientificNotation + " ns");
    }
}