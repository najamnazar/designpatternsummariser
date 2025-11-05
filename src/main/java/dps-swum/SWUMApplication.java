package dpsSwum;

import java.io.File;
import java.io.IOException;

import dpsSwum.swum.SWUMEvaluationPipeline;

/**
 * SWUM-based Design Pattern Summarizer Application
 * Processes JSON files using Software Word Usage Model (SWUM) for summarization
 */
public class SWUMApplication {

    public static void main(String[] args) throws IOException {
        System.out.println("Starting SWUM-based Design Pattern Summarizer...");
        
        // Check if JSON output directory exists
        File jsonOutputDir = new File("output/json-output");
        if (!jsonOutputDir.exists()) {
            System.err.println("Error: JSON output directory not found at: " + jsonOutputDir.getAbsolutePath());
            System.err.println("Please run the DPS-NLG project first to generate JSON files.");
            return;
        }
        
        // Create SWUM output directory
        File swumOutputDir = new File("swum-output");
        if (!swumOutputDir.exists()) {
            swumOutputDir.mkdirs();
        }
        
        try {
            // Run SWUM evaluation pipeline
            SWUMEvaluationPipeline pipeline = new SWUMEvaluationPipeline();
            pipeline.runCompletePipeline();
            
            System.out.println("\nSWUM processing completed successfully!");
            System.out.println("SWUM summaries available in: " + swumOutputDir.getAbsolutePath());
            
        } catch (Exception e) {
            System.err.println("Error during SWUM processing: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
