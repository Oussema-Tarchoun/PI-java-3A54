package Services;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parse la réponse Groq pour extraire un repas structuré.
 * Format attendu (mais le parser est très tolérant) :
 *
 *   **Nom du plat** | 410 kcal
 *   • Yaourt nature — 200 g (100 kcal)
 *   • Pain complet — 50 g (80 kcal)
 *   💡 NOTE: conseil
 *   RECETTE:
 *   1. étape 1
 *   2. étape 2
 */
public class ChatRepasParser {

    public static class ParsedMeal {
        public String name = "";
        public int totalCalories = 0;
        public List<ParsedIngredient> ingredients = new ArrayList<>();
        public List<String> steps = new ArrayList<>();
        public String note = "";

        public boolean isValid() {
            return !name.isEmpty() && !ingredients.isEmpty();
        }
    }

    public static class ParsedIngredient {
        public String name = "";
        public String quantity = "";
        public int calories = 0;
    }

    // ─── Détection ────────────────────────────────────────────────────────────────

    /**
     * Retourne true si la réponse contient une proposition de repas structurée.
     * Critères souples : présence de "kcal" + "**" (gras) ou "•"/"—"
     */
    public static boolean containsMealProposal(String response) {
        if (response == null || response.isEmpty()) return false;
        boolean hasCalories = response.contains("kcal");
        boolean hasBold     = response.contains("**");
        boolean hasBullet   = response.contains("•") || response.contains("—") || response.contains("-");
        boolean hasNumbers  = response.matches("(?s).*\\d+\\s*g.*");
        return hasCalories && (hasBold || (hasBullet && hasNumbers));
    }

    // ─── Parser principal ─────────────────────────────────────────────────────────

    public static ParsedMeal parse(String response) {
        ParsedMeal meal = new ParsedMeal();
        if (response == null) return meal;

        String text = response.trim();

        // 1. Nom + calories totales
        // Patterns par ordre de priorité
        Pattern[] headerPatterns = {
                // **Nom du plat** | 410 kcal
                Pattern.compile("\\*\\*(.+?)\\*\\*\\s*[|]\\s*(\\d+)\\s*kcal", Pattern.CASE_INSENSITIVE),
                // **Nom du plat** (410 kcal)
                Pattern.compile("\\*\\*(.+?)\\*\\*\\s*\\((\\d+)\\s*kcal\\)", Pattern.CASE_INSENSITIVE),
                // **Nom du plat** - 410 kcal
                Pattern.compile("\\*\\*(.+?)\\*\\*\\s*[-–]\\s*(\\d+)\\s*kcal", Pattern.CASE_INSENSITIVE),
                // Nom du plat : 410 kcal  (sans gras)
                Pattern.compile("^([A-ZÀ-Ü][\\wÀ-ÿ\\s']+?)\\s*[:|]\\s*(\\d+)\\s*kcal", Pattern.MULTILINE),
        };

        for (Pattern p : headerPatterns) {
            Matcher m = p.matcher(text);
            if (m.find()) {
                meal.name = cleanText(m.group(1));
                meal.totalCalories = Integer.parseInt(m.group(2));
                break;
            }
        }

        // Fallback: premier texte en gras comme nom
        if (meal.name.isEmpty()) {
            Matcher boldMatcher = Pattern.compile("\\*\\*(.+?)\\*\\*").matcher(text);
            if (boldMatcher.find()) {
                meal.name = cleanText(boldMatcher.group(1));
            }
        }

        // 2. Ingrédients — patterns très souples
        // Matches: • Yaourt nature — 200 g (100 kcal)
        //          - Pain complet — 50g (80kcal)
        //          * Banane — 1 unité (90 kcal)
        //          Noisettes — 10 g (100 kcal)
        Pattern ingPattern = Pattern.compile(
                "(?:^|\\n)\\s*[•\\-*]?\\s*" +           // bullet optionnel
                        "([A-ZÀ-ÿa-z][^—\\-\\n(]{2,40})"  +    // nom ingrédient
                        "\\s*[—\\-–]+\\s*" +                      // séparateur
                        "([\\d.,]+\\s*(?:g|ml|cl|l|kg|unité|unit|tbsp|tsp|cup|piece|pièce)?)" + // quantité
                        "\\s*\\((\\d+)\\s*kcal\\)",               // calories entre parenthèses
                Pattern.CASE_INSENSITIVE | Pattern.MULTILINE
        );

        Matcher ingMatcher = ingPattern.matcher(text);
        while (ingMatcher.find()) {
            ParsedIngredient ing = new ParsedIngredient();
            ing.name     = cleanText(ingMatcher.group(1));
            ing.quantity = ingMatcher.group(2).trim();
            ing.calories = Integer.parseInt(ingMatcher.group(3));

            // Ignorer si le "nom" ressemble à un entête ou est vide
            if (!ing.name.isEmpty() && !ing.name.equalsIgnoreCase("recette") &&
                    !ing.name.equalsIgnoreCase("ingrédients")) {
                meal.ingredients.add(ing);
            }
        }

        // Fallback ingrédients si rien trouvé: chercher "xxx — xxx (yyy kcal)"
        if (meal.ingredients.isEmpty()) {
            Pattern fallback = Pattern.compile(
                    "([A-ZÀ-ÿa-z][^(\\n]{2,35})\\s*\\((\\d+)\\s*kcal\\)",
                    Pattern.CASE_INSENSITIVE
            );
            Matcher fb = fallback.matcher(text);
            while (fb.find()) {
                String raw = fb.group(1).trim();
                // Doit contenir un — ou chiffre (quantité)
                if (raw.contains("—") || raw.contains("-") || raw.matches(".*\\d.*")) {
                    ParsedIngredient ing = new ParsedIngredient();
                    // Séparer nom / quantité
                    String[] parts = raw.split("[—\\-–]", 2);
                    ing.name     = cleanText(parts[0]);
                    ing.quantity = parts.length > 1 ? parts[1].trim() : "";
                    ing.calories = Integer.parseInt(fb.group(2));
                    if (!ing.name.isEmpty()) meal.ingredients.add(ing);
                }
            }
        }

        // 3. Calculer les calories totales si manquantes
        if (meal.totalCalories == 0 && !meal.ingredients.isEmpty()) {
            meal.totalCalories = meal.ingredients.stream().mapToInt(i -> i.calories).sum();
        }

        // 4. Étapes de recette
        // Chercher section après "RECETTE:", "Recette:", "Étapes:", "Steps:"
        Pattern sectionPattern = Pattern.compile(
                "(?:RECETTE|Recette|Étapes?|Steps?)[:\\s]*\\n((?:(?:\\d+[.)\\s]|[-•]).*\\n?)+)",
                Pattern.CASE_INSENSITIVE
        );
        Matcher secMatcher = sectionPattern.matcher(text);
        if (secMatcher.find()) {
            Matcher stepMatcher = Pattern.compile("(?:\\d+[.)\\s]|[-•])\\s*(.+)").matcher(secMatcher.group(1));
            while (stepMatcher.find()) {
                String step = cleanText(stepMatcher.group(1));
                if (!step.isEmpty()) meal.steps.add(step);
            }
        }

        // 5. Note nutritionnelle
        Pattern notePattern = Pattern.compile(
                "(?:💡\\s*NOTE|NOTE|Conseil)[:\\s]*(.+?)(?:\\n|$)",
                Pattern.CASE_INSENSITIVE
        );
        Matcher noteMatcher = notePattern.matcher(text);
        if (noteMatcher.find()) {
            meal.note = cleanText(noteMatcher.group(1));
        }

        return meal;
    }

    private static String cleanText(String s) {
        if (s == null) return "";
        return s.replaceAll("\\*+", "")
                .replaceAll("^[\\s•\\-*]+", "")
                .replaceAll("[\\s•\\-*]+$", "")
                .trim();
    }
}
