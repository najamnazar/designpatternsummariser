package evaluation;

import java.util.*;

/**
 * Implementation of BLEU-4 (Bilingual Evaluation Understudy) score calculation
 * for evaluating the quality of machine-generated text summaries.
 */
public class BLEU4Evaluator {
    
    /**
     * Calculates BLEU-4 score between candidate and reference summaries
     * 
     * @param candidate The generated summary text
     * @param reference The reference (ground truth) summary text
     * @return BLEU-4 score between 0.0 and 1.0
     */
    public double calculateBLEU4(String candidate, String reference) {
        return calculateBLEU4(candidate, Arrays.asList(reference));
    }
    
    /**
     * Calculates BLEU-4 score between candidate and multiple reference summaries
     * 
     * @param candidate The generated summary text
     * @param references List of reference summary texts
     * @return BLEU-4 score between 0.0 and 1.0
     */
    public double calculateBLEU4(String candidate, List<String> references) {
        if (candidate == null || candidate.trim().isEmpty() || 
            references == null || references.isEmpty()) {
            return 0.0;
        }
        
        List<String> candidateTokens = tokenize(candidate);
        if (candidateTokens.isEmpty()) {
            return 0.0;
        }
        
        List<List<String>> referenceTokensList = new ArrayList<>();
        for (String reference : references) {
            referenceTokensList.add(tokenize(reference));
        }
        
        // Calculate precision for n-grams (1 to 4)
        double[] precisions = new double[4];
        
        for (int n = 1; n <= 4; n++) {
            precisions[n - 1] = calculateNGramPrecision(candidateTokens, referenceTokensList, n);
        }
        
        // Calculate brevity penalty
        double brevityPenalty = calculateBrevityPenalty(candidateTokens, referenceTokensList);
        
        // Calculate geometric mean of precisions
        double geometricMean = 1.0;
        for (double precision : precisions) {
            if (precision == 0.0) {
                return 0.0; // If any precision is 0, BLEU score is 0
            }
            geometricMean *= Math.pow(precision, 0.25); // Equal weights (1/4) for each n-gram
        }
        
        return brevityPenalty * geometricMean;
    }
    
    /**
     * Calculates n-gram precision for given n
     */
    private double calculateNGramPrecision(List<String> candidate, 
                                         List<List<String>> references, int n) {
        if (candidate.size() < n) {
            return 0.0;
        }
        
        // Generate candidate n-grams
        Map<List<String>, Integer> candidateNGrams = generateNGrams(candidate, n);
        
        // Generate reference n-grams with maximum counts
        Map<List<String>, Integer> referenceNGrams = new HashMap<>();
        for (List<String> reference : references) {
            Map<List<String>, Integer> refNGrams = generateNGrams(reference, n);
            for (Map.Entry<List<String>, Integer> entry : refNGrams.entrySet()) {
                List<String> ngram = entry.getKey();
                int count = entry.getValue();
                referenceNGrams.put(ngram, Math.max(referenceNGrams.getOrDefault(ngram, 0), count));
            }
        }
        
        // Calculate clipped counts
        int clippedCount = 0;
        int totalCount = 0;
        
        for (Map.Entry<List<String>, Integer> entry : candidateNGrams.entrySet()) {
            List<String> ngram = entry.getKey();
            int candidateCount = entry.getValue();
            int referenceCount = referenceNGrams.getOrDefault(ngram, 0);
            
            clippedCount += Math.min(candidateCount, referenceCount);
            totalCount += candidateCount;
        }
        
        return totalCount > 0 ? (double) clippedCount / totalCount : 0.0;
    }
    
    /**
     * Generates n-grams from a list of tokens
     */
    private Map<List<String>, Integer> generateNGrams(List<String> tokens, int n) {
        Map<List<String>, Integer> nGrams = new HashMap<>();
        
        for (int i = 0; i <= tokens.size() - n; i++) {
            List<String> ngram = tokens.subList(i, i + n);
            nGrams.put(ngram, nGrams.getOrDefault(ngram, 0) + 1);
        }
        
        return nGrams;
    }
    
    /**
     * Calculates brevity penalty
     */
    private double calculateBrevityPenalty(List<String> candidate, List<List<String>> references) {
        int candidateLength = candidate.size();
        
        // Find the reference length closest to candidate length
        int closestRefLength = references.get(0).size();
        int minDiff = Math.abs(candidateLength - closestRefLength);
        
        for (List<String> reference : references) {
            int diff = Math.abs(candidateLength - reference.size());
            if (diff < minDiff) {
                minDiff = diff;
                closestRefLength = reference.size();
            }
        }
        
        if (candidateLength >= closestRefLength) {
            return 1.0;
        } else {
            return Math.exp(Math.min(0, 1.0 - (double) closestRefLength / candidateLength));
        }
    }
    
    /**
     * Simple tokenization - splits on whitespace and punctuation
     */
    private List<String> tokenize(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        // Simple tokenization: split on whitespace and common punctuation
        String[] tokens = text.toLowerCase()
            .replaceAll("[^a-zA-Z0-9\\s]", " ") // Replace punctuation with spaces
            .split("\\s+");
        
        List<String> result = new ArrayList<>();
        for (String token : tokens) {
            if (!token.trim().isEmpty()) {
                result.add(token.trim());
            }
        }
        
        return result;
    }
    
    /**
     * Calculates individual n-gram precisions for detailed analysis
     */
    public Map<String, Double> calculateDetailedScores(String candidate, String reference) {
        Map<String, Double> scores = new HashMap<>();
        
        List<String> candidateTokens = tokenize(candidate);
        List<List<String>> referenceTokensList = Arrays.asList(tokenize(reference));
        
        // Calculate individual n-gram precisions
        for (int n = 1; n <= 4; n++) {
            double precision = calculateNGramPrecision(candidateTokens, referenceTokensList, n);
            scores.put("BLEU-" + n, precision);
        }
        
        // Calculate brevity penalty
        double brevityPenalty = calculateBrevityPenalty(candidateTokens, referenceTokensList);
        scores.put("Brevity_Penalty", brevityPenalty);
        
        // Calculate overall BLEU-4
        scores.put("BLEU-4", calculateBLEU4(candidate, reference));
        
        return scores;
    }
}