package it.unipi.hadoop;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class LanguageNormalizer {

    private static final Map<Character, Character> ITALIAN_ACCENTS_MAP;
    private static final Pattern ENGLISH_ALPHABET = Pattern.compile("[a-z]");
    private static final Pattern ITALIAN_ALPHABET = Pattern.compile("[a-z]");
    private static final Pattern TURKISH_ALPHABET = Pattern.compile("[a-zçğışöü]");

    static {
        ITALIAN_ACCENTS_MAP = new HashMap<>();
        ITALIAN_ACCENTS_MAP.put('à', 'a');
        ITALIAN_ACCENTS_MAP.put('è', 'e');
        ITALIAN_ACCENTS_MAP.put('é', 'e');
        ITALIAN_ACCENTS_MAP.put('ì', 'i');
        ITALIAN_ACCENTS_MAP.put('ò', 'o');
        ITALIAN_ACCENTS_MAP.put('ù', 'u');
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
        return retainMatchingCharacters(input, ENGLISH_ALPHABET);
    }

    private static String normalizeItalian(String input) {
        StringBuilder sb = new StringBuilder();
        for (char c : input.toCharArray()) {
            sb.append(ITALIAN_ACCENTS_MAP.getOrDefault(c, c));
        }
        return retainMatchingCharacters(sb.toString(), ITALIAN_ALPHABET);
    }

    private static String normalizeTurkish(String input) {
        return retainMatchingCharacters(input, TURKISH_ALPHABET);
    }

    private static String retainMatchingCharacters(String input, Pattern pattern) {
        StringBuilder sb = new StringBuilder();
        for (char c : input.toCharArray()) {
            if (pattern.matcher(String.valueOf(c)).matches()) {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}