import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

public class RandomNumberList {
    public static List<Integer> listGen(int number) {
        // Create a HashSet to store unique numbers
        HashSet<Integer> uniqueNumbers = new HashSet<>();
        Random random = new Random();

        // Generate 10,000 distinct random numbers
        while (uniqueNumbers.size() < 10000) {
            int randomNumber = random.nextInt(1000000) + 1; // Range: 1 to 1,000,000
            uniqueNumbers.add(randomNumber);
        }

        // Convert HashSet to List
        List<Integer> numberList = new ArrayList<>(uniqueNumbers);

        return numberList;
    }
}