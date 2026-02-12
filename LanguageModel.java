import java.util.HashMap;
import java.util.Random;
import java.io.FileReader;
import java.io.IOException;

public class LanguageModel {

    // The map of this model.
    // Maps windows to lists of character data objects.
    HashMap<String, List> CharDataMap;

    // The window length used in this model.
    int windowLength;

    // The random number generator used by this model.
    private Random randomGenerator;

    /** Constructs a language model with the given window length and a given
     * seed value. Generating texts from this model multiple times with the
     * same seed value will produce the same random texts. Good for debugging. */
    public LanguageModel(int windowLength, int seed) {
        this.windowLength = windowLength;
        randomGenerator = new Random(seed);
        CharDataMap = new HashMap<String, List>();
    }

    /** Constructs a language model with the given window length.
     * Generating texts from this model multiple times will produce
     * different random texts. Good for production. */
    public LanguageModel(int windowLength) {
        this.windowLength = windowLength;
        randomGenerator = new Random();
        CharDataMap = new HashMap<String, List>();
    }

    /** Builds a language model from the text in the given file (the corpus). */
    public void train(String fileName) {
        String window = "";
        char c;
        
        // In the homework instructions (Appendix B), they suggest using an 'In' class.
        // Since 'In' is a custom class, I am using standard Java FileReader here.
        // If you have the In.java file, you can use it as described in the PDF.
        try (FileReader reader = new FileReader(fileName)) {
            // Read the first windowLength characters to initialize the window
            for (int i = 0; i < windowLength; i++) {
                int charCode = reader.read();
                if (charCode == -1) return; // File too short
                window += (char) charCode;
            }

            // Read the rest of the file character by character
            int charCode;
            while ((charCode = reader.read()) != -1) {
                char nextChar = (char) charCode;

                // Get the list corresponding to the current window
                List probs = CharDataMap.get(window);

                // If the window is new, create a new list and put it in the map
                if (probs == null) {
                    probs = new List();
                    CharDataMap.put(window, probs);
                }

                // Counts the current character. 
                // Using update() as specified in the PDF (Stage 1 instructions).
                probs.update(nextChar);

                // Advances the window: adds nextChar to the window's end, 
                // and deletes the window's first character.
                window = window.substring(1) + nextChar;
            }

        } catch (IOException e) {
            System.err.println("Error reading file: " + fileName);
            e.printStackTrace();
            return;
        }

        // After reading the whole file, calculate probabilities for all lists in the map
        for (List probs : CharDataMap.values()) {
            calculateProbabilities(probs);
        }
    }

    // Computes and sets the probabilities (p and cp fields) of all the
    // characters in the given list. */
    public void calculateProbabilities(List probs) {
        int totalCount = 0;

        // 1. Calculate the total count of all characters in this list
        // We use the ListIterator since getSize() might not be available.
        // Assuming listIterator(0) returns an iterator starting at the beginning.
        ListIterator itr = probs.listIterator(0); 
        while (itr.hasNext()) {
            CharData cd = itr.next();
            totalCount += cd.count;
        }
        
        if (totalCount == 0) return;

        double cumulativeProbability = 0.0;

        // 2. Calculate p and cp for each CharData object
        // Reset iterator to start
        itr = probs.listIterator(0);
        while (itr.hasNext()) {
             CharData cd = itr.next();
             cd.p = (double) cd.count / totalCount;
             cumulativeProbability += cd.p;
             cd.cp = cumulativeProbability;
        }
    }

    // Returns a random character from the given probabilities list.
    public char getRandomChar(List probs) {
        // Generate a random number between 0.0 (inclusive) and 1.0 (exclusive)
        double r = randomGenerator.nextDouble();

        // Iterate through the list to find the character
        ListIterator itr = probs.listIterator(0);
        while (itr.hasNext()) {
            CharData cd = itr.next();
            // Return the first character whose cumulative probability exceeds r
            if (cd.cp > r) {
                return cd.chr;
            }
        }
        
        // Fallback (should rarely happen if logic is correct)
        // If we get here, return the last element's char (or a placeholder)
        // Since we can't easily get the last element without traversing, 
        // we'll assume the loop handles it. If the list is valid, we won't reach here.
        return ' '; 
    }

    /**
     * Generates a random text, based on the probabilities that were learned during training. 
     * @param initialText - text to start with.
     * @param textLength - the size of text to generate
     * @return the generated text
     */
    public String generate(String initialText, int textLength) {
        if (initialText.length() < windowLength) {
            return initialText;
        }

        StringBuilder generatedText = new StringBuilder(initialText);
        String currentWindow = generatedText.substring(generatedText.length() - windowLength);

        for (int i = 0; i < textLength; i++) {
            List probs = CharDataMap.get(currentWindow);

            if (probs == null) {
                break;
            }

            char nextChar = getRandomChar(probs);

            generatedText.append(nextChar);
            currentWindow = generatedText.substring(generatedText.length() - windowLength);
        }

        return generatedText.toString();
    }

    /** Returns a string representing the map of this language model. */
    public String toString() {
        StringBuilder str = new StringBuilder();
        for (String key : CharDataMap.keySet()) {
            List keyProbs = CharDataMap.get(key);
            str.append(key + " : " + keyProbs + "\n");
        }
        return str.toString();
    }

    public static void main(String[] args) {
        // You can implement tests here as described in the PDF
    }
}