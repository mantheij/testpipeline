package com.mycompany.app;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class App {
    private static final String DATASET_URL = "http://localhost:8080/v1/dataset";
    private static final String RESULT_URL = "http://localhost:8080/v1/result";

    public static void main(String[] args) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(DATASET_URL))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        String responseBody = response.body();

        // JSON Parsing with Jackson
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(responseBody);

        Map<String, Long> customerUsage = new HashMap<>();
        Map<String, Map<String, Long>> workloadTimes = new HashMap<>();

        JsonNode events = root.get("events");
        Iterator<JsonNode> iterator = events.elements();

        while (iterator.hasNext()) {
            JsonNode event = iterator.next();
            String customerId = event.get("customerId").asText();
            String workloadId = event.get("workloadId").asText();
            long timestamp = event.get("timestamp").asLong();
            String eventType = event.get("eventType").asText();

            workloadTimes.putIfAbsent(customerId, new HashMap<>());
            Map<String, Long> customerWorkloads = workloadTimes.get(customerId);

            if (eventType.equals("start")) {
                customerWorkloads.put(workloadId, timestamp);
            } else if (eventType.equals("stop")) {
                if (customerWorkloads.containsKey(workloadId)) {
                    long startTime = customerWorkloads.get(workloadId);
                    long runtime = timestamp - startTime;
                    customerUsage.put(customerId, customerUsage.getOrDefault(customerId, 0L) + runtime);
                    customerWorkloads.remove(workloadId);
                }
            }
        }

        // Prepare result JSON
        JsonNode resultPayload = mapper.createObjectNode()
            .putPOJO("result", customerUsage.entrySet().stream()
                .map(entry -> {
                    return mapper.createObjectNode()
                        .put("customerId", entry.getKey())
                        .put("consumption", entry.getValue());
                })
                .toArray());

        // Sending results
        HttpRequest resultRequest = HttpRequest.newBuilder()
                .uri(URI.create(RESULT_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(resultPayload.toString()))
                .build();

        HttpResponse<String> resultResponse = client.send(resultRequest, HttpResponse.BodyHandlers.ofString());
        System.out.println("Response: " + resultResponse.statusCode());
        System.out.println(resultResponse.body());
    }
}
