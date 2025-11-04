package summarise;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import org.apache.commons.collections4.MultiValuedMap;

import simplenlg.framework.NLGFactory;
import simplenlg.lexicon.Lexicon;
import simplenlg.realiser.english.Realiser;
import utils.Utils;

public class Summarise {
    
    // Static CSV writer to accumulate all summaries
    private static FileWriter csvWriter = null;
    private static boolean csvHeaderWritten = false;
    
    public String summarise(HashMap<String, HashMap> fileDetails,
            ArrayList<HashMap> designPatternDetails,
            HashMap<String, MultiValuedMap<String, String>> summary, String projectName) {

        ClassInterfaceSummariser classInterfaceSummariser = new ClassInterfaceSummariser();
        DesignPatternSummarise designPatternSummarise = new DesignPatternSummarise();
        MethodSummariser methodSummariser = new MethodSummariser();

        Lexicon lexicon = Lexicon.getDefaultLexicon();
        NLGFactory nlgFactory = new NLGFactory(lexicon);
        Realiser realiser = new Realiser(lexicon);

        String projectSummary = "";

        // Initialize CSV file if not already done
        initializeCsvWriter();

        // if the project has a design pattern, include the multivalue map to store values
        if (!designPatternDetails.isEmpty()) {
            designPatternSummarise.summarise(fileDetails, designPatternDetails, summary);
        }
        
        // Process each file individually and write separate CSV rows
        for (Map.Entry<String, HashMap> fileEntry : fileDetails.entrySet()) {
            String file = fileEntry.getKey();
            String fileSummary = "";
            
            // Check if this file has any design pattern summaries
            boolean hasDesignPatterns = false;
            for (String designPattern : summary.keySet()) {
                if (summary.get(designPattern).containsKey(file) && 
                    !summary.get(designPattern).get(file).isEmpty()) {
                    hasDesignPatterns = true;
                    break;
                }
            }
            
            if (hasDesignPatterns) {
                // Generate summary for files with design patterns
                for (String designPattern : summary.keySet()) {
                    if (summary.get(designPattern).containsKey(file) && 
                        !summary.get(designPattern).get(file).isEmpty()) {
                        
                        HashSet<String> fileSummarySet = new HashSet<>();
                        for (String summary_text : summary.get(designPattern).get(file))
                            fileSummarySet.add(summary_text);

                        // generate class detail description, put summary as a parameter so that
                        // design pattern details shall be included.
                        ArrayList classDetails = Utils.getClassOrInterfaceDetails(fileEntry.getValue());
                        if (classDetails.size() == 0) {
                            continue;
                        }
                        HashMap classDetail = (HashMap) Utils.getClassOrInterfaceDetails(fileEntry.getValue()).get(0);
                        String classDescription = classInterfaceSummariser.generateClassDescription(nlgFactory,
                                realiser, classDetail, fileSummarySet);
                        // generate method description, as well as method usage description, merge into
                        // method summary
                        ArrayList<HashMap> methodDetails = Utils.getMethodDetails(fileEntry.getValue());
                        if (methodDetails.size() != 0) {
                            String methodDescription = methodSummariser.generateMethodsSummary(nlgFactory, realiser,
                                    methodDetails, file);
                            String methodUsageDescription = methodSummariser.generateMethodDescription(nlgFactory, realiser,
                                    methodDetails);
                            String methodSummary = methodDescription + " " + methodUsageDescription;
                            classDescription += " " + methodSummary;
                        }
                        
                        if (!fileSummary.isEmpty()) {
                            fileSummary += " ";
                        }
                        fileSummary += designPattern + ": " + classDescription;
                    }
                }
            } else {
                // Generate summary for files without design patterns
                ArrayList classDetails = Utils.getClassOrInterfaceDetails(fileEntry.getValue());
                if (classDetails.size() > 0) {
                    HashMap classDetail = (HashMap) classDetails.get(0);
                    String classDescription = classInterfaceSummariser.generateClassDescription(nlgFactory,
                            realiser, classDetail, new HashSet<>());
                    
                    ArrayList<HashMap> methodDetails = Utils.getMethodDetails(fileEntry.getValue());
                    if (methodDetails.size() != 0) {
                        String methodDescription = methodSummariser.generateMethodsSummary(nlgFactory, realiser,
                                methodDetails, file);
                        String methodUsageDescription = methodSummariser.generateMethodDescription(nlgFactory, realiser,
                                methodDetails);
                        String methodSummary = methodDescription + " " + methodUsageDescription;
                        classDescription += " " + methodSummary;
                    }
                    
                    fileSummary = classDescription;
                }
            }
            
            // Write individual file summary to CSV
            if (!fileSummary.isEmpty()) {
                // Convert class name to Java filename (e.g., "VideoConversionFacade" -> "VideoConversionFacade.java")
                String javaFilename = file + ".java";
                writeToCsv(projectName, javaFilename, fileSummary);
                projectSummary += javaFilename + ": " + fileSummary + "\n";
            }
        }

        return projectSummary;
    }

    /**
     * Initialize the CSV writer for summary output
     */
    private static void initializeCsvWriter() {
        if (csvWriter == null) {
            try {
                // Ensure summary-output directory exists
                File summaryOutputDir = new File("output/summary-output");
                if (!summaryOutputDir.exists()) {
                    summaryOutputDir.mkdirs();
                }
                
                File csvFile = new File("output/summary-output/project_summary_improved.csv");
                csvWriter = new FileWriter(csvFile, false); // false = overwrite existing file
                
                // Write CSV header
                if (!csvHeaderWritten) {
                    csvWriter.write("Project Name,Filename,Summary\n");
                    csvHeaderWritten = true;
                }
                
                System.out.println("Initialized CSV summary file: " + csvFile.getAbsolutePath());
            } catch (IOException e) {
                System.err.println("Failed to initialize CSV writer: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Write a single project summary to the CSV file
     */
    private static void writeToCsv(String projectName, String fileName, String summary) {
        try {
            if (csvWriter == null) {
                initializeCsvWriter();
            }

            // Clean up the summary for CSV format
            String cleanSummary = summary.replace("\"", "\"\""); // Escape quotes
            cleanSummary = cleanSummary.replace("\r\n", " ").replace("\n", " ").replace("\r", " "); // Remove newlines
            cleanSummary = cleanSummary.trim();

            // Limit summary length to prevent CSV issues
            if (cleanSummary.length() > 1000) {
                cleanSummary = cleanSummary.substring(0, 1000) + "...";
            }

            // Write CSV row
            csvWriter.write(String.format("\"%s\",\"%s\",\"%s\"\n", 
                projectName, fileName, cleanSummary));
            csvWriter.flush(); // Ensure data is written immediately
            
        } catch (IOException e) {
            System.err.println("Failed to write to CSV: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Close the CSV writer - call this when all processing is complete
     */
    public static void closeCsvWriter() {
        try {
            if (csvWriter != null) {
                csvWriter.close();
                csvWriter = null;
                csvHeaderWritten = false;
                System.out.println("CSV summary file closed successfully.");
            }
        } catch (IOException e) {
            System.err.println("Failed to close CSV writer: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
