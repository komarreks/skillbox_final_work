package searchengine.lemmatizer;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

public class Lemmatizer {

    private static LuceneMorphology morph;
    private static final String[] particlesNames = new String[]{"МЕЖД", "ПРЕДЛ", "СОЮЗ"};

    static {
        try {
            morph = new RussianLuceneMorphology();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static HashMap<String, Integer> getLemmasCount(String text) throws IOException {
        HashMap<String, Integer> lemmasCount = new HashMap<>();

        String[] strings = arrayContainsRussianWords(text);

        for (String word : strings) {
            if (word.isBlank()) {
                continue;
            }

            List<String> wordBaseForms = morph.getMorphInfo(word);
            if (anyWordBaseBelongToParticle(wordBaseForms)) {
                continue;
            }

            List<String> normalForms = morph.getNormalForms(word);
            if (normalForms.isEmpty()) {
                continue;
            }

            String normalWord = normalForms.get(0);

            if (lemmasCount.containsKey(normalWord)) {
                lemmasCount.put(normalWord, lemmasCount.get(normalWord) + 1);
            } else {
                lemmasCount.put(normalWord, 1);
            }
        }

        return lemmasCount;
    }

    private static boolean anyWordBaseBelongToParticle(List<String> wordBaseForms) {
        AtomicBoolean hasPart = new AtomicBoolean(false);
        wordBaseForms.forEach(word ->{
            if (hasParticleProperty(word)){
                hasPart.set(true);
            }
        });

        return hasPart.get();
    }

    private static boolean hasParticleProperty(String wordBase) {
        for (String property : particlesNames) {
            if (wordBase.toUpperCase().contains(property)) {
                return true;
            }
        }
        return false;
    }

    private static String[] arrayContainsRussianWords(String text) {
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("([^а-я\\s])", " ")
                .trim()
                .split("\\s+");
    }
}
