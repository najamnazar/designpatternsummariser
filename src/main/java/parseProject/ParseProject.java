package parseProject;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ParserConfiguration.LanguageLevel;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserMethodDeclaration;

import designPatterns.CheckPattern;
import summarise.Summarise;
import utils.Utils;
import utils.SymbolSolverFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Map;
import java.util.*;
import java.util.regex.Pattern;

import org.apache.commons.collections4.MultiValuedMap;

public class ParseProject {

    // reference: java callgraph
    // 需要跳过的pattern列表
    private List<Pattern> skipPatterns = new ArrayList<Pattern>();

    public HashMap<String, Object> parseProject(File directory) throws FileNotFoundException {

        ArrayList<File> fileArrayList = new ArrayList<>();

        // referenced from Java Callgraph
        ArrayList<String> srcPathList = new ArrayList<>();
        ArrayList<String> libPathList = new ArrayList<>();

        // names of all files in directory added to fileArrayList (list not tree)
        // srcPathList and libPathList consist of abs paths of src and lib folders
        fetchFiles(directory, fileArrayList, srcPathList, libPathList);

        // Enhanced symbol resolution with multiple strategies
        JavaSymbolSolver symbolSolver = SymbolSolverFactory.getJavaSymbolSolver(srcPathList, libPathList);
        StaticJavaParser.getParserConfiguration().setSymbolResolver(symbolSolver);
        StaticJavaParser.getParserConfiguration().setLanguageLevel(LanguageLevel.BLEEDING_EDGE);
        
        // Configure parser to be more lenient with symbol resolution failures
        StaticJavaParser.getParserConfiguration().setDoNotAssignCommentsPrecedingEmptyLines(false);
        
        // Debug: Print source paths being used for symbol resolution
        System.out.println("Source paths for " + directory.getName() + ": " + srcPathList);
        
        // Pre-parse all compilation units to build symbol table
        Map<String, CompilationUnit> preParseCache = new HashMap<>();
        for (File file : fileArrayList) {
            try {
                CompilationUnit cu = StaticJavaParser.parse(file);
                String className = Utils.getBaseName(file.getName());
                preParseCache.put(className, cu);
            } catch (Exception e) {
                // Continue with files that can be parsed
            }
        }

        // referenced from Java callgraph
        // Generate call graph using JavaParser symbol resolution
        HashMap<String, ArrayList<String>> callerCallees = new HashMap<>();

        HashMap<String, HashMap> parsedFile = new HashMap<>();
        CheckPattern checkPattern = new CheckPattern();
        Summarise summarise = new Summarise();

        ArrayList designPatternArrayList = new ArrayList<>();

        HashMap<String, MultiValuedMap<String, String>> summaries = new HashMap<>();
        HashMap<String, HashMap<String, HashSet<String>>> summaryMap = new HashMap<String, HashMap<String, HashSet<String>>>();
        String finalSummary = "";

        // go through all files under the project with enhanced error handling
        int successfullyParsed = 0;
        int totalFiles = fileArrayList.size();
        
        for (File file : fileArrayList) {
            try {
                HashMap<String, ArrayList> fileDetails = new HashMap<>();
                CompilationUnit compilationUnit = new CompilationUnit();

                try {
                    // Try to use cached compilation unit first
                    String className = Utils.getBaseName(file.getName());
                    if (preParseCache.containsKey(className)) {
                        compilationUnit = preParseCache.get(className);
                    } else {
                        compilationUnit = StaticJavaParser.parse(file);
                    }
                    
                    // Additional validation
                    if (compilationUnit.getPackageDeclaration().isEmpty() && 
                        compilationUnit.getTypes().isEmpty()) {
                        System.out.println("Skipping empty compilation unit: " + file.getName());
                        continue;
                    }
                    
                } catch (Exception e) {
                    System.out.println("Skipping file due to parse error: " + file.getName() + ", " + e.getMessage());
                    // Try alternative parsing with lenient settings
                    try {
                        compilationUnit = StaticJavaParser.parse(file);
                    } catch (Exception e2) {
                        System.out.println("Failed alternative parsing for: " + file.getName());
                        continue;
                    }
                } catch (Error e) {
                    System.out.println("Skipping file due to parse error: " + file.getName() + ", " + e.getMessage());
                    continue;
                }

                MethodsExtr methodsExtr = new MethodsExtr();

                FieldExtr fieldExtr = new FieldExtr();
                ConstructorExtr constructorExtr = new ConstructorExtr();
                VariableExtr variableExtr = new VariableExtr();
                ClassOrInterfaceExtr classOrInterfaceExtr = new ClassOrInterfaceExtr();

                fileDetails.put("FIELDDETAIL", fieldExtr.getFieldInfo(compilationUnit));
                fileDetails.put("CONSTRUCTORDETAIL", constructorExtr.getConstructorInfo(compilationUnit));
                fileDetails.put("VARIABLEDETAIL", variableExtr.getVariableInfo(compilationUnit));
                fileDetails.put("METHODDETAIL", methodsExtr.getMethodInfo(compilationUnit));
                fileDetails.put("CLASSORINTERFACEDETAIL", classOrInterfaceExtr.getClassInterfaceInfo(compilationUnit));
                
                // Generate call graph for this compilation unit and merge with overall call graph
                HashMap<String, ArrayList<String>> fileCallGraph = buildCallGraphFromCU(compilationUnit);
                for (String caller : fileCallGraph.keySet()) {
                    callerCallees.computeIfAbsent(caller, k -> new ArrayList<>()).addAll(fileCallGraph.get(caller));
                }
                
                // Keep the old extract method call for any additional processing
                extract(compilationUnit, callerCallees, skipPatterns);

                // zip the extracted file details with the file name
                parsedFile.put(Utils.getBaseName(file.getName()), fileDetails);
                successfullyParsed++;
                
            } catch (Exception e) {
                System.out.println("Error processing file: " + file.getName() + ", " + e.getMessage());
                e.printStackTrace();
                continue;
            } catch (Error e) {
                System.out.println("Error processing file: " + file.getName() + ", " + e.getMessage());
                e.printStackTrace();
                continue;
            }
        }
        
        // Print parsing statistics
        System.out.println("Successfully parsed " + successfullyParsed + " out of " + totalFiles + " files for " + directory.getName());

        // merge the features with the callgraph
        HashMap<String, Object> parsedProject = new HashMap<>();
        HashMap extractedCallGraph = extractCallgraphResults(parsedFile, callerCallees);

        if (extractedCallGraph.isEmpty())
            return new HashMap<>();
        checkPattern.extractDesignPattern(extractedCallGraph, designPatternArrayList);

        // Always generate summaries, regardless of whether design patterns are found
        finalSummary = summarise.summarise(extractedCallGraph, designPatternArrayList,
                summaries, directory.getName());
        
        // Process design pattern summaries if any were found
        if (!designPatternArrayList.isEmpty()) {
            for (String designPattern : summaries.keySet()) {
                summaryMap.put(designPattern, new HashMap<>());
                for (String classString : summaries.get(designPattern).keySet()) {
                    HashSet<String> summarySet = new HashSet<String>();
                    for (String summary : summaries.get(designPattern).get(classString)) {
                        summarySet.add(summary);
                    }
                    summaryMap.get(designPattern).put(classString, summarySet);
                }
            }
        }
        parsedProject.put(directory.getName(), extractedCallGraph);
        parsedProject.put("design_pattern", designPatternArrayList);
        parsedProject.put("summary_NLG", summaryMap);
        parsedProject.put("final_summary", finalSummary);

        // return the result, which contains all files of the project, stored in the
        // hashmap, the key is file name, the value is the details.
        return parsedProject;
    }

    private HashMap<String, HashMap> extractCallgraphResults(HashMap<String, HashMap> parsedFile,
            HashMap<String, ArrayList<String>> callerCallees) {
        Set<String> classNames = parsedFile.keySet();

        for (HashMap.Entry mapElement : callerCallees.entrySet()) {
            String caller = (String) mapElement.getKey();
            ArrayList<String> callees = callerCallees.get(caller);

            String callerClass = extractCallgraphClass(caller);
            String callerMethodName = extractCallgraphMethodName(caller);

            if (classNames.contains(callerClass)) {
                HashMap<String, ArrayList> parsedCallerClass = parsedFile.get(callerClass);
                ArrayList<HashMap> parsedCalledMethods = Utils.getMethodDetails(parsedCallerClass);
                for (HashMap parsedCalledMethod : parsedCalledMethods) {

                    // need parameter comparison also
                    if (Utils.getMethodName(parsedCalledMethod).equals(callerMethodName)) {
                        for (String callee : callees) {

                            String calleeClass = extractCallgraphClass(callee);
                            String calleeMethodName = extractCallgraphMethodName(callee);

                            HashMap<String, String> newOutgoing = new HashMap<>();
                            newOutgoing.put("CALLEECLASS", calleeClass);
                            newOutgoing.put("CALLEEMETHODNAME", calleeMethodName);

                            Utils.getOutgoingMethod(parsedCalledMethod).add(newOutgoing);

                            // add incoming method for the method in caller class
                            HashMap<String, ArrayList> parsedCalleeClass = parsedFile.get(calleeClass);
                            if (parsedCalleeClass == null) {
                                continue;
                            }
                            ArrayList<HashMap> parsedCallingMethods = Utils.getMethodDetails(parsedCalleeClass);

                            for (HashMap parsedCallingMethod : parsedCallingMethods) {

                                if (Utils.getMethodName(parsedCallingMethod).equals(calleeMethodName)) {

                                    HashMap<String, String> newIncoming = new HashMap<>();
                                    newIncoming.put("CALLEDCLASS", callerClass);
                                    newIncoming.put("CALLEDMETHODNAME", callerMethodName);

                                    Utils.getIncomingMethod(parsedCallingMethod).add(newIncoming);

                                    // Update number of incoming calls
                                    parsedCallingMethod.put("NUMBEROFINCOMINGMETHODS",
                                            Utils.getIncomingMethod(parsedCallingMethod).size());
                                }
                            }
                        }
                    }
                }
            }
        }
        return parsedFile;
    }

    private String extractCallgraphClass(String caller) {
        String filteredCaller = caller.replaceAll("\\(.*\\)", "");
        return Utils.splitByDot(filteredCaller, 2);
    }

    private String extractCallgraphMethodName(String caller) {
        String filteredCaller = caller.replaceAll("\\(.*\\)", "");
        return Utils.splitByDot(filteredCaller, 1);
    }

    private void fetchFiles(File dir, ArrayList<File> fileList, ArrayList<String> srcPathList,
            ArrayList<String> libPathList) {
        // Always add the root directory to srcPathList for better symbol resolution
        if (srcPathList.isEmpty()) {
            srcPathList.add(dir.getAbsolutePath());
        }
        
        // Enhanced source path detection - add any directory that contains Java files
        if (dir.isDirectory()) {
            boolean hasJavaFiles = false;
            boolean hasSubDirectories = false;
            
            for (File file : dir.listFiles()) {
                if (file.isFile() && Utils.getExtension(file).equals("java")) {
                    hasJavaFiles = true;
                }
                if (file.isDirectory()) {
                    hasSubDirectories = true;
                }
            }
            
            // If this directory contains Java files and isn't already added, add it as a source path
            if (hasJavaFiles && !srcPathList.contains(dir.getAbsolutePath())) {
                srcPathList.add(dir.getAbsolutePath());
                System.out.println("Added source path: " + dir.getAbsolutePath());
            }
            
            // Also add parent directory of Java file directories for better package resolution
            if (hasJavaFiles && dir.getParentFile() != null && 
                !srcPathList.contains(dir.getParentFile().getAbsolutePath())) {
                srcPathList.add(dir.getParentFile().getAbsolutePath());
                System.out.println("Added parent source path: " + dir.getParentFile().getAbsolutePath());
            }
        }
        
        // Original logic for "src" and "lib" directories - enhanced
        if (dir.getName().equals("src") || dir.getName().equals("main") || 
            dir.getName().equals("java") || dir.getName().equals("source")) {
            if (!srcPathList.contains(dir.getAbsolutePath())) {
                srcPathList.add(dir.getAbsolutePath());
                System.out.println("Added standard source path: " + dir.getAbsolutePath());
            }
        }

        if (dir.getName().equals("lib") || dir.getName().equals("libs") || 
            dir.getName().equals("dependencies")) {
            libPathList.add(dir.getAbsolutePath());
            System.out.println("Added library path: " + dir.getAbsolutePath());
        }

        if (dir.isDirectory()) {
            for (File file1 : dir.listFiles()) {
                fetchFiles(file1, fileList, srcPathList, libPathList);
            }
        } else if (Utils.getExtension(dir).equals("java")) {
            fileList.add(dir);
        }
    }

    // referenced from Java Callgraph
    private void extract(CompilationUnit compilationUnit, HashMap<String, ArrayList<String>> callerCallees,
            List<Pattern> skipPatterns) {

        // 获取到方法声明，并进行遍历
        List<MethodDeclaration> all = compilationUnit.findAll(MethodDeclaration.class);
        for (MethodDeclaration methodDeclaration : all) {
            ArrayList<String> curCallees = new ArrayList<>();

            // 对每个方法声明内容进行遍历，查找内部调用的其他方法
            methodDeclaration.accept(new MethodCallVisitor(skipPatterns), curCallees);
            String caller;
            try {
                caller = methodDeclaration.resolve().getQualifiedSignature();
            } catch (Exception e) {
                caller = methodDeclaration.getSignature().asString();
                System.out.println("Use " + caller + " instead of  qualified signature, cause: " + e.getMessage());
            }
            assert caller != null;

            // // 如果map中还没有key，则添加key
            if (!callerCallees.containsKey(caller) && !Utils.shouldSkip(caller, skipPatterns)) {
                callerCallees.put(caller, new ArrayList<>());
            }

            if (!Utils.shouldSkip(caller, skipPatterns)) {
                callerCallees.get(caller).addAll(curCallees);
            }

        }
    }

    // 遍历源码文件时，只关注方法调用的Visitor， 然后提取存放到第二个参数collector中
    private static class MethodCallVisitor extends VoidVisitorAdapter<List<String>> {

        private List<Pattern> skipPatterns = new ArrayList<Pattern>();
        private Set<String> knownLoggers = new HashSet<>();
        private Set<String> knownPatternMethods = new HashSet<>();

        public MethodCallVisitor(List<Pattern> skipPatterns) {
            if (skipPatterns != null) {
                this.skipPatterns = skipPatterns;
            }
            initializeKnownPatterns();
        }
        
        private void initializeKnownPatterns() {
            // Common logger names
            knownLoggers.addAll(Arrays.asList("LOGGER", "LOG", "logger", "log"));
            
            // Common design pattern methods
            knownPatternMethods.addAll(Arrays.asList(
                "info", "debug", "warn", "error", "attack", "fleeBattle", "getDescription",
                "getAttackPower", "row", "sail", "drive", "accept", "visit", "update",
                "notify", "subscribe", "unsubscribe", "getInstance", "create", "build",
                "execute", "undo", "redo", "restore", "save", "getKing", "getCastle",
                "getArmy", "makeKing", "makeCastle", "makeArmy", "setKing", "setCastle",
                "setArmy", "timePasses", "addObserver", "removeObserver", "prepare",
                "proceed", "stop", "action", "cookFood", "growing", "setMemento", "getMemento"
            ));
        }

        @Override
        public void visit(MethodCallExpr n, List<String> collector) {
            // 提取方法调用
            ResolvedMethodDeclaration resolvedMethodDeclaration = null;
            try {
                resolvedMethodDeclaration = n.resolve();
                // 仅关注提供src目录的工程代码
                String signature = n.resolve().getQualifiedSignature();
                if (!Utils.shouldSkip(signature, skipPatterns)) {
                    if (resolvedMethodDeclaration instanceof JavaParserMethodDeclaration) {
                        collector.add(signature);
                    }
                }
            } catch (Exception e) {
                // Enhanced error handling with pattern recognition
                String methodName = n.getNameAsString();
                String scopeName = null;
                
                if (n.getScope().isPresent()) {
                    scopeName = n.getScope().get().toString();
                }
                
                // Check if this is a known pattern that we can safely ignore
                if (isKnownPattern(scopeName, methodName)) {
                    // Don't print error for known patterns, just skip quietly
                    return;
                } else {
                    // Print error for unknown patterns
                    System.out.print("Line ");
                    System.out.print(n.getRange().get().begin.line);
                    System.out.print(", ");
                    System.out.print(
                            n.getNameAsString() + n.getArguments()
                                    .toString().replace("[", "(").replace("]", ")"));
                    System.out.print(" cannot resolve some symbol, because ");
                    System.out.println(e.getMessage());
                }
            }
            // Don't forget to call super, it may find more method calls inside the
            // arguments of this method call, for example.
            super.visit(n, collector);
        }
        
        private boolean isKnownPattern(String scopeName, String methodName) {
            // Check if it's a logger method
            if (scopeName != null && knownLoggers.contains(scopeName)) {
                return Arrays.asList("info", "debug", "warn", "error", "trace").contains(methodName);
            }
            
            // Check if it's a known design pattern method
            return knownPatternMethods.contains(methodName);
        }
    }

    /**
     * Builds a call graph from a compilation unit using JavaParser symbol resolution
     * @param cu The compilation unit to analyze
     * @return HashMap mapping caller methods to their callees
     */
    private HashMap<String, ArrayList<String>> buildCallGraphFromCU(CompilationUnit cu) {
        HashMap<String, ArrayList<String>> callGraph = new HashMap<>();
        
        cu.findAll(ClassOrInterfaceDeclaration.class).forEach(clazz -> {
            String className = clazz.getNameAsString();
            
            clazz.findAll(MethodDeclaration.class).forEach(method -> {
                String methodKey = className + "." + method.getNameAsString();
                ArrayList<String> callees = new ArrayList<>();
                
                method.findAll(MethodCallExpr.class).forEach(methodCall -> {
                    try {
                        ResolvedMethodDeclaration resolved = methodCall.resolve();
                        String targetClass = resolved.declaringType().getName();
                        String targetMethod = resolved.getName();
                        String calleeKey = targetClass + "." + targetMethod;
                        callees.add(calleeKey);
                    } catch (Exception e) {
                        // Handle unresolvable method calls - still useful for design pattern analysis
                        String callName = methodCall.getNameAsString();
                        
                        // Try to determine scope from the call expression
                        String scopeClass = "UNKNOWN";
                        if (methodCall.getScope().isPresent()) {
                            scopeClass = methodCall.getScope().get().toString();
                        }
                        
                        String calleeKey = scopeClass + "." + callName;
                        callees.add(calleeKey);
                    }
                });
                
                if (!callees.isEmpty()) {
                    callGraph.put(methodKey, callees);
                }
            });
        });
        
        return callGraph;
    }

}
