package dpsSwum.swum.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a complete SWUM parse structure for a method or class.
 * Contains the parsed components and can generate natural language summaries.
 */
public class SWUMStructure {
    
    private String methodName;
    private String className;
    private SWUMNode parseTree;
    private List<String> parameters;
    private String returnType;
    private List<String> actions;
    private List<String> objects;
    private String designPattern;
    
    public SWUMStructure() {
        this.parameters = new ArrayList<>();
        this.actions = new ArrayList<>();
        this.objects = new ArrayList<>();
    }
    
    public SWUMStructure(String methodName, String className) {
        this();
        this.methodName = methodName;
        this.className = className;
    }
    
    // Getters and setters
    public String getMethodName() { return methodName; }
    public void setMethodName(String methodName) { this.methodName = methodName; }
    
    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
    
    public SWUMNode getParseTree() { return parseTree; }
    public void setParseTree(SWUMNode parseTree) { this.parseTree = parseTree; }
    
    public List<String> getParameters() { return parameters; }
    public void setParameters(List<String> parameters) { this.parameters = parameters; }
    public void addParameter(String parameter) { this.parameters.add(parameter); }
    
    public String getReturnType() { return returnType; }
    public void setReturnType(String returnType) { this.returnType = returnType; }
    
    public List<String> getActions() { return actions; }
    public void setActions(List<String> actions) { this.actions = actions; }
    public void addAction(String action) { this.actions.add(action); }
    
    public List<String> getObjects() { return objects; }
    public void setObjects(List<String> objects) { this.objects = objects; }
    public void addObject(String object) { this.objects.add(object); }
    
    public String getDesignPattern() { return designPattern; }
    public void setDesignPattern(String designPattern) { this.designPattern = designPattern; }
    
    /**
     * Generates a natural language summary using SWUM grammar rules
     */
    public String generateSummary() {
        if (parseTree == null) {
            return generateBasicSummary();
        }
        
        StringBuilder summary = new StringBuilder();
        
        // Generate summary based on parse tree structure
        List<String> yield = parseTree.getYield();
        if (!yield.isEmpty()) {
            summary.append(String.join(" ", yield));
        }
        
        // Add context information
        if (!parameters.isEmpty()) {
            summary.append(" with parameters: ").append(String.join(", ", parameters));
        }
        
        if (returnType != null && !returnType.equals("void")) {
            summary.append(" returning ").append(returnType);
        }
        
        if (designPattern != null) {
            summary.append(" as part of ").append(designPattern).append(" pattern");
        }
        
        return summary.toString().trim();
    }
    
    /**
     * Generates a basic summary when no parse tree is available
     */
    private String generateBasicSummary() {
        StringBuilder summary = new StringBuilder();
        
        if (className != null) {
            summary.append("Class ").append(className);
        }
        
        if (methodName != null) {
            if (summary.length() > 0) summary.append(" ");
            summary.append("method ").append(methodName);
        }
        
        if (!actions.isEmpty()) {
            summary.append(" performs: ").append(String.join(", ", actions));
        }
        
        if (!objects.isEmpty()) {
            summary.append(" on objects: ").append(String.join(", ", objects));
        }
        
        return summary.toString().trim();
    }
    
    /**
     * Returns a structured representation for JSON serialization
     */
    public String toStructuredString() {
        StringBuilder sb = new StringBuilder();
        sb.append("SWUM Structure:\n");
        sb.append("  Method: ").append(methodName != null ? methodName : "N/A").append("\n");
        sb.append("  Class: ").append(className != null ? className : "N/A").append("\n");
        sb.append("  Actions: ").append(actions).append("\n");
        sb.append("  Objects: ").append(objects).append("\n");
        sb.append("  Parameters: ").append(parameters).append("\n");
        sb.append("  Return Type: ").append(returnType != null ? returnType : "void").append("\n");
        sb.append("  Design Pattern: ").append(designPattern != null ? designPattern : "None").append("\n");
        if (parseTree != null) {
            sb.append("  Parse Tree: ").append(parseTree.toTreeString()).append("\n");
        }
        sb.append("  Summary: ").append(generateSummary());
        return sb.toString();
    }
}
