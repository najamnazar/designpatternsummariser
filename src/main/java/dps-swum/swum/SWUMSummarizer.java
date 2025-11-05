package dpsSwum.swum;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dpsSwum.swum.grammar.SWUMGrammarParser;
import dpsSwum.swum.model.SWUMStructure;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * Main SWUM Summarizer that processes JSON output and generates
 * summaries using Software Word Usage Model (SWUM) grammar.
 */
public class SWUMSummarizer {
    
    private SWUMGrammarParser parser;
    private ObjectMapper objectMapper;
    
    public SWUMSummarizer() {
        this.parser = new SWUMGrammarParser();
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Processes a single JSON file and generates SWUM summaries
     */
    public void processJsonFile(String inputPath, String outputPath) throws IOException {
        File inputFile = new File(inputPath);
        JsonNode rootNode = objectMapper.readTree(inputFile);
        
        // Create output structure
        ObjectNode swumOutput = objectMapper.createObjectNode();
        
        String projectName = extractProjectName(rootNode);
        swumOutput.put("project_name", projectName);
        swumOutput.put("swum_version", "1.0");
        swumOutput.put("generated_timestamp", System.currentTimeMillis());
        
        // Process each class/interface in the JSON
        ObjectNode classeSummaries = objectMapper.createObjectNode();
        
        rootNode.fields().forEachRemaining(entry -> {
            String key = entry.getKey();
            JsonNode value = entry.getValue();
            
            // Skip system fields
            if (key.equals("final_summary") || key.equals("design_pattern")) {
                return;
            }
            
            if (value.isObject()) {
                try {
                    ObjectNode classSummary = processClass(key, value);
                    classeSummaries.set(key, classSummary);
                } catch (Exception e) {
                    System.err.println("Error processing class " + key + ": " + e.getMessage());
                }
            }
        });
        
        swumOutput.set("class_summaries", classeSummaries);
        
        // Generate overall project summary using SWUM
        String projectSummary = generateProjectSummary(classeSummaries, rootNode);
        swumOutput.put("swum_project_summary", projectSummary);
        
        // Extract design patterns for context
        JsonNode designPatterns = rootNode.get("design_pattern");
        if (designPatterns != null) {
            swumOutput.set("design_patterns", designPatterns);
        }
        
        // Write output
        try (FileWriter writer = new FileWriter(outputPath)) {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(writer, swumOutput);
        }
        
        System.out.println("SWUM summary generated: " + outputPath);
    }
    
    /**
     * Processes a single class and generates SWUM summaries for its methods
     */
    private ObjectNode processClass(String className, JsonNode classData) {
        ObjectNode classSummary = objectMapper.createObjectNode();
        
        // Extract class information
        JsonNode classDetails = classData.get("CLASSORINTERFACEDETAIL");
        String classType = "class";
        if (classDetails != null && classDetails.isArray() && classDetails.size() > 0) {
            JsonNode firstDetail = classDetails.get(0);
            if (firstDetail.has("ISINTERFACEORNOT") && firstDetail.get("ISINTERFACEORNOT").asBoolean()) {
                classType = "interface";
            }
        }
        
        classSummary.put("class_name", className);
        classSummary.put("class_type", classType);
        
        // Process methods
        JsonNode methodDetails = classData.get("METHODDETAIL");
        ObjectNode methodSummaries = objectMapper.createObjectNode();
        List<String> methodNames = new ArrayList<>();
        
        if (methodDetails != null && methodDetails.isArray()) {
            for (JsonNode method : methodDetails) {
                String methodName = method.get("METHODNAME").asText();
                methodNames.add(methodName);
                
                ObjectNode methodSummary = processMethod(methodName, className, method);
                methodSummaries.set(methodName, methodSummary);
            }
        }
        
        classSummary.set("method_summaries", methodSummaries);
        
        // Generate class-level SWUM summary
        String designPattern = extractDesignPattern(className);
        String classSummaryText = parser.generateClassSummary(className, methodNames, designPattern);
        classSummary.put("swum_class_summary", classSummaryText);
        
        return classSummary;
    }
    
    /**
     * Processes a single method and generates SWUM summary
     */
    private ObjectNode processMethod(String methodName, String className, JsonNode methodData) {
        ObjectNode methodSummary = objectMapper.createObjectNode();
        
        // Extract method information
        String returnType = methodData.has("METHODRETURNTYPE") ? 
            methodData.get("METHODRETURNTYPE").asText() : "void";
        
        List<String> parameters = new ArrayList<>();
        JsonNode paramNodes = methodData.get("METHODPARAMETER");
        if (paramNodes != null && paramNodes.isArray()) {
            for (JsonNode param : paramNodes) {
                if (param.has("PARAMETERDATATYPE")) {
                    parameters.add(param.get("PARAMETERDATATYPE").asText());
                }
            }
        }
        
        // Generate SWUM summary for method
        String designPattern = extractDesignPattern(className);
        String swumSummary = parser.generateMethodSummary(methodName, className, parameters, returnType, designPattern);
        
        methodSummary.put("method_name", methodName);
        methodSummary.put("return_type", returnType);
        methodSummary.put("parameters", String.join(", ", parameters));
        methodSummary.put("swum_summary", swumSummary);
        
        // Add structural information
        SWUMStructure structure = parser.parseMethodName(methodName, className);
        methodSummary.put("swum_actions", String.join(", ", structure.getActions()));
        methodSummary.put("swum_objects", String.join(", ", structure.getObjects()));
        
        if (structure.getParseTree() != null) {
            methodSummary.put("swum_parse_tree", structure.getParseTree().toTreeString());
        }
        
        return methodSummary;
    }
    
    /**
     * Generates overall project summary using SWUM analysis
     */
    private String generateProjectSummary(ObjectNode classSummaries, JsonNode rootData) {
        StringBuilder summary = new StringBuilder();
        
        // Analyze design patterns
        Set<String> detectedPatterns = new HashSet<>();
        JsonNode designPatterns = rootData.get("design_pattern");
        if (designPatterns != null && designPatterns.isArray()) {
            for (JsonNode pattern : designPatterns) {
                pattern.fieldNames().forEachRemaining(detectedPatterns::add);
            }
        }
        
        // Count classes and methods
        int classCount = 0;
        int methodCount = 0;
        Set<String> allActions = new HashSet<>();
        Set<String> allObjects = new HashSet<>();
        
        classSummaries.fields().forEachRemaining(classEntry -> {
            JsonNode classData = classEntry.getValue();
            JsonNode methodSummaries = classData.get("method_summaries");
            
            if (methodSummaries != null) {
                methodSummaries.fields().forEachRemaining(methodEntry -> {
                    JsonNode methodData = methodEntry.getValue();
                    
                    // Extract actions and objects
                    if (methodData.has("swum_actions")) {
                        String actions = methodData.get("swum_actions").asText();
                        if (!actions.isEmpty()) {
                            allActions.addAll(Arrays.asList(actions.split(", ")));
                        }
                    }
                    
                    if (methodData.has("swum_objects")) {
                        String objects = methodData.get("swum_objects").asText();
                        if (!objects.isEmpty()) {
                            allObjects.addAll(Arrays.asList(objects.split(", ")));
                        }
                    }
                });
            }
        });
        
        // Count methods separately to avoid lambda variable issue
        final int[] methodCountArray = {0};
        classSummaries.fields().forEachRemaining(entry -> {
            JsonNode classData = entry.getValue();
            JsonNode methods = classData.get("method_summaries");
            if (methods != null) {
                methodCountArray[0] += methods.size();
            }
        });
        methodCount = methodCountArray[0];
        classCount = classSummaries.size();
        
        // Build summary
        summary.append("This software project contains ").append(classCount).append(" classes");
        if (methodCount > 0) {
            summary.append(" with ").append(methodCount).append(" methods");
        }
        summary.append(". ");
        
        if (!detectedPatterns.isEmpty()) {
            summary.append("The system implements ");
            if (detectedPatterns.size() == 1) {
                summary.append("the ").append(detectedPatterns.iterator().next()).append(" design pattern");
            } else {
                summary.append("multiple design patterns including ");
                summary.append(String.join(", ", detectedPatterns));
            }
            summary.append(". ");
        }
        
        if (!allActions.isEmpty()) {
            List<String> topActions = allActions.stream()
                .filter(action -> !action.isEmpty())
                .sorted()
                .limit(5)
                .collect(java.util.stream.Collectors.toList());
            if (!topActions.isEmpty()) {
                summary.append("Primary operations include ");
                summary.append(String.join(", ", topActions));
                summary.append(". ");
            }
        }
        
        if (!allObjects.isEmpty()) {
            List<String> topObjects = allObjects.stream()
                .filter(object -> !object.isEmpty())
                .sorted()
                .limit(5)
                .collect(java.util.stream.Collectors.toList());
            if (!topObjects.isEmpty()) {
                summary.append("Key objects manipulated are ");
                summary.append(String.join(", ", topObjects));
                summary.append(".");
            }
        }
        
        return summary.toString().trim();
    }
    
    /**
     * Extracts project name from JSON data
     */
    private String extractProjectName(JsonNode rootNode) {
        Iterator<String> fieldNames = rootNode.fieldNames();
        while (fieldNames.hasNext()) {
            String fieldName = fieldNames.next();
            if (!fieldName.equals("final_summary") && !fieldName.equals("design_pattern")) {
                return fieldName;
            }
        }
        return "Unknown Project";
    }
    
    /**
     * Extracts design pattern information for a class
     */
    private String extractDesignPattern(String className) {
        String lowerClassName = className.toLowerCase();
        
        if (lowerClassName.contains("factory")) return "Factory";
        if (lowerClassName.contains("builder")) return "Builder";
        if (lowerClassName.contains("singleton")) return "Singleton";
        if (lowerClassName.contains("observer")) return "Observer";
        if (lowerClassName.contains("adapter")) return "Adapter";
        if (lowerClassName.contains("decorator")) return "Decorator";
        if (lowerClassName.contains("facade")) return "Facade";
        if (lowerClassName.contains("visitor")) return "Visitor";
        if (lowerClassName.contains("memento")) return "Memento";
        if (lowerClassName.contains("strategy")) return "Strategy";
        
        return null;
    }
    
    /**
     * Processes all JSON files in the output directory
     */
    public void processAllFiles(String inputDir, String outputDir) {
        File inputDirectory = new File(inputDir);
        File outputDirectory = new File(outputDir);
        
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs();
        }
        
        File[] jsonFiles = inputDirectory.listFiles((dir, name) -> name.endsWith(".json"));
        
        if (jsonFiles == null) {
            System.err.println("No JSON files found in " + inputDir);
            return;
        }
        
        int processed = 0;
        for (File jsonFile : jsonFiles) {
            try {
                String outputFileName = jsonFile.getName().replace(".json", "_swum.json");
                String outputPath = new File(outputDirectory, outputFileName).getAbsolutePath();
                
                processJsonFile(jsonFile.getAbsolutePath(), outputPath);
                processed++;
            } catch (Exception e) {
                System.err.println("Error processing " + jsonFile.getName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        System.out.println("Processed " + processed + " JSON files with SWUM summarizer");
    }
    
    /**
     * Processes a complete project JSON file and adds SWUM summaries
     */
    public JsonNode processProjectJson(JsonNode originalJson) {
        ObjectNode result = objectMapper.createObjectNode();
        
        // Copy original data
        result.setAll((ObjectNode) originalJson);
        
        // Add SWUM processing timestamp
        result.put("swum_processing_timestamp", System.currentTimeMillis());
        result.put("swum_version", "1.0");
        
        // Process project-level summary
        String projectName = originalJson.has("project_name") ? 
            originalJson.get("project_name").asText() : "UnknownProject";
        
        String projectSummary = generateProjectSummary(originalJson);
        result.put("swum_project_summary", projectSummary);
        
        // Process individual files if available
        if (originalJson.has("files")) {
            ObjectNode swumFiles = objectMapper.createObjectNode();
            JsonNode files = originalJson.get("files");
            
            files.fields().forEachRemaining(entry -> {
                String fileName = entry.getKey();
                JsonNode fileData = entry.getValue();
                
                ObjectNode swumFileData = objectMapper.createObjectNode();
                swumFileData.setAll((ObjectNode) fileData);
                
                // Generate SWUM summary for the file
                String swumSummary = generateFileSummary(fileName, fileData);
                swumFileData.put("swum_summary", swumSummary);
                
                // Process methods if available
                if (fileData.has("methods")) {
                    ObjectNode swumMethods = objectMapper.createObjectNode();
                    JsonNode methods = fileData.get("methods");
                    
                    methods.fields().forEachRemaining(methodEntry -> {
                        String methodName = methodEntry.getKey();
                        JsonNode methodData = methodEntry.getValue();
                        
                        ObjectNode swumMethodData = objectMapper.createObjectNode();
                        swumMethodData.setAll((ObjectNode) methodData);
                        
                        String methodSummary = generateMethodSummary(methodName);
                        swumMethodData.put("swum_summary", methodSummary);
                        
                        swumMethods.set(methodName, swumMethodData);
                    });
                    
                    swumFileData.set("swum_methods", swumMethods);
                }
                
                swumFiles.set(fileName, swumFileData);
            });
            
            result.set("swum_files", swumFiles);
        }
        
        // Add SWUM statistics
        ObjectNode swumStats = objectMapper.createObjectNode();
        swumStats.put("total_files_processed", result.has("files") ? result.get("files").size() : 0);
        swumStats.put("grammar_rules_applied", parser.getAppliedRulesCount());
        swumStats.put("vocabulary_coverage", calculateVocabularyCoverage(originalJson));
        
        result.set("swum_statistics", swumStats);
        
        return result;
    }
    
    /**
     * Generates a SWUM summary for an entire project
     */
    private String generateProjectSummary(JsonNode projectJson) {
        String projectName = projectJson.has("project_name") ? 
            projectJson.get("project_name").asText() : "Project";
        
        // Extract patterns if available
        StringBuilder summary = new StringBuilder();
        
        if (projectJson.has("detected_patterns")) {
            JsonNode patterns = projectJson.get("detected_patterns");
            if (patterns.isArray() && patterns.size() > 0) {
                summary.append("This ").append(cleanIdentifier(projectName)).append(" implements ");
                
                List<String> patternNames = new ArrayList<>();
                for (JsonNode pattern : patterns) {
                    if (pattern.has("pattern")) {
                        patternNames.add(pattern.get("pattern").asText());
                    }
                }
                
                if (!patternNames.isEmpty()) {
                    summary.append(String.join(", ", patternNames)).append(" design pattern");
                    if (patternNames.size() > 1) summary.append("s");
                }
            }
        }
        
        // Add file count information
        if (projectJson.has("files")) {
            int fileCount = projectJson.get("files").size();
            if (summary.length() > 0) {
                summary.append(" across ");
            } else {
                summary.append("This project contains ");
            }
            summary.append(fileCount).append(" source file");
            if (fileCount > 1) summary.append("s");
        }
        
        // Add final summary if available
        if (projectJson.has("final_summary")) {
            String finalSummary = projectJson.get("final_summary").asText();
            if (!finalSummary.trim().isEmpty()) {
                if (summary.length() > 0) {
                    summary.append(". ");
                }
                // Use SWUM to process the final summary
                summary.append(generateSummaryFromText(finalSummary));
            }
        }
        
        if (summary.length() == 0) {
            summary.append("This ").append(cleanIdentifier(projectName)).append(" represents a software implementation");
        }
        
        return summary.toString();
    }
    
    /**
     * Generates a SWUM summary for a single file
     */
    private String generateFileSummary(String fileName, JsonNode fileData) {
        String cleanFileName = cleanIdentifier(fileName.replaceAll("\\.(java|py|cpp|cs)$", ""));
        
        StringBuilder summary = new StringBuilder();
        summary.append("The ").append(cleanFileName).append(" component");
        
        // Check for class information
        if (fileData.has("class_name")) {
            String className = fileData.get("class_name").asText();
            String classSummary = generateClassSummary(className);
            summary.append(" ").append(classSummary.toLowerCase());
        }
        
        // Add method count
        if (fileData.has("methods")) {
            int methodCount = fileData.get("methods").size();
            summary.append(" and contains ").append(methodCount).append(" method");
            if (methodCount > 1) summary.append("s");
        }
        
        // Add pattern information if available
        if (fileData.has("pattern")) {
            String pattern = fileData.get("pattern").asText();
            summary.append(" implementing ").append(pattern).append(" pattern");
        }
        
        return summary.toString();
    }
    
    /**
     * Generates SWUM summary from existing text
     */
    private String generateSummaryFromText(String text) {
        // Extract key action words and objects from the text
        String[] words = text.toLowerCase().split("\\s+");
        List<String> actionWords = new ArrayList<>();
        List<String> objectWords = new ArrayList<>();
        
        for (String word : words) {
            String cleanWord = word.replaceAll("[^a-zA-Z]", "");
            if (parser.getActionVerbs().contains(cleanWord)) {
                actionWords.add(cleanWord);
            } else if (parser.getObjectNouns().contains(cleanWord)) {
                objectWords.add(cleanWord);
            }
        }
        
        // Construct SWUM-style summary
        StringBuilder swumSummary = new StringBuilder();
        if (!actionWords.isEmpty() && !objectWords.isEmpty()) {
            swumSummary.append("This system ");
            swumSummary.append(actionWords.get(0));
            swumSummary.append("s ");
            swumSummary.append(objectWords.get(0));
            if (objectWords.size() > 1) {
                swumSummary.append(" and ").append(objectWords.get(1));
            }
        } else {
            // Fallback to simplified version
            swumSummary.append(text);
        }
        
        return swumSummary.toString();
    }
    
    /**
     * Calculates vocabulary coverage for SWUM processing
     */
    private double calculateVocabularyCoverage(JsonNode projectJson) {
        Set<String> allWords = new HashSet<>();
        Set<String> recognizedWords = new HashSet<>();
        
        // Extract words from project
        extractWordsFromJson(projectJson, allWords);
        
        // Check recognition
        for (String word : allWords) {
            String cleanWord = word.toLowerCase().replaceAll("[^a-zA-Z]", "");
            if (parser.getActionVerbs().contains(cleanWord) || 
                parser.getObjectNouns().contains(cleanWord) ||
                parser.getPatternTerms().contains(cleanWord)) {
                recognizedWords.add(cleanWord);
            }
        }
        
        return allWords.isEmpty() ? 0.0 : (double) recognizedWords.size() / allWords.size();
    }
    
    /**
     * Recursively extracts words from JSON structure
     */
    private void extractWordsFromJson(JsonNode node, Set<String> words) {
        if (node.isTextual()) {
            String text = node.asText();
            String[] textWords = text.split("\\W+");
            for (String word : textWords) {
                if (!word.trim().isEmpty()) {
                    words.add(word.trim());
                }
            }
        } else if (node.isObject()) {
            node.fields().forEachRemaining(entry -> {
                words.add(entry.getKey());
                extractWordsFromJson(entry.getValue(), words);
            });
        } else if (node.isArray()) {
            for (JsonNode arrayElement : node) {
                extractWordsFromJson(arrayElement, words);
            }
        }
    }
    
    /**
     * Cleans identifier names by converting camelCase to readable text
     */
    private String cleanIdentifier(String identifier) {
        if (identifier == null || identifier.trim().isEmpty()) {
            return identifier;
        }
        
        // Split camelCase and convert to lowercase with spaces
        String result = identifier.replaceAll("([a-z])([A-Z])", "$1 $2").toLowerCase();
        
        // Clean up extra spaces
        return result.replaceAll("\\s+", " ").trim();
    }
    
    /**
     * Generates a simple method summary from method name only
     */
    public String generateMethodSummary(String methodName) {
        if (methodName == null || methodName.trim().isEmpty()) {
            return "Unknown method";
        }
        
        // Use SWUM grammar parser to create summary
        SWUMStructure structure = parser.parseMethodName(methodName, "UnknownClass");
        
        StringBuilder summary = new StringBuilder();
        if (!structure.getActions().isEmpty()) {
            summary.append("This method ");
            summary.append(structure.getActions().get(0));
            if (!structure.getObjects().isEmpty()) {
                summary.append(" ");
                summary.append(structure.getObjects().get(0));
            }
        } else {
            summary.append("The ").append(cleanIdentifier(methodName)).append(" method");
        }
        
        return summary.toString();
    }
    
    /**
     * Generates a simple class summary from class name only
     */
    public String generateClassSummary(String className) {
        if (className == null || className.trim().isEmpty()) {
            return "Unknown class";
        }
        
        String cleanName = cleanIdentifier(className);
        
        // Check for common patterns in class name
        String lowerName = className.toLowerCase();
        if (lowerName.contains("factory")) {
            return "Creates and manages " + cleanName.replace("factory", "").trim() + " instances";
        } else if (lowerName.contains("builder")) {
            return "Constructs " + cleanName.replace("builder", "").trim() + " objects step by step";
        } else if (lowerName.contains("adapter")) {
            return "Adapts " + cleanName.replace("adapter", "").trim() + " interface";
        } else if (lowerName.contains("decorator")) {
            return "Decorates " + cleanName.replace("decorator", "").trim() + " with additional functionality";
        } else if (lowerName.contains("facade")) {
            return "Provides simplified interface to " + cleanName.replace("facade", "").trim() + " subsystem";
        } else if (lowerName.contains("observer")) {
            return "Observes changes in " + cleanName.replace("observer", "").trim() + " state";
        } else if (lowerName.contains("visitor")) {
            return "Visits " + cleanName.replace("visitor", "").trim() + " elements";
        } else if (lowerName.contains("singleton")) {
            return "Ensures single instance of " + cleanName.replace("singleton", "").trim();
        } else if (lowerName.contains("memento")) {
            return "Captures " + cleanName.replace("memento", "").trim() + " state";
        } else {
            return "The " + cleanName + " class";
        }
    }
}
