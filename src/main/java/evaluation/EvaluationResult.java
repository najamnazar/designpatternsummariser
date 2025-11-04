package evaluation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents the evaluation results for comparing SWUM and SimpleNLG summaries
 */
public class EvaluationResult {
    
    private String projectName;
    private String simpleNLGSummary;
    private String swumSummary;
    
    // BLEU scores
    private double bleuScore;
    private Map<String, Double> bleuDetails;
    
    // NIST scores
    private double nistScore;
    private Map<String, Double> nistDetails;
    
    // Reverse scores (using opposite as reference)
    private double reverseBLEU;
    private double reverseNIST;
    
    // Additional metrics
    private double summaryLengthRatio;
    private double wordOverlapRatio;
    
    // Error tracking
    private List<String> errors;
    
    public EvaluationResult() {
        this.bleuDetails = new HashMap<>();
        this.nistDetails = new HashMap<>();
        this.errors = new ArrayList<>();
    }
    
    // Getters and setters
    public String getProjectName() {
        return projectName;
    }
    
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }
    
    public String getSimpleNLGSummary() {
        return simpleNLGSummary;
    }
    
    public void setSimpleNLGSummary(String simpleNLGSummary) {
        this.simpleNLGSummary = simpleNLGSummary;
    }
    
    public String getSWUMSummary() {
        return swumSummary;
    }
    
    public void setSWUMSummary(String swumSummary) {
        this.swumSummary = swumSummary;
    }
    
    public double getBleuScore() {
        return bleuScore;
    }
    
    public void setBleuScore(double bleuScore) {
        this.bleuScore = bleuScore;
    }
    
    public Map<String, Double> getBleuDetails() {
        return bleuDetails;
    }
    
    public void setBleuDetails(Map<String, Double> bleuDetails) {
        this.bleuDetails = bleuDetails;
    }
    
    public double getNistScore() {
        return nistScore;
    }
    
    public void setNistScore(double nistScore) {
        this.nistScore = nistScore;
    }
    
    public Map<String, Double> getNistDetails() {
        return nistDetails;
    }
    
    public void setNistDetails(Map<String, Double> nistDetails) {
        this.nistDetails = nistDetails;
    }
    
    public double getReverseBLEU() {
        return reverseBLEU;
    }
    
    public void setReverseBLEU(double reverseBLEU) {
        this.reverseBLEU = reverseBLEU;
    }
    
    public double getReverseNIST() {
        return reverseNIST;
    }
    
    public void setReverseNIST(double reverseNIST) {
        this.reverseNIST = reverseNIST;
    }
    
    public double getSummaryLengthRatio() {
        return summaryLengthRatio;
    }
    
    public void setSummaryLengthRatio(double summaryLengthRatio) {
        this.summaryLengthRatio = summaryLengthRatio;
    }
    
    public double getWordOverlapRatio() {
        return wordOverlapRatio;
    }
    
    public void setWordOverlapRatio(double wordOverlapRatio) {
        this.wordOverlapRatio = wordOverlapRatio;
    }
    
    public List<String> getErrors() {
        return errors;
    }
    
    public void setErrors(List<String> errors) {
        this.errors = errors;
    }
    
    public void addError(String error) {
        this.errors.add(error);
    }
    
    public boolean hasErrors() {
        return !errors.isEmpty();
    }
    
    /**
     * Returns a formatted string representation of the evaluation results
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("EvaluationResult{");
        sb.append("project='").append(projectName).append('\'');
        sb.append(", BLEU-4=").append(String.format("%.4f", bleuScore));
        sb.append(", NIST=").append(String.format("%.4f", nistScore));
        sb.append(", lengthRatio=").append(String.format("%.2f", summaryLengthRatio));
        sb.append(", wordOverlap=").append(String.format("%.2f", wordOverlapRatio));
        if (hasErrors()) {
            sb.append(", errors=").append(errors);
        }
        sb.append('}');
        return sb.toString();
    }
    
    /**
     * Returns a brief summary of the key metrics
     */
    public String getShortSummary() {
        return String.format("%s: BLEU=%.3f, NIST=%.3f", 
            projectName, bleuScore, nistScore);
    }
}