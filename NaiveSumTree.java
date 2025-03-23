import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;

public class NaiveSumTree {
    static class NaiveNode {
        int value;
        NaiveNode left;
        NaiveNode right;

        // Leaf constructor
        NaiveNode(int value) {
            this.value = value;
        }

        // Internal node constructor
        NaiveNode(NaiveNode left, NaiveNode right) {
            this.left = left;
            this.right = right;
            this.value = left.value + right.value;
        }

        void update() {
            if (left != null && right != null) {
                this.value = left.value + right.value;
            }
        }
    }

    public static void main(String[] args) {
        int depth = 20;
        List<List<NaiveNode>> allLevels = new ArrayList<>();

        // 1. Build leaves (level 0)
        List<NaiveNode> leaves = new ArrayList<>();
        for (int i = 0; i < (1 << depth); i++) {
            leaves.add(new NaiveNode(1));
        }
        allLevels.add(leaves);

        // 2. Build tree bottom-up and store all levels
        for (int level = 1; level <= depth; level++) {
            List<NaiveNode> prevLevel = allLevels.get(level - 1);
            List<NaiveNode> currLevel = new ArrayList<>();

            for (int i = 0; i < prevLevel.size(); i += 2) {
                NaiveNode left = prevLevel.get(i);
                NaiveNode right = prevLevel.get(i + 1);
                currLevel.add(new NaiveNode(left, right));
            }
            allLevels.add(currLevel);
        }

        System.out.println("Initial sum: " + allLevels.get(depth).get(0).value);

        // 3. Change a leaf and propagate
        NaiveNode targetLeaf = leaves.get(0);
        targetLeaf.value = 2;

        long start = System.nanoTime();
        // Propagate changes bottom-up through stored levels
        for (int level = 1; level <= depth; level++) {
            List<NaiveNode> currLevel = allLevels.get(level);
            for (NaiveNode node : currLevel) {
                node.update();
            }
        }
        long naiveTime = System.nanoTime() - start;

        System.out.println("Updated sum: " + allLevels.get(depth).get(0).value);
        System.out.println("Naive time: " + naiveTime + " ns");
        NumberFormat scientificFormat = new DecimalFormat("0.###E0");

        // Convert the number to scientific format
        String scientificNotation = scientificFormat.format(naiveTime);
        System.out.println("Native Time in Scientific Notation: " + scientificNotation + " ns");
    }
}