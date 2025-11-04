package swum;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import evaluation.SummaryEvaluator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Main class to run the complete SWUM evaluation pipeline
 */
public class SWUMEvaluationPipeline {
    
    private SWUMSummarizer swumSummarizer;
    private SummaryEvaluator summaryEvaluator;
    private ObjectMapper objectMapper;
    
    private static final String INPUT_DIR = "output";
    private static final String SWUM_OUTPUT_DIR = "swum-output";
    private static final String EVALUATION_OUTPUT_DIR = "evaluation-results";
    
    public SWUMEvaluationPipeline() {
        this.swumSummarizer = new SWUMSummarizer();
        this.summaryEvaluator = new SummaryEvaluator();
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Runs the complete pipeline: SWUM processing + evaluation
     */
    public void runCompletePipeline() {
        try {
            System.out.println("=== SWUM Evaluation Pipeline ===");
            
            // Step 1: Create output directories
            createOutputDirectories();
            
            // Step 2: Process all JSON files with SWUM
            System.out.println("\nStep 1: Processing files with SWUM...");
            processAllFilesWithSWUM();
            
            // Step 3: Run evaluation comparison
            System.out.println("\nStep 2: Running evaluation comparison...");
            runEvaluation();
            
            // Step 4: Generate summary report
            System.out.println("\nStep 3: Generating summary report...");
            generateFinalReport();
            
            System.out.println("\n=== Pipeline Complete ===");
            System.out.println("Results saved to: " + EVALUATION_OUTPUT_DIR);
            
        } catch (Exception e) {
            System.err.println("Pipeline failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Processes a single JSON file with SWUM
     */
    public void processSingleFile(String inputFilePath, String outputFilePath) throws IOException {
        System.out.println("Processing: " + inputFilePath);
        
        // Read original JSON
        JsonNode originalJson = objectMapper.readTree(new File(inputFilePath));
        
        // Process with SWUM
        JsonNode swumJson = swumSummarizer.processProjectJson(originalJson);
        
        // Write SWUM output
        try (FileWriter writer = new FileWriter(outputFilePath)) {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(writer, swumJson);
        }
        
        System.out.println("SWUM output saved to: " + outputFilePath);
    }
    
    /**
     * Processes all JSON files in the input directory
     */
    private void processAllFilesWithSWUM() throws IOException {
        File inputDir = new File(INPUT_DIR);
        File outputDir = new File(SWUM_OUTPUT_DIR);
        
        if (!inputDir.exists() || !inputDir.isDirectory()) {
            throw new IOException("Input directory not found: " + INPUT_DIR);
        }
        
        File[] jsonFiles = inputDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (jsonFiles == null || jsonFiles.length == 0) {
            throw new IOException("No JSON files found in " + INPUT_DIR);
        }
        
        int processed = 0;
        int total = jsonFiles.length;
        
        for (File jsonFile : jsonFiles) {
            try {
                String outputFileName = jsonFile.getName().replace(".json", "_swum.json");
                File outputFile = new File(outputDir, outputFileName);
                
                processSingleFile(jsonFile.getAbsolutePath(), outputFile.getAbsolutePath());
                processed++;
                
                System.out.printf("Progress: %d/%d files processed\n", processed, total);
                
            } catch (Exception e) {
                System.err.println("Error processing " + jsonFile.getName() + ": " + e.getMessage());
            }
        }
        
        System.out.printf("SWUM processing complete: %d/%d files processed successfully\n", processed, total);
    }
    
    /**
     * Runs the evaluation comparison between SWUM and SimpleNLG
     */
    private void runEvaluation() throws IOException {
        summaryEvaluator.evaluateAllProjects(INPUT_DIR, SWUM_OUTPUT_DIR, EVALUATION_OUTPUT_DIR);
    }
    
    /**
     * Creates necessary output directories
     */
    private void createOutputDirectories() throws IOException {
        Files.createDirectories(Paths.get(SWUM_OUTPUT_DIR));
        Files.createDirectories(Paths.get(EVALUATION_OUTPUT_DIR));
        System.out.println("Output directories created");
    }
    
    /**
     * Generates a final comprehensive report
     */
    private void generateFinalReport() throws IOException {
        File evaluationResultsFile = new File(EVALUATION_OUTPUT_DIR, "evaluation_results.json");
        if (!evaluationResultsFile.exists()) {
            System.err.println("Warning: Evaluation results file not found");
            return;
        }
        
        JsonNode evaluationResults = objectMapper.readTree(evaluationResultsFile);
        
        // Create final report
        ObjectNode finalReport = objectMapper.createObjectNode();
        finalReport.put("pipeline_completion_time", System.currentTimeMillis());
        finalReport.put("evaluation_summary", "SWUM vs SimpleNLG Comparison");
        
        // Extract key statistics
        JsonNode stats = evaluationResults.get("statistics");
        if (stats != null) {
            ObjectNode summary = objectMapper.createObjectNode();
            summary.put("total_projects", evaluationResults.get("total_projects").asInt());
            summary.put("mean_bleu_score", stats.get("bleu_mean").asDouble());
            summary.put("mean_nist_score", stats.get("nist_mean").asDouble());
            summary.put("mean_length_ratio", stats.get("length_ratio_mean").asDouble());
            summary.put("mean_word_overlap", stats.get("word_overlap_mean").asDouble());
            
            finalReport.set("key_metrics", summary);
        }
        
        // Add methodology description
        ObjectNode methodology = objectMapper.createObjectNode();
        methodology.put("swum_approach", "Software Word Usage Model with grammar-based parsing");
        methodology.put("baseline", "SimpleNLG natural language generation");
        methodology.put("evaluation_metrics", "BLEU-4 and NIST scores for summary quality assessment");
        methodology.put("reference_direction", "SimpleNLG summaries used as reference for SWUM evaluation");
        
        finalReport.set("methodology", methodology);
        
        // Write final report
        try (FileWriter writer = new FileWriter(new File(EVALUATION_OUTPUT_DIR, "final_report.json"))) {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(writer, finalReport);
        }
        
        // Write human-readable summary
        writeHumanReadableSummary(stats);
    }
    
    /**
     * Writes a human-readable summary report
     */
    private void writeHumanReadableSummary(JsonNode stats) throws IOException {
        try (FileWriter writer = new FileWriter(new File(EVALUATION_OUTPUT_DIR, "pipeline_summary.txt"))) {
            writer.write("=== SWUM Evaluation Pipeline Results ===\n\n");
            writer.write("This pipeline compared SWUM (Software Word Usage Model) generated summaries\n");
            writer.write("with SimpleNLG generated summaries using BLEU-4 and NIST evaluation metrics.\n\n");
            
            if (stats != null) {
                writer.write("KEY FINDINGS:\n");
                writer.write(String.format("  • Average BLEU-4 Score: %.4f\n", stats.get("bleu_mean").asDouble()));
                writer.write(String.format("  • Average NIST Score: %.4f\n", stats.get("nist_mean").asDouble()));
                writer.write(String.format("  • Average Length Ratio: %.2f\n", stats.get("length_ratio_mean").asDouble()));
                writer.write(String.format("  • Average Word Overlap: %.2f%%\n", stats.get("word_overlap_mean").asDouble() * 100));
                
                writer.write("\nSCORE INTERPRETATION:\n");
                writer.write("  • BLEU-4: Measures n-gram precision (0-1, higher is better)\n");
                writer.write("  • NIST: Information-weighted evaluation (0-∞, higher is better)\n");
                writer.write("  • Length Ratio: SWUM length / SimpleNLG length\n");
                writer.write("  • Word Overlap: Jaccard similarity of vocabulary\n\n");
                
                writer.write("METHODOLOGY:\n");
                writer.write("  • SWUM uses grammar-based parsing with software-specific vocabulary\n");
                writer.write("  • SimpleNLG uses natural language generation templates\n");
                writer.write("  • Evaluation treats SimpleNLG as reference for comparison\n");
                writer.write("  • Both forward and reverse evaluations calculated\n\n");
            }
            
            writer.write("FILES GENERATED:\n");
            writer.write("  • evaluation_results.json - Complete detailed results\n");
            writer.write("  • evaluation_results.csv - Data for spreadsheet analysis\n");
            writer.write("  • evaluation_summary.txt - Statistical summary\n");
            writer.write("  • final_report.json - Structured pipeline report\n");
            writer.write("  • pipeline_summary.txt - This human-readable summary\n");
        }
    }
    
    /**
     * Main method to run the pipeline
     */
    public static void main(String[] args) {
        SWUMEvaluationPipeline pipeline = new SWUMEvaluationPipeline();
        
        if (args.length == 0) {
            // Run complete pipeline
            pipeline.runCompletePipeline();
        } else if (args.length == 2) {
            // Process single file
            try {
                pipeline.processSingleFile(args[0], args[1]);
            } catch (IOException e) {
                System.err.println("Error processing file: " + e.getMessage());
                System.exit(1);
            }
        } else {
            System.out.println("Usage:");
            System.out.println("  java SWUMEvaluationPipeline                    # Run complete pipeline");
            System.out.println("  java SWUMEvaluationPipeline <input> <output>   # Process single file");
            System.exit(1);
        }
    }
    
    /**
     * Utility method to list available input files
     */
    public static void listAvailableFiles() {
        File inputDir = new File(INPUT_DIR);
        if (!inputDir.exists()) {
            System.out.println("Input directory not found: " + INPUT_DIR);
            return;
        }
        
        File[] jsonFiles = inputDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (jsonFiles == null || jsonFiles.length == 0) {
            System.out.println("No JSON files found in " + INPUT_DIR);
            return;
        }
        
        System.out.println("Available JSON files:");
        for (File file : jsonFiles) {
            System.out.println("  " + file.getName());
        }
    }
}