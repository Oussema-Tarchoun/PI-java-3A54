package Services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import Models.Repas;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class AnalyseService {

    private static final String API_KEY = "//"; // même clé que GroqService
    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL   = "llama-3.3-70b-versatile";
    private final ObjectMapper mapper   = new ObjectMapper();

    public JsonNode analyserSemaine(List<Repas> repas) throws IOException {

        // 1. Construire la liste des repas pour le prompt
        StringBuilder repasList = new StringBuilder();
        for (Repas r : repas) {
            repasList.append("- ").append(r.getNom())
                    .append(" | ").append(r.getCalories()).append(" kcal")
                    .append(" | ").append(r.getType())
                    .append(" | ").append(r.getDate()).append("\n");
        }

        // 2. Stats simples
        int totalRepas = repas.size();
        long totalJours = repas.stream().map(Repas::getDate).distinct().count();
        int kcalMoy = totalJours > 0
                ? repas.stream().mapToInt(Repas::getCalories).sum() / (int) totalJours : 0;

        String prompt = "Tu es un nutritionniste expert. Analyse ces repas et réponds UNIQUEMENT en JSON valide, sans markdown, sans explication.\n\n"
                + "Repas de la semaine:\n" + repasList
                + "\nStats: " + totalRepas + " repas, " + totalJours + " jours, " + kcalMoy + " kcal/jour moy.\n\n"
                + "Format JSON OBLIGATOIRE:\n"
                + "{\n"
                + "  \"score\": 75,\n"
                + "  \"resume\": \"Bon, repas équilibré mais faible en calories\",\n"
                + "  \"points_positifs\": [\"point 1\", \"point 2\"],\n"
                + "  \"a_ameliorer\": [\"point 1\", \"point 2\"],\n"
                + "  \"conseils\": [\"conseil 1\", \"conseil 2\"],\n"
                + "  \"analyse_par_jour\": [\n"
                + "    {\"date\": \"2026-04-23\", \"commentaire\": \"...\"}\n"
                + "  ],\n"
                + "  \"stats\": {\n"
                + "    \"total_repas\": " + totalRepas + ",\n"
                + "    \"total_jours\": " + totalJours + ",\n"
                + "    \"kcal_moy_par_jour\": " + kcalMoy + "\n"
                + "  }\n"
                + "}";

        // 3. Appel API
        ObjectNode body = mapper.createObjectNode();
        body.put("model", MODEL);
        body.put("temperature", 0.4);
        body.put("max_tokens", 1500);
        ArrayNode messages = mapper.createArrayNode();
        ObjectNode msg = mapper.createObjectNode();
        msg.put("role", "user");
        msg.put("content", prompt);
        messages.add(msg);
        body.set("messages", messages);

        HttpURLConnection conn = (HttpURLConnection) new URL(API_URL).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
        conn.setDoOutput(true);
        conn.setConnectTimeout(30_000);
        conn.setReadTimeout(60_000);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(mapper.writeValueAsBytes(body));
        }

        InputStream stream = conn.getResponseCode() < 300
                ? conn.getInputStream() : conn.getErrorStream();
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line; while ((line = br.readLine()) != null) sb.append(line);
        }

        String content = mapper.readTree(sb.toString())
                .path("choices").get(0).path("message").path("content").asText();

        // Nettoyer si l'IA met du markdown quand même
        content = content.replaceAll("```json", "").replaceAll("```", "").trim();
        return mapper.readTree(content);
    }
}