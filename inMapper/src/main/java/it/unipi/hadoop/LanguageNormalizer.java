package it.unipi.hadoop;

// Class for normalizing text based on the language
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class LanguageNormalizer {

    private static final Map<Character, Character> ITALIAN_ACCENTS_MAP;
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile("[^a-zçğışöü]");

    static {
        ITALIAN_ACCENTS_MAP = new HashMap<>();
        ITALIAN_ACCENTS_MAP.put('à', 'a');
        ITALIAN_ACCENTS_MAP.put('è', 'e');
        ITALIAN_ACCENTS_MAP.put('é', 'e');
        ITALIAN_ACCENTS_MAP.put('ì', 'i');
        ITALIAN_ACCENTS_MAP.put('ò', 'o');
        ITALIAN_ACCENTS_MAP.put('ù', 'u');
        // Add other if necessary
    }

    public static String normalize(String input, String language) {
        // Convert the input to lowercase
        input = input.toLowerCase();

        switch (language.toLowerCase()) {
            case "en":
                return normalizeEnglish(input);
            case "it":
                return normalizeItalian(input);
            case "trk":
                return normalizeTurkish(input);
            default:
                throw new IllegalArgumentException("Unsupported language: " + language);
        }
    }

    private static String normalizeEnglish(String input) {
        input = removeSpecialCharacters(input);

        // No specific normalization for English, return the input as is
        return input;
    }

    private static String normalizeItalian(String input) {
        input = removeSpecialCharacters(input);

        StringBuilder sb = new StringBuilder();
        for (char c : input.toCharArray()) {
            sb.append(ITALIAN_ACCENTS_MAP.getOrDefault(c, c));
        }
        return sb.toString();
    }

    private static String normalizeTurkish(String input) {
        input = removeSpecialCharacters(input);
        // No specific normalization for Turkish, return the input as is
        return input;
    }

    private static String removeSpecialCharacters(String input) {
        // Remove all characters that are not letters (a-z, ç, ğ, ı, ş, ö, ü)
        return SPECIAL_CHAR_PATTERN.matcher(input).replaceAll("");
    }
}
