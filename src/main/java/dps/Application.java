package dps;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import dps.projectparser.ParseProject;
import dps.summarygenerator.Summarise;

public class Application {

    public static void main(String[] args) {
        try {
            runApplication();
        } catch (IOException e) {
            System.err.println("Fatal error during application execution: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    private static void runApplication() throws IOException {
        ParseProject parseProject = new ParseProject();

        // Create output and reference Directory if non-existent
        createDirectories();

        // Gets list of directories (folders) from specified input folder
        File[] projects = new File("input").listFiles(File::isDirectory);
        if (projects == null) {
            throw new IOException("Input directory not found or is not a directory");
        }
        
        for (File project : projects) {
            processProject(project, parseProject);
        }
        
        // Close the CSV writer to finalize the summary file
        Summarise.closeCsvWriter();
        System.out.println("\nAll projects processed. CSV summary file has been generated.");
    }
    
    private static void createDirectories() throws IOException {
        String[] directories = {"output", "output/json-output", "output/summary-output", "reference"};
        
        for (String dirPath : directories) {
            File dir = new File(dirPath);
            if (!dir.exists() && !dir.mkdirs()) {
                throw new IOException("Failed to create directory: " + dirPath);
            }
        }
    }
    
    private static void processProject(File project, ParseProject parseProject) throws IOException {
        System.out.println("\n" + project.getName());
        HashMap<String, Object> parsedProject;

        try {
            // Each directory in input folder is parsed
            parsedProject = parseProject.parseProject(project);
        } catch (Exception e) {
            System.err.println("\tError during project " + project.getName() + ": " + e.getMessage());
            e.printStackTrace();
            return; // Continue with next project instead of throwing
        }

        ObjectWriter writer = new ObjectMapper()
                .writer(new DefaultPrettyPrinter().withObjectIndenter(new DefaultIndenter("\t", "\n")));

        if (parsedProject.isEmpty()) {
            System.out.println("\tEmpty");
            return;
        }
        
        writer.writeValue(new File("output/json-output/" + project.getName() + ".json"), parsedProject);
    }
}

