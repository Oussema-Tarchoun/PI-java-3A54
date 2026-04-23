package utils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles all communication with the Ollama AI server.
 * Mirrors the PHP RoadmapAI + StudyHelperAI services.
 */
public class OllamaService {

    private static final String OLLAMA_URL  = "http://localhost:11434";
    private static final String MODEL       = "llama3.2:latest";
    private static final int    TIMEOUT_SEC = 120;

    private final HttpClient httpClient;

    // ── Session-based conversation history (max 6 messages) ───────────────────
    private final List<JSONObject> conversationHistory = new ArrayList<>();

    public OllamaService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(120))
                .build();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CHAT  — mirrors StudyHelperAI.chatWithAI()
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Send a user message to Ollama and get an AI response.
     * Keeps the last 6 messages in session memory.
     *
     * @param userMessage the user's chat message
     * @param coursId     optional course ID for context (pass null if none)
     * @return AI response text, or an error message
     */
    public String chat(String userMessage, Integer coursId) {
        // Keep only last 6 messages
        if (conversationHistory.size() >= 6) {
            conversationHistory.subList(0, conversationHistory.size() - 5).clear();
        }

        // Add user message to history
        JSONObject userMsg = new JSONObject();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);
        conversationHistory.add(userMsg);

        // Build system prompt — same as PHP version
        String systemPrompt = "Tu es un assistant d'étude IA. Règles STRICTES:\n" +
                "- Réponds TOUJOURS en français\n" +
                "- Maximum 80 mots par réponse\n" +
                "- Sois direct et concis, pas de répétition\n" +
                "- Utilise • pour les listes\n" +
                "- Si demande de vidéo/tutoriel YouTube, donne ce lien: " +
                "https://www.youtube.com/results?search_query=" + userMessage.replace(" ", "+") +
                (coursId != null ? "\n- Tu aides pour le cours ID: " + coursId : "");

        JSONObject systemMsg = new JSONObject();
        systemMsg.put("role", "system");
        systemMsg.put("content", systemPrompt);

        // Build messages array: system + history
        JSONArray messages = new JSONArray();
        messages.put(systemMsg);
        for (JSONObject msg : conversationHistory) {
            messages.put(msg);
        }

        // Build request body
        JSONObject body = new JSONObject();
        body.put("model", MODEL);
        body.put("messages", messages);
        body.put("stream", false);

        JSONObject options = new JSONObject();
        options.put("num_predict", 200);
        options.put("temperature", 0.3);
        options.put("top_p", 0.9);
        options.put("num_ctx", 1024);
        body.put("options", options);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(OLLAMA_URL + "/api/chat"))
                    .timeout(Duration.ofSeconds(TIMEOUT_SEC))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            JSONObject result   = new JSONObject(response.body());
            String     aiAnswer = result.getJSONObject("message").getString("content");

            // Add AI response to history
            JSONObject assistantMsg = new JSONObject();
            assistantMsg.put("role", "assistant");
            assistantMsg.put("content", aiAnswer);
            conversationHistory.add(assistantMsg);

            // Keep only last 6
            if (conversationHistory.size() > 6) {
                conversationHistory.subList(0, conversationHistory.size() - 6).clear();
            }

            return aiAnswer;

        } catch (Exception e) {
            return "❌ Ollama indisponible. Vérifie que `ollama serve` est lancé.\n(" + e.getMessage() + ")";
        }
    }

    /** Clear conversation history (new session) */
    public void clearHistory() {
        conversationHistory.clear();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // ROADMAP  — mirrors RoadmapAI.generateRoadmap()
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Generate a learning roadmap for a course based on its level.
     *
     * @param level      "debutant" | "intermediaire" | "avance"
     * @param coursTitre course title
     * @return generated roadmap text, or an error message
     */
    public String generateRoadmap(String level, String coursTitre) {
        String prompt = buildRoadmapPrompt(level, coursTitre);

        JSONObject body = new JSONObject();
        body.put("model", MODEL);
        body.put("prompt", prompt);
        body.put("stream", false);

        JSONObject options = new JSONObject();
        options.put("temperature", 0.3);
        options.put("num_predict", 80);  // ← max 80 tokens output
        options.put("num_ctx", 512);     // ← small context window = faster
        body.put("options", options);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(OLLAMA_URL + "/api/generate"))
                    .timeout(Duration.ofSeconds(120))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            JSONObject result = new JSONObject(response.body());
            return result.optString("response", "❌ Erreur génération roadmap.");

        } catch (Exception e) {
            return "❌ Ollama indisponible. Vérifie que `ollama serve` est lancé.\n(" + e.getMessage() + ")";
        }
    }

    /** Build the level-specific prompt — same logic as PHP version */
    private String buildRoadmapPrompt(String level, String coursTitre) {
        return "Give me a 3-step learning plan for '" + coursTitre + "' (" + level + "). Max 30 words.";
    }
}