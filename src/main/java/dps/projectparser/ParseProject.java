package dps.projectparser;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ParserConfiguration.LanguageLevel;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserMethodDeclaration;

import dps.designpatternidentifier.CheckPattern;
import dps.summarygenerator.Summarise;
import dps.utils.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

import org.apache.commons.collections4.MultiValuedMap;

public class ParseProject {

    // reference: java callgraph
    // 需要跳过的pattern列表
    private List<Pattern> skipPatterns = new ArrayList<Pattern>();

    public HashMap<String, Object> parseProject(File directory) throws FileNotFoundException, IOException {

        ArrayList<File> fileArrayList = new ArrayList<>();

        // referenced from Java Callgraph
        ArrayList<String> srcPathList = new ArrayList<>();
        ArrayList<String> libPathList = new ArrayList<>();

        // names of all files in directory added to fileArrayList (list not tree)
        // srcPathList and libPathList consist of abs paths of src and lib folders
        fetchFiles(directory, fileArrayList, srcPathList, libPathList);

        // referenced from Java Callgraph
        JavaSymbolSolver symbolSolver = SymbolSolverFactory.getJavaSymbolSolver(srcPathList, libPathList);
        StaticJavaParser.getParserConfiguration().setSymbolResolver(symbolSolver);
        StaticJavaParser.getParserConfiguration().setLanguageLevel(LanguageLevel.BLEEDING_EDGE);

        // referenced from Java callgraph
        // 获取src目录中的全部java文件，并进行解析
        HashMap<String, ArrayList<String>> callerCallees = new HashMap<>();

        HashMap<String, HashMap> parsedFile = new HashMap<>();
        CheckPattern checkPattern = new CheckPattern();
        Summarise summarise = new Summarise();

        ArrayList designPatternArrayList = new ArrayList<>();

        HashMap<String, MultiValuedMap<String, String>> summaries = new HashMap<>();
        HashMap<String, HashMap<String, HashSet<String>>> summaryMap = new HashMap<String, HashMap<String, HashSet<String>>>();
        String finalSummary = "";

        // go through all files under the project
        for (File file : fileArrayList) {
            HashMap<String, ArrayList> fileDetails = new HashMap<>();
            CompilationUnit compilationUnit = parseFileToCompilationUnit(file);
            
            if (compilationUnit != null) {
                // File parsed successfully - extract detailed information
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
                extract(compilationUnit, callerCallees, skipPatterns);
            } else {
                // File couldn't be parsed - create empty details but still include in summary
                fileDetails.put("FIELDDETAIL", new ArrayList<>());
                fileDetails.put("CONSTRUCTORDETAIL", new ArrayList<>());
                fileDetails.put("VARIABLEDETAIL", new ArrayList<>());
                fileDetails.put("METHODDETAIL", new ArrayList<>());
                fileDetails.put("CLASSORINTERFACEDETAIL", new ArrayList<>());
                // Note: Can't extract call graph info for unparseable files
            }

            // Always add file to parsedFile map for summary generation
            parsedFile.put(Utils.getBaseName(file.getName()), fileDetails);
        }

        // merge the features with the callgraph
        HashMap<String, Object> parsedProject = new HashMap<>();
        HashMap extractedCallGraph = extractCallgraphResults(parsedFile, callerCallees);

        // Only return empty if no files were processed at all
        if (parsedFile.isEmpty())
            return new HashMap<>();
        
        // Extract design patterns only if call graph information is available
        if (!extractedCallGraph.isEmpty()) {
            checkPattern.extractDesignPattern(extractedCallGraph, designPatternArrayList);
        }

        // Decide which data to summarise/store: prefer call graph enriched data if available
        HashMap dataToStore = extractedCallGraph.isEmpty() ? parsedFile : extractedCallGraph;

        // Always run the summariser so that every parsed file gets a CSV row (even if there are no
        // detected design patterns). The Summarise class internally skips design-pattern-specific
        // processing when designPatternArrayList is empty and will still produce class/method
        // summaries for files without patterns.
        finalSummary = summarise.summarise(dataToStore, designPatternArrayList, summaries, directory.getName());

        // Only populate the structured summaryMap if any design-pattern summaries were produced
        if (!summaries.isEmpty()) {
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

        parsedProject.put(directory.getName(), dataToStore);
        parsedProject.put("design_pattern", designPatternArrayList);
        parsedProject.put("summary_NLG", summaryMap);
        parsedProject.put("final_summary", finalSummary);

        // return the result, which contains all files of the project, stored in the
        // hashmap, the key is file name, the value is the details.
        return parsedProject;
    }

    /**
     * Helper method to parse file to CompilationUnit with proper exception handling
     * @param file The file to parse
     * @return CompilationUnit or null if parsing fails
     */
    private CompilationUnit parseFileToCompilationUnit(File file) {
        try {
            return StaticJavaParser.parse(file);
        } catch (Exception e) {
            System.out.println("Skipping file due to parse error: " + file.getName() + ", " + e.getMessage());
            return null;
        } catch (Error e) {
            System.out.println("Skipping file due to parse error: " + file.getName() + ", " + e.getMessage());
            return null;
        }
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
        if (dir.getName().equals("src")) {
            srcPathList.add(dir.getAbsolutePath());
        }

        if (dir.getName().equals("lib")) {
            libPathList.add(dir.getAbsolutePath());
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
            String caller = getQualifiedSignature(methodDeclaration);
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

        public MethodCallVisitor(List<Pattern> skipPatterns) {
            if (skipPatterns != null) {
                this.skipPatterns = skipPatterns;

            }
        }

        @Override
        public void visit(MethodCallExpr n, List<String> collector) {
            // 提取方法调用
            String signature = ParseProject.getResolvedMethodSignature(n);
            if (signature != null && !Utils.shouldSkip(signature, skipPatterns)) {
                ResolvedMethodDeclaration resolvedMethodDeclaration;
                try {
                    resolvedMethodDeclaration = n.resolve();
                    if (resolvedMethodDeclaration instanceof JavaParserMethodDeclaration) {
                        collector.add(signature);
                    }
                } catch (Exception e) {
                    // Continue execution - just log the issue
                }
            }
            // Don't forget to call super, it may find more method calls inside the
            // arguments of this method call, for example.
            super.visit(n, collector);
        }
    }

    /**
     * Helper method to get qualified signature with fallback handling
     * @param methodDeclaration The method declaration
     * @return qualified signature or simple signature as fallback
     */
    private String getQualifiedSignature(MethodDeclaration methodDeclaration) {
        try {
            return methodDeclaration.resolve().getQualifiedSignature();
        } catch (Exception e) {
            String fallback = methodDeclaration.getSignature().asString();
            System.out.println("Use " + fallback + " instead of qualified signature, cause: " + e.getMessage());
            return fallback;
        }
    }

    /**
     * Helper method to get resolved method signature with error handling
     * @param methodCall The method call expression
     * @return qualified signature or null if resolution fails
     */
    private static String getResolvedMethodSignature(MethodCallExpr methodCall) {
        try {
            return methodCall.resolve().getQualifiedSignature();
        } catch (Exception e) {
            System.out.print("Line ");
            System.out.print(methodCall.getRange().get().begin.line);
            System.out.print(", ");
            System.out.print(
                    methodCall.getNameAsString() + methodCall.getArguments()
                            .toString().replace("[", "(").replace("]", ")"));
            System.out.print(" cannot resolve some symbol, because ");
            System.out.println(e.getMessage());
            return null;
        }
    }

}
