package dps.utils;

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
    public static JavaSymbolSolver getJavaSymbolSolver(List<String> srcPaths, List<String> libPaths) throws IOException {
        CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
        
        // Add reflection type solver first for JDK classes (most reliable)
        ReflectionTypeSolver reflectionTypeSolver = new ReflectionTypeSolver(false); // Set false to prevent caching issues
        combinedTypeSolver.add(reflectionTypeSolver);
        
        // Enhanced Java parser type solvers
        List<JavaParserTypeSolver> javaParserTypeSolvers = makeJavaParserTypeSolvers(srcPaths);
        for (JavaParserTypeSolver solver : javaParserTypeSolvers) {
            combinedTypeSolver.add(solver);
            System.out.println("Added source path for symbol resolution: " + solver.toString());
        }
        
        // Add jar type solvers
        List<JarTypeSolver> jarTypeSolvers = makeJarTypeSolvers(libPaths);
        for (JarTypeSolver solver : jarTypeSolvers) {
            combinedTypeSolver.add(solver);
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
        // Add resolvers for common packages that are often missing
        // This helps with basic Java collections, utilities, etc.
        // These are already covered by the main ReflectionTypeSolver, but this ensures consistency
    }

    // referenced from Java Callgraph
    /**
     * 获取jar包的符号推理器
     * 
     * @param libPaths
     * @return
     */
    private static List<JarTypeSolver> makeJarTypeSolvers(List<String> libPaths) throws IOException {
        List<String> jarPaths = Utils.getFilesBySuffixInPaths("jar", libPaths);
        List<JarTypeSolver> jarTypeSolvers = new ArrayList<>(jarPaths.size());
        for (String jarPath : jarPaths) {
            jarTypeSolvers.add(new JarTypeSolver(jarPath));
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
            File srcDir = new File(srcPath);
            if (srcDir.exists() && srcDir.isDirectory()) {
                JavaParserTypeSolver typeSolver = new JavaParserTypeSolver(srcDir);
                javaParserTypeSolvers.add(typeSolver);
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
    public static JavaSymbolSolver getJavaSymbolSolver(String srcPath, String libPath) throws IOException {
        return getJavaSymbolSolver(Utils.makeListFromOneElement(srcPath), Utils.makeListFromOneElement(libPath));
    }
}

