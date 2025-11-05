package dpsSwum.utils;

import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JarTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

// referenced from Java Callgraph
/**
 * 符号解析工厂类
 * 
 * @author allen
 */
public class SymbolSolverFactory {
    // referenced from Java Callgraph
    /**
     * 获取符号推理器，以便获取某个类的具体来源
     * 
     * @param srcPaths
     * @param libPaths
     * @return
     */
    public static JavaSymbolSolver getJavaSymbolSolver(List<String> srcPaths, List<String> libPaths) {
        CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
        
        // Add reflection type solver first for JDK classes (most reliable)
        ReflectionTypeSolver reflectionTypeSolver = new ReflectionTypeSolver(false); // Set false to prevent caching issues
        combinedTypeSolver.add(reflectionTypeSolver);
        
        // Enhanced Java parser type solvers with error handling
        List<JavaParserTypeSolver> javaParserTypeSolvers = makeJavaParserTypeSolvers(srcPaths);
        for (JavaParserTypeSolver solver : javaParserTypeSolvers) {
            try {
                combinedTypeSolver.add(solver);
                System.out.println("Added source path for symbol resolution: " + solver.toString());
            } catch (Exception e) {
                System.out.println("Failed to add JavaParser type solver: " + e.getMessage());
            }
        }
        
        // Add jar type solvers with error handling
        List<JarTypeSolver> jarTypeSolvers = makeJarTypeSolvers(libPaths);
        for (JarTypeSolver solver : jarTypeSolvers) {
            try {
                combinedTypeSolver.add(solver);
            } catch (Exception e) {
                System.out.println("Failed to add JAR type solver: " + e.getMessage());
            }
        }
        
        // Add common library type solvers for typical dependencies
        addCommonLibraryResolvers(combinedTypeSolver);
        
        // Configure the symbol solver to be more lenient with unresolved symbols
        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(combinedTypeSolver);
        return symbolSolver;
    }
    
    /**
     * Add resolvers for common Java libraries that might be missing
     */
    private static void addCommonLibraryResolvers(CombinedTypeSolver combinedTypeSolver) {
        try {
            // Add resolvers for common packages that are often missing
            // This helps with basic Java collections, utilities, etc.
            ReflectionTypeSolver javaUtilSolver = new ReflectionTypeSolver();
            // These are already covered by the main ReflectionTypeSolver, but this ensures consistency
        } catch (Exception e) {
            System.out.println("Could not add common library resolvers: " + e.getMessage());
        }
    }

    // referenced from Java Callgraph
    /**
     * 获取jar包的符号推理器
     * 
     * @param libPaths
     * @return
     */
    private static List<JarTypeSolver> makeJarTypeSolvers(List<String> libPaths) {
        List<String> jarPaths = Utils.getFilesBySuffixInPaths("jar", libPaths);
        List<JarTypeSolver> jarTypeSolvers = new ArrayList<>(jarPaths.size());
        try {
            for (String jarPath : jarPaths) {
                jarTypeSolvers.add(new JarTypeSolver(jarPath));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return jarTypeSolvers;
    }

    // referenced from Java Callgraph
    /**
     * 获取工程源代码src的符号推理器
     * 
     * @param srcPaths
     * @return
     */
    private static List<JavaParserTypeSolver> makeJavaParserTypeSolvers(List<String> srcPaths) {
        List<JavaParserTypeSolver> javaParserTypeSolvers = new ArrayList<>();
        for (String srcPath : srcPaths) {
            try {
                File srcDir = new File(srcPath);
                if (srcDir.exists() && srcDir.isDirectory()) {
                    JavaParserTypeSolver typeSolver = new JavaParserTypeSolver(srcDir);
                    javaParserTypeSolvers.add(typeSolver);
                }
            } catch (Exception e) {
                System.out.println("Could not create type solver for path: " + srcPath + " - " + e.getMessage());
            }
        }
        return javaParserTypeSolvers;
    }

    // referenced from Java Callgraph
    /**
     * 获取符号推理器
     * 
     * @param srcPath
     * @param libPath
     * @return
     */
    public JavaSymbolSolver getJavaSymbolSolver(String srcPath, String libPath) {
        return getJavaSymbolSolver(Utils.makeListFromOneElement(srcPath), Utils.makeListFromOneElement(libPath));
    }
}

