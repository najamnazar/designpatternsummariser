package evaluation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * Evaluates and compares SWUM-generated summaries with SimpleNLG-generated summaries
 * using BLEU-4 and NIST metrics.
 */
public class SummaryEvaluator {
    
    private BLEU4Evaluator bleuEvaluator;
    private NISTEvaluator nistEvaluator;
    private ObjectMapper objectMapper;
    
    public SummaryEvaluator() {
        this.bleuEvaluator = new BLEU4Evaluator();
        this.nistEvaluator = new NISTEvaluator();
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Evaluates a single project by comparing SWUM and SimpleNLG summaries
     */
    public EvaluationResult evaluateProject(String originalJsonPath, String swumJsonPath) throws IOException {
        // Load original JSON (with SimpleNLG summary)
        JsonNode originalJson = objectMapper.readTree(new File(originalJsonPath));
        
        // Load SWUM JSON
        JsonNode swumJson = objectMapper.readTree(new File(swumJsonPath));
        
        EvaluationResult result = new EvaluationResult();
        result.setProjectName(extractProjectName(originalJsonPath));
        
        // Extract summaries
        String simpleNLGSummary = extractSimpleNLGSummary(originalJson);
        String swumSummary = extractSWUMSummary(swumJson);
        
        if (simpleNLGSummary == null || simpleNLGSummary.trim().isEmpty()) {
            result.addError("No SimpleNLG summary found");
            return result;
        }
        
        if (swumSummary == null || swumSummary.trim().isEmpty()) {
            result.addError("No SWUM summary found");
            return result;
        }
        
        result.setSimpleNLGSummary(simpleNLGSummary);
        result.setSWUMSummary(swumSummary);
        
        // Calculate BLEU-4 scores (using SimpleNLG as reference)
        double bleuScore = bleuEvaluator.calculateBLEU4(swumSummary, simpleNLGSummary);
        Map<String, Double> bleuDetails = bleuEvaluator.calculateDetailedScores(swumSummary, simpleNLGSummary);
        
        result.setBleuScore(bleuScore);
        result.setBleuDetails(bleuDetails);
        
        // Calculate NIST scores (using SimpleNLG as reference)
        double nistScore = nistEvaluator.calculateNIST(swumSummary, simpleNLGSummary);
        Map<String, Double> nistDetails = nistEvaluator.calculateDetailedScores(swumSummary, simpleNLGSummary);
        
        result.setNistScore(nistScore);
        result.setNistDetails(nistDetails);
        
        // Calculate reverse scores (using SWUM as reference)
        double reverseBLEU = bleuEvaluator.calculateBLEU4(simpleNLGSummary, swumSummary);
        double reverseNIST = nistEvaluator.calculateNIST(simpleNLGSummary, swumSummary);
        
        result.setReverseBLEU(reverseBLEU);
        result.setReverseNIST(reverseNIST);
        
        // Calculate similarity metrics
        result.setSummaryLengthRatio(calculateLengthRatio(swumSummary, simpleNLGSummary));
        result.setWordOverlapRatio(calculateWordOverlap(swumSummary, simpleNLGSummary));
        
        return result;
    }
    
    /**
     * Evaluates all projects in given directories
     */
    public void evaluateAllProjects(String originalDir, String swumDir, String outputDir) throws IOException {
        File originalDirectory = new File(originalDir);
        File swumDirectory = new File(swumDir);
        File outputDirectory = new File(outputDir);
        
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs();
        }
        
        List<EvaluationResult> results = new ArrayList<>();
        
        // Find matching files
        File[] originalFiles = originalDirectory.listFiles((dir, name) -> name.endsWith(".json"));
        if (originalFiles == null) {
            throw new IOException("No JSON files found in " + originalDir);
        }
        
        for (File originalFile : originalFiles) {
            String swumFileName = originalFile.getName().replace(".json", "_swum.json");
            File swumFile = new File(swumDirectory, swumFileName);
            
            if (!swumFile.exists()) {
                System.err.println("Warning: SWUM file not found for " + originalFile.getName());
                continue;
            }
            
            try {
                EvaluationResult result = evaluateProject(originalFile.getAbsolutePath(), swumFile.getAbsolutePath());
                results.add(result);
                System.out.println("Evaluated: " + result.getProjectName());
            } catch (Exception e) {
                System.err.println("Error evaluating " + originalFile.getName() + ": " + e.getMessage());
            }
        }
        
        // Generate comprehensive report
        generateReport(results, outputDirectory);
        
        System.out.println("Evaluation completed. Results saved to " + outputDir);
    }
    
    /**
     * Generates comprehensive evaluation report
     */
    private void generateReport(List<EvaluationResult> results, File outputDir) throws IOException {
        // Individual results
        ObjectNode allResults = objectMapper.createObjectNode();
        allResults.put("evaluation_timestamp", System.currentTimeMillis());
        allResults.put("total_projects", results.size());
        
        ObjectNode projectResults = objectMapper.createObjectNode();
        
        for (EvaluationResult result : results) {
            ObjectNode projectResult = objectMapper.createObjectNode();
            
            projectResult.put("project_name", result.getProjectName());
            projectResult.put("bleu_score", result.getBleuScore());
            projectResult.put("nist_score", result.getNistScore());
            projectResult.put("reverse_bleu", result.getReverseBLEU());
            projectResult.put("reverse_nist", result.getReverseNIST());
            projectResult.put("length_ratio", result.getSummaryLengthRatio());
            projectResult.put("word_overlap", result.getWordOverlapRatio());
            
            // Add detailed scores
            ObjectNode bleuDetails = objectMapper.createObjectNode();
            for (Map.Entry<String, Double> entry : result.getBleuDetails().entrySet()) {
                bleuDetails.put(entry.getKey(), entry.getValue());
            }
            projectResult.set("bleu_details", bleuDetails);
            
            ObjectNode nistDetails = objectMapper.createObjectNode();
            for (Map.Entry<String, Double> entry : result.getNistDetails().entrySet()) {
                nistDetails.put(entry.getKey(), entry.getValue());
            }
            projectResult.set("nist_details", nistDetails);
            
            projectResult.put("simplenlg_summary", result.getSimpleNLGSummary());
            projectResult.put("swum_summary", result.getSWUMSummary());
            
            if (!result.getErrors().isEmpty()) {
                projectResult.put("errors", String.join("; ", result.getErrors()));
            }
            
            projectResults.set(result.getProjectName(), projectResult);
        }
        
        allResults.set("project_results", projectResults);
        
        // Calculate statistics
        ObjectNode statistics = calculateStatistics(results);
        allResults.set("statistics", statistics);
        
        // Write individual results
        try (FileWriter writer = new FileWriter(new File(outputDir, "evaluation_results.json"))) {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(writer, allResults);
        }
        
        // Write summary statistics
        writeSummaryReport(statistics, outputDir);
        
        // Write CSV for easy analysis
        writeCSVReport(results, outputDir);
    }
    
    /**
     * Calculates summary statistics
     */
    private ObjectNode calculateStatistics(List<EvaluationResult> results) {
        ObjectNode stats = objectMapper.createObjectNode();
        
        if (results.isEmpty()) {
            return stats;
        }
        
        // BLEU statistics
        double[] bleuScores = results.stream().mapToDouble(EvaluationResult::getBleuScore).toArray();
        stats.put("bleu_mean", Arrays.stream(bleuScores).average().orElse(0.0));
        stats.put("bleu_median", calculateMedian(bleuScores));
        stats.put("bleu_std", calculateStandardDeviation(bleuScores));
        stats.put("bleu_min", Arrays.stream(bleuScores).min().orElse(0.0));
        stats.put("bleu_max", Arrays.stream(bleuScores).max().orElse(0.0));
        
        // NIST statistics
        double[] nistScores = results.stream().mapToDouble(EvaluationResult::getNistScore).toArray();
        stats.put("nist_mean", Arrays.stream(nistScores).average().orElse(0.0));
        stats.put("nist_median", calculateMedian(nistScores));
        stats.put("nist_std", calculateStandardDeviation(nistScores));
        stats.put("nist_min", Arrays.stream(nistScores).min().orElse(0.0));
        stats.put("nist_max", Arrays.stream(nistScores).max().orElse(0.0));
        
        // Length ratio statistics
        double[] lengthRatios = results.stream().mapToDouble(EvaluationResult::getSummaryLengthRatio).toArray();
        stats.put("length_ratio_mean", Arrays.stream(lengthRatios).average().orElse(0.0));
        stats.put("length_ratio_std", calculateStandardDeviation(lengthRatios));
        
        // Word overlap statistics
        double[] wordOverlaps = results.stream().mapToDouble(EvaluationResult::getWordOverlapRatio).toArray();
        stats.put("word_overlap_mean", Arrays.stream(wordOverlaps).average().orElse(0.0));
        stats.put("word_overlap_std", calculateStandardDeviation(wordOverlaps));
        
        return stats;
    }
    
    /**
     * Writes a human-readable summary report
     */
    private void writeSummaryReport(ObjectNode statistics, File outputDir) throws IOException {
        try (FileWriter writer = new FileWriter(new File(outputDir, "evaluation_summary.txt"))) {
            writer.write("=== SWUM vs SimpleNLG Evaluation Summary ===\n\n");
            
            writer.write("BLEU-4 Scores:\n");
            writer.write(String.format("  Mean: %.4f\n", statistics.get("bleu_mean").asDouble()));
            writer.write(String.format("  Median: %.4f\n", statistics.get("bleu_median").asDouble()));
            writer.write(String.format("  Std Dev: %.4f\n", statistics.get("bleu_std").asDouble()));
            writer.write(String.format("  Range: %.4f - %.4f\n\n", 
                statistics.get("bleu_min").asDouble(), statistics.get("bleu_max").asDouble()));
            
            writer.write("NIST Scores:\n");
            writer.write(String.format("  Mean: %.4f\n", statistics.get("nist_mean").asDouble()));
            writer.write(String.format("  Median: %.4f\n", statistics.get("nist_median").asDouble()));
            writer.write(String.format("  Std Dev: %.4f\n", statistics.get("nist_std").asDouble()));
            writer.write(String.format("  Range: %.4f - %.4f\n\n", 
                statistics.get("nist_min").asDouble(), statistics.get("nist_max").asDouble()));
            
            writer.write("Summary Length Ratio (SWUM/SimpleNLG):\n");
            writer.write(String.format("  Mean: %.4f\n", statistics.get("length_ratio_mean").asDouble()));
            writer.write(String.format("  Std Dev: %.4f\n\n", statistics.get("length_ratio_std").asDouble()));
            
            writer.write("Word Overlap Ratio:\n");
            writer.write(String.format("  Mean: %.4f\n", statistics.get("word_overlap_mean").asDouble()));
            writer.write(String.format("  Std Dev: %.4f\n", statistics.get("word_overlap_std").asDouble()));
        }
    }
    
    /**
     * Writes CSV report for easy spreadsheet analysis
     */
    private void writeCSVReport(List<EvaluationResult> results, File outputDir) throws IOException {
        try (FileWriter writer = new FileWriter(new File(outputDir, "evaluation_results.csv"))) {
            // Write header
            writer.write("Project,BLEU-4,NIST,Reverse_BLEU,Reverse_NIST,Length_Ratio,Word_Overlap,BLEU-1,BLEU-2,BLEU-3,NIST-1,NIST-2,NIST-3,NIST-4,NIST-5\n");
            
            // Write data
            for (EvaluationResult result : results) {
                writer.write(String.format("%s,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f\n",
                    result.getProjectName(),
                    result.getBleuScore(),
                    result.getNistScore(),
                    result.getReverseBLEU(),
                    result.getReverseNIST(),
                    result.getSummaryLengthRatio(),
                    result.getWordOverlapRatio(),
                    result.getBleuDetails().getOrDefault("BLEU-1", 0.0),
                    result.getBleuDetails().getOrDefault("BLEU-2", 0.0),
                    result.getBleuDetails().getOrDefault("BLEU-3", 0.0),
                    result.getNistDetails().getOrDefault("NIST-1", 0.0),
                    result.getNistDetails().getOrDefault("NIST-2", 0.0),
                    result.getNistDetails().getOrDefault("NIST-3", 0.0),
                    result.getNistDetails().getOrDefault("NIST-4", 0.0),
                    result.getNistDetails().getOrDefault("NIST-5", 0.0)
                ));
            }
        }
    }
    
    // Helper methods
    private String extractProjectName(String filePath) {
        return new File(filePath).getName().replace(".json", "");
    }
    
    private String extractSimpleNLGSummary(JsonNode json) {
        JsonNode summaryNode = json.get("final_summary");
        return summaryNode != null ? summaryNode.asText() : null;
    }
    
    private String extractSWUMSummary(JsonNode json) {
        JsonNode summaryNode = json.get("swum_project_summary");
        return summaryNode != null ? summaryNode.asText() : null;
    }
    
    private double calculateLengthRatio(String swumSummary, String simpleNLGSummary) {
        int swumLength = swumSummary.split("\\s+").length;
        int simpleNLGLength = simpleNLGSummary.split("\\s+").length;
        return simpleNLGLength > 0 ? (double) swumLength / simpleNLGLength : 0.0;
    }
    
    private double calculateWordOverlap(String summary1, String summary2) {
        Set<String> words1 = new HashSet<>(Arrays.asList(summary1.toLowerCase().split("\\s+")));
        Set<String> words2 = new HashSet<>(Arrays.asList(summary2.toLowerCase().split("\\s+")));
        
        Set<String> intersection = new HashSet<>(words1);
        intersection.retainAll(words2);
        
        Set<String> union = new HashSet<>(words1);
        union.addAll(words2);
        
        return union.size() > 0 ? (double) intersection.size() / union.size() : 0.0;
    }
    
    private double calculateMedian(double[] values) {
        Arrays.sort(values);
        int n = values.length;
        if (n % 2 == 0) {
            return (values[n/2 - 1] + values[n/2]) / 2.0;
        } else {
            return values[n/2];
        }
    }
    
    private double calculateStandardDeviation(double[] values) {
        double mean = Arrays.stream(values).average().orElse(0.0);
        double variance = Arrays.stream(values)
            .map(x -> Math.pow(x - mean, 2))
            .average().orElse(0.0);
        return Math.sqrt(variance);
    }
}