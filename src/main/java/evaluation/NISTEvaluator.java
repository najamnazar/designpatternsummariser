package evaluation;

import java.util.*;

/**
 * Implementation of NIST (National Institute of Standards and Technology) score calculation
 * for evaluating the quality of machine-generated text summaries.
 * NIST is similar to BLEU but gives more weight to informative n-grams.
 */
public class NISTEvaluator {
    
    private static final int MAX_N = 5; // NIST typically uses up to 5-grams
    
    /**
     * Calculates NIST score between candidate and reference summaries
     * 
     * @param candidate The generated summary text
     * @param reference The reference (ground truth) summary text
     * @return NIST score (higher is better, no upper bound)
     */
    public double calculateNIST(String candidate, String reference) {
        return calculateNIST(candidate, Arrays.asList(reference));
    }
    
    /**
     * Calculates NIST score between candidate and multiple reference summaries
     * 
     * @param candidate The generated summary text
     * @param references List of reference summary texts
     * @return NIST score (higher is better, no upper bound)
     */
    public double calculateNIST(String candidate, List<String> references) {
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
        
        // Calculate information weights for all n-grams in references
        Map<List<String>, Double> informationWeights = calculateInformationWeights(referenceTokensList);
        
        // Calculate weighted precision for n-grams (1 to 5)
        double totalScore = 0.0;
        
        for (int n = 1; n <= MAX_N; n++) {
            double weightedPrecision = calculateWeightedNGramPrecision(
                candidateTokens, referenceTokensList, n, informationWeights);
            totalScore += weightedPrecision;
        }
        
        // Calculate brevity penalty (similar to BLEU but different formula)
        double brevityPenalty = calculateNISTBrevityPenalty(candidateTokens, referenceTokensList);
        
        return totalScore * brevityPenalty;
    }
    
    /**
     * Calculates information weights for n-grams based on their frequency
     * More rare n-grams get higher weights
     */
    private Map<List<String>, Double> calculateInformationWeights(List<List<String>> references) {
        Map<List<String>, Double> weights = new HashMap<>();
        
        // Count all n-grams in references
        Map<List<String>, Integer> ngramCounts = new HashMap<>();
        Map<Integer, Integer> totalNGramCounts = new HashMap<>(); // Total count per n-gram size
        
        for (List<String> reference : references) {
            for (int n = 1; n <= MAX_N; n++) {
                Map<List<String>, Integer> refNGrams = generateNGrams(reference, n);
                for (Map.Entry<List<String>, Integer> entry : refNGrams.entrySet()) {
                    List<String> ngram = entry.getKey();
                    int count = entry.getValue();
                    
                    ngramCounts.put(ngram, ngramCounts.getOrDefault(ngram, 0) + count);
                    totalNGramCounts.put(n, totalNGramCounts.getOrDefault(n, 0) + count);
                }
            }
        }
        
        // Calculate information weights: log(total_count / ngram_count)
        for (Map.Entry<List<String>, Integer> entry : ngramCounts.entrySet()) {
            List<String> ngram = entry.getKey();
            int ngramCount = entry.getValue();
            int n = ngram.size();
            int totalCount = totalNGramCounts.get(n);
            
            if (ngramCount > 0 && totalCount > 0) {
                double weight = Math.log((double) totalCount / ngramCount) / Math.log(2.0);
                weights.put(ngram, Math.max(weight, 0.0)); // Ensure non-negative
            } else {
                weights.put(ngram, 0.0);
            }
        }
        
        return weights;
    }
    
    /**
     * Calculates weighted n-gram precision for given n
     */
    private double calculateWeightedNGramPrecision(List<String> candidate, 
                                                 List<List<String>> references, 
                                                 int n,
                                                 Map<List<String>, Double> weights) {
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
        
        // Calculate weighted clipped counts
        double weightedClippedCount = 0.0;
        int totalCount = 0;
        
        for (Map.Entry<List<String>, Integer> entry : candidateNGrams.entrySet()) {
            List<String> ngram = entry.getKey();
            int candidateCount = entry.getValue();
            int referenceCount = referenceNGrams.getOrDefault(ngram, 0);
            
            int clippedCount = Math.min(candidateCount, referenceCount);
            double weight = weights.getOrDefault(ngram, 0.0);
            
            weightedClippedCount += clippedCount * weight;
            totalCount += candidateCount;
        }
        
        return totalCount > 0 ? weightedClippedCount / totalCount : 0.0;
    }
    
    /**
     * Generates n-grams from a list of tokens
     */
    private Map<List<String>, Integer> generateNGrams(List<String> tokens, int n) {
        Map<List<String>, Integer> nGrams = new HashMap<>();
        
        for (int i = 0; i <= tokens.size() - n; i++) {
            List<String> ngram = new ArrayList<>(tokens.subList(i, i + n));
            nGrams.put(ngram, nGrams.getOrDefault(ngram, 0) + 1);
        }
        
        return nGrams;
    }
    
    /**
     * Calculates NIST brevity penalty (different from BLEU)
     */
    private double calculateNISTBrevityPenalty(List<String> candidate, List<List<String>> references) {
        int candidateLength = candidate.size();
        
        // Calculate average reference length
        double avgRefLength = references.stream()
            .mapToInt(List::size)
            .average()
            .orElse(0.0);
        
        if (candidateLength >= avgRefLength) {
            return 1.0;
        } else {
            // NIST uses a different penalty formula
            double ratio = candidateLength / avgRefLength;
            return Math.exp(-1.0 * Math.pow(Math.log(ratio), 2.0) / (2.0 * Math.pow(0.5, 2.0)));
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
     * Calculates individual n-gram scores for detailed analysis
     */
    public Map<String, Double> calculateDetailedScores(String candidate, String reference) {
        Map<String, Double> scores = new HashMap<>();
        
        List<String> candidateTokens = tokenize(candidate);
        List<List<String>> referenceTokensList = Arrays.asList(tokenize(reference));
        
        // Calculate information weights
        Map<List<String>, Double> weights = calculateInformationWeights(referenceTokensList);
        
        // Calculate individual n-gram scores
        for (int n = 1; n <= MAX_N; n++) {
            double score = calculateWeightedNGramPrecision(candidateTokens, referenceTokensList, n, weights);
            scores.put("NIST-" + n, score);
        }
        
        // Calculate brevity penalty
        double brevityPenalty = calculateNISTBrevityPenalty(candidateTokens, referenceTokensList);
        scores.put("Brevity_Penalty", brevityPenalty);
        
        // Calculate overall NIST
        scores.put("NIST", calculateNIST(candidate, reference));
        
        return scores;
    }
    
    /**
     * Calculates corpus-level NIST score for multiple sentence pairs
     */
    public double calculateCorpusNIST(List<String> candidates, List<String> references) {
        if (candidates.size() != references.size()) {
            throw new IllegalArgumentException("Candidates and references must have the same size");
        }
        
        double totalScore = 0.0;
        int validPairs = 0;
        
        for (int i = 0; i < candidates.size(); i++) {
            double score = calculateNIST(candidates.get(i), references.get(i));
            if (!Double.isNaN(score) && !Double.isInfinite(score)) {
                totalScore += score;
                validPairs++;
            }
        }
        
        return validPairs > 0 ? totalScore / validPairs : 0.0;
    }
}