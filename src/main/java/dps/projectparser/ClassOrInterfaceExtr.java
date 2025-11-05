package dps.projectparser;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import dps.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;

public class ClassOrInterfaceExtr extends VoidVisitorAdapter<Void> {
    private ArrayList<HashMap<String, Object>> classInterfaceInfoHashArray;

    public ClassOrInterfaceExtr() {
        this.classInterfaceInfoHashArray = new ArrayList<>();
    }

    public ArrayList<HashMap<String, Object>> getClassInterfaceInfo(CompilationUnit compilationUnit) {
        compilationUnit.accept(new VoidVisitorAdapter<Void>() {
            @Override
            public void visit(ClassOrInterfaceDeclaration cid, Void arg) {
                HashMap<String, Object> classInterfaceInfo = new HashMap<>();

                // CN
                classInterfaceInfo.put("CLASSNAME", cid.getName().toString());
                classInterfaceInfo.put("CLASSTYPEPARAMS", Utils.nodeListToArrayList(cid.getTypeParameters()));

                // CMT
                classInterfaceInfo.put("CLASSMODIFIERTYPE", Utils.nodeListToArrayList(cid.getModifiers()));
                classInterfaceInfo.put("CLASSACCESSSPECIFIER", cid.getAccessSpecifier().toString());

                // IMF
                classInterfaceInfo.put("IMPLEMENTSFROM",
                        Utils.nodeListToArrayList(cid.getImplementedTypes()));

                // EXF
                classInterfaceInfo.put("EXTENDSFROM", Utils.nodeListToArrayList(cid.getExtendedTypes()));

                // ION
                classInterfaceInfo.put("ISINTERFACEORNOT", cid.isInterface());

                // AON
                classInterfaceInfo.put("ISABSTRACTORNOT", cid.isAbstract());

                // NOMC
                classInterfaceInfo.put("NUMBEROFMETHODCALLS",
                        cid.findAll(MethodCallExpr.class).size());

                classInterfaceInfoHashArray.add(classInterfaceInfo);
            }

            @Override
            public void visit(EnumDeclaration ed, Void arg) {
                HashMap<String, Object> enumInfo = new HashMap<>();

                // CN
                enumInfo.put("CLASSNAME", ed.getName().toString());
                enumInfo.put("CLASSTYPEPARAMS", new ArrayList<>()); // Enums don't have type parameters

                // CMT
                enumInfo.put("CLASSMODIFIERTYPE", Utils.nodeListToArrayList(ed.getModifiers()));
                enumInfo.put("CLASSACCESSSPECIFIER", ed.getAccessSpecifier().toString());

                // IMF
                enumInfo.put("IMPLEMENTSFROM", Utils.nodeListToArrayList(ed.getImplementedTypes()));

                // EXF
                enumInfo.put("EXTENDSFROM", new ArrayList<>()); // Enums can't extend classes

                // ION
                enumInfo.put("ISINTERFACEORNOT", false); // Enums are not interfaces

                // AON
                enumInfo.put("ISABSTRACTORNOT", false); // Enums are not abstract

                // NOMC
                enumInfo.put("NUMBEROFMETHODCALLS",
                        ed.findAll(MethodCallExpr.class).size());

                classInterfaceInfoHashArray.add(enumInfo);
            }
        }, null);

        return classInterfaceInfoHashArray;
    }

}

