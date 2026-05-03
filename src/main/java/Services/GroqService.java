package Services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class GroqService {

    private static final String API_KEY = "//"; // même clé que Symfony ✅
    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";

    private static final String MODEL_TEXT   = "llama-3.3-70b-versatile";
    private static final String MODEL_VISION = "meta-llama/llama-4-scout-17b-16e-instruct";

    private final ObjectMapper mapper = new ObjectMapper();
    private final List<ObjectNode> conversationHistory = new ArrayList<>();

    private static final String SYSTEM_PROMPT =
            "Tu es un assistant nutritionnel. Tu réponds TOUJOURS en français.\n\n" +
                    "RÈGLE CRITIQUE : Quand tu proposes un repas/recette, utilise EXACTEMENT ce format :\n\n" +
                    "**NOM_DU_PLAT** | TOTAL kcal\n" +
                    "• INGREDIENT — QUANTITE g (CALORIES kcal)\n" +
                    "• INGREDIENT — QUANTITE g (CALORIES kcal)\n" +
                    "💡 NOTE: conseil nutritionnel\n\n" +
                    "RECETTE:\n" +
                    "1. Étape\n" +
                    "2. Étape\n\n" +
                    "EXEMPLE OBLIGATOIRE à suivre :\n" +
                    "**Omelette aux légumes** | 320 kcal\n" +
                    "• Oeufs — 120 g (180 kcal)\n" +
                    "• Poivron — 80 g (25 kcal)\n" +
                    "• Huile olive — 10 g (90 kcal)\n" +
                    "• Sel — 2 g (0 kcal)\n" +
                    "💡 NOTE: Riche en protéines, parfait pour le déjeuner.\n\n" +
                    "RECETTE:\n" +
                    "1. Battre les oeufs dans un bol.\n" +
                    "2. Faire revenir le poivron.\n" +
                    "3. Verser les oeufs et cuire 3 min.\n\n" +
                    "Pour bonjour/questions générales : réponds normalement, sans format repas.";

    public GroqService() {
        ObjectNode sys = mapper.createObjectNode();
        sys.put("role", "system");
        sys.put("content", SYSTEM_PROMPT);
        conversationHistory.add(sys);
    }

    /** Chat texte normal */
    public String chat(String userMessage) throws IOException {
        addToHistory("user", userMessage);
        String response = callApi(MODEL_TEXT, conversationHistory);
        addToHistory("assistant", response);
        return response;
    }

    /** Chat avec image (base64) */
    public String chatWithImage(String base64Image, String mimeType) throws IOException {
        // Message vision avec image + instruction de format
        ObjectNode userMsg = mapper.createObjectNode();
        userMsg.put("role", "user");

        ArrayNode contentArr = mapper.createArrayNode();

        ObjectNode textNode = mapper.createObjectNode();
        textNode.put("type", "text");
        textNode.put("text",
                "Analyse ce plat. Identifie les ingrédients et estime les calories. " +
                        "Réponds en français avec le format EXACT : " +
                        "**NOM** | TOTAL kcal, puis • ingredient — quantité g (cal kcal) pour chaque ingrédient, " +
                        "puis 💡 NOTE: conseil, puis RECETTE: avec étapes numérotées."
        );
        contentArr.add(textNode);

        ObjectNode imgNode = mapper.createObjectNode();
        imgNode.put("type", "image_url");
        ObjectNode imgUrl = mapper.createObjectNode();
        imgUrl.put("url", "data:" + mimeType + ";base64," + base64Image);
        imgNode.set("image_url", imgUrl);
        contentArr.add(imgNode);

        userMsg.set("content", contentArr);

        // Appel vision séparé (pas de long historique = plus rapide + évite erreurs)
        List<ObjectNode> visionMsgs = new ArrayList<>();
        ObjectNode visionSys = mapper.createObjectNode();
        visionSys.put("role", "system");
        visionSys.put("content",
                "Tu es un expert en nutrition. Analyse les photos de plats et réponds en français. " +
                        "Format OBLIGATOIRE: **NOM** | TOTAL kcal, • ingredient — qté g (cal kcal), " +
                        "💡 NOTE: conseil, RECETTE: 1. étape 2. étape"
        );
        visionMsgs.add(visionSys);
        visionMsgs.add(userMsg);

        String response = callApi(MODEL_VISION, visionMsgs);

        // Garder trace dans l'historique texte
        addToHistory("user", "[Photo envoyée - analyse demandée]");
        addToHistory("assistant", response);
        return response;
    }

    private String callApi(String model, List<ObjectNode> messages) throws IOException {
        ObjectNode body = mapper.createObjectNode();
        body.put("model", model);
        body.put("temperature", 0.5);
        body.put("max_tokens", 1500);

        ArrayNode msgArr = mapper.createArrayNode();
        for (ObjectNode m : messages) msgArr.add(m);
        body.set("messages", msgArr);

        String json = mapper.writeValueAsString(body);

        HttpURLConnection conn = (HttpURLConnection) new URL(API_URL).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
        conn.setDoOutput(true);
        conn.setConnectTimeout(30_000);
        conn.setReadTimeout(90_000);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(json.getBytes(StandardCharsets.UTF_8));
        }

        int status = conn.getResponseCode();
        InputStream stream = (status >= 200 && status < 300)
                ? conn.getInputStream() : conn.getErrorStream();

        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
        }

        if (status < 200 || status >= 300) {
            throw new IOException("Groq API erreur " + status + " : " + sb);
        }

        return mapper.readTree(sb.toString())
                .path("choices").get(0)
                .path("message").path("content").asText();
    }

    private void addToHistory(String role, String content) {
        ObjectNode msg = mapper.createObjectNode();
        msg.put("role", role);
        msg.put("content", content);
        conversationHistory.add(msg);
    }

    public void clearHistory() {
        ObjectNode sys = conversationHistory.get(0);
        conversationHistory.clear();
        conversationHistory.add(sys);
    }
}
