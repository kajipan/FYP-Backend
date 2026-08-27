package com.mechinow.controller;

import com.mechinow.BikeClassificationService;
import com.mechinow.GeminiService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final BikeClassificationService bikeClassificationService;
    private final GeminiService geminiService;

    public ChatController(BikeClassificationService bikeClassificationService, GeminiService geminiService) {
        this.bikeClassificationService = bikeClassificationService;
        this.geminiService = geminiService;
    }

    @PostMapping("/classify-bike")
    public Map<String, Object> classifyBike(@RequestParam("image") MultipartFile image) {
        try {
            String bodyType = bikeClassificationService.classify(image);
            return Map.of("bodyType", bodyType, "success", true);
        } catch (Exception e) {
            return Map.of("bodyType", "commuter_standard", "success", false, "error", e.getMessage());
        }
    }

    @PostMapping("/diagnose")
    public Map<String, Object> diagnose(@RequestBody Map<String, Object> payload) {
        String issueType = (String) payload.getOrDefault("issueType", "other");
        String bodyType = (String) payload.getOrDefault("bodyType", "unknown");
        String causeName = (String) payload.get("causeName");
        String freeText = (String) payload.get("freeText");

        String prompt;
        if (freeText != null) {
            prompt = "A motorbike (" + bodyType + ") owner reports this problem: \"" + freeText + "\". "
                    + "In under 80 words, give a friendly, simple explanation of the likely cause and a safe next step. "
                    + "If it sounds dangerous to keep riding, say so clearly.";
        } else {
            prompt = "A motorbike (" + bodyType + ") has a '" + issueType + "' issue. "
                    + "The likely technical cause is: " + causeName + ". "
                    + "In under 80 words, explain this simply and warmly to a non-technical rider, "
                    + "and tell them whether it's safe to keep riding.";
        }

        String solutionText = geminiService.generateSolutionText(prompt);
        if (solutionText == null) {
            solutionText = "We've identified the likely cause as: " + causeName
                    + ". Please contact a mechanic for a proper inspection.";
        }

        return Map.of("solutionText", solutionText.trim());
    }
}