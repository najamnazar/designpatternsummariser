package dpsSwum.swum;

import dpsSwum.swum.grammar.SWUMGrammarParser;
import dpsSwum.swum.model.SWUMStructure;

/**
 * Demonstration class to run a simple SWUM evaluation test
 */
public class SWUMDemo {
    
    public static void main(String[] args) {
        System.out.println("=== SWUM (Software Word Usage Model) Demonstration ===\n");
        
        // Test SWUM grammar parsing
        testSWUMGrammarParsing();
        
        // Test SWUM summarization
        testSWUMSummarization();
        
        // Test evaluation metrics
        testEvaluationMetrics();
        
        System.out.println("\n=== SWUM Demo Complete ===");
        System.out.println("To run the full evaluation pipeline:");
        System.out.println("java -cp target/classes swum.SWUMEvaluationPipeline");
    }
    
    /**
     * Test SWUM grammar parsing functionality
     */
    private static void testSWUMGrammarParsing() {
        System.out.println("1. Testing SWUM Grammar Parsing:");
        
        try {
            SWUMGrammarParser parser = new SWUMGrammarParser();
            
            String[] testMethods = {
                "getUserAccountFromDatabase",
                "createFactoryInstance", 
                "validateUserInput",
                "processPaymentTransaction",
                "sendEmailNotification"
            };
            
            for (String methodName : testMethods) {
                SWUMStructure structure = parser.parseMethodName(methodName, "TestClass");
                System.out.printf("  Method: %-25s -> Actions: %s, Objects: %s\n", 
                    methodName, 
                    String.join(", ", structure.getActions()),
                    String.join(", ", structure.getObjects())
                );
            }
            
            System.out.printf("  Grammar rules applied: %d\n", parser.getAppliedRulesCount());
            
        } catch (Exception e) {
            System.err.println("  Error in grammar parsing: " + e.getMessage());
        }
        
        System.out.println();
    }
    
    /**
     * Test SWUM summarization
     */
    private static void testSWUMSummarization() {
        System.out.println("2. Testing SWUM Summarization:");
        
        try {
            SWUMSummarizer summarizer = new SWUMSummarizer();
            
            String[] testMethods = {
                "createUserAccount",
                "validatePassword", 
                "processOrder",
                "sendNotification"
            };
            
            String[] testClasses = {
                "UserAccountFactory",
                "PaymentProcessor",
                "EmailAdapter", 
                "OrderBuilder"
            };
            
            System.out.println("  Method Summaries:");
            for (String method : testMethods) {
                String summary = summarizer.generateMethodSummary(method);
                System.out.printf("    %-20s -> %s\n", method, summary);
            }
            
            System.out.println("\n  Class Summaries:");
            for (String className : testClasses) {
                String summary = summarizer.generateClassSummary(className);
                System.out.printf("    %-20s -> %s\n", className, summary);
            }
            
        } catch (Exception e) {
            System.err.println("  Error in summarization: " + e.getMessage());
        }
        
        System.out.println();
    }
    
    /**
     * Test evaluation metrics
     */
    private static void testEvaluationMetrics() {
        System.out.println("3. Testing Evaluation Metrics:");
        
        try {
            evaluation.BLEU4Evaluator bleuEvaluator = new evaluation.BLEU4Evaluator();
            evaluation.NISTEvaluator nistEvaluator = new evaluation.NISTEvaluator();
            
            // Test sentences
            String candidate = "This method creates user account from database";
            String reference = "The function establishes user account using database";
            
            double bleuScore = bleuEvaluator.calculateBLEU4(candidate, reference);
            double nistScore = nistEvaluator.calculateNIST(candidate, reference);
            
            System.out.printf("  Candidate: %s\n", candidate);
            System.out.printf("  Reference: %s\n", reference);
            System.out.printf("  BLEU-4 Score: %.4f\n", bleuScore);
            System.out.printf("  NIST Score: %.4f\n", nistScore);
            
            // Detailed scores
            java.util.Map<String, Double> bleuDetails = bleuEvaluator.calculateDetailedScores(candidate, reference);
            java.util.Map<String, Double> nistDetails = nistEvaluator.calculateDetailedScores(candidate, reference);
            
            System.out.println("\n  Detailed BLEU Scores:");
            bleuDetails.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith("BLEU-"))
                .forEach(entry -> System.out.printf("    %s: %.4f\n", entry.getKey(), entry.getValue()));
                
            System.out.println("\n  Detailed NIST Scores:");
            nistDetails.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith("NIST-"))
                .forEach(entry -> System.out.printf("    %s: %.4f\n", entry.getKey(), entry.getValue()));
            
        } catch (Exception e) {
            System.err.println("  Error in evaluation: " + e.getMessage());
        }
        
        System.out.println();
    }
}
