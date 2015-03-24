/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.betterdocs.parser;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("rawtypes")
public class MethodVisitor extends VoidVisitorAdapter {

    private List<ImportDeclaration> imports;
    private Map<String, String> importDeclMap;
    private Map<String, String> fieldVariableMap = new HashMap<String, String>();
    private String className = null;
    private Map<String, List<String>> methodCallStack = new HashMap<String, List<String>>();
    private String currentMethod;
    private HashMap<String, String> nameVsTypeMap;
    private HashMap<String, ArrayList<Integer>> lineNumbersMap = new HashMap<String, ArrayList<Integer>>();

    public void parse(String classcontent, String filename) throws ParseException, IOException {
        if (classcontent == null || classcontent.isEmpty()) {
            System.err.println("No class content to parse... " + filename);
        }
        try {
            parse(new ByteArrayInputStream(classcontent.getBytes()));
        } catch (Throwable e) {
            System.err.println("Could not parse. Skipping file: " + filename + ", exception: " + e.getMessage());
        }

        //System.out.println("Parsed file : " + filename);
    }

    @SuppressWarnings("unchecked")
    public void parse(InputStream in) throws Throwable {
        CompilationUnit cu = null;
        try {
            // parse the file
            cu = JavaParser.parse(in);
        } catch (Throwable e) {
            throw e;
        } finally {
            in.close();
        }

        if (cu != null) {
            List<ImportDeclaration> imports = cu.getImports();
            PackageDeclaration pakg = cu.getPackage();
            String pkg = "";
            pkg = pakg != null ? pakg.getName().toString() : pkg;
            setImports(imports);
            visit(cu, pkg);
        }
    }

    @Override
    public void visit(ClassOrInterfaceDeclaration n, Object arg) {
        className = arg + "." + n.getName();
        List<BodyDeclaration> members = n.getMembers();
        for (BodyDeclaration b : members) {
            if (b instanceof FieldDeclaration) {
                visit((FieldDeclaration) b, null);
            } else if (b instanceof MethodDeclaration) {
                visit((MethodDeclaration) b, null);
            }
        }
    }

    @Override
    public void visit(FieldDeclaration n, Object arg) {
        List<VariableDeclarator> variables = n.getVariables();
        for (VariableDeclarator v : variables) {
            Type type = n.getType();
            fieldVariableMap.put(v.getId().toString(),
                    fullType(type.toString()));
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void visit(MethodDeclaration n, Object arg) {
        nameVsTypeMap = new HashMap<String, String>();
        List<Parameter> parameters = n.getParameters();
        if (parameters != null) {
            for (Parameter p : parameters) {
                String type = p.getType().toString();
                nameVsTypeMap.put(p.getId().toString(), fullType(type));
            }
        }

        nameVsTypeMap.put("this", className);
        BlockStmt body = n.getBody();
        currentMethod = className + "." + n.getName();
        if (body != null) {
            visit(body, nameVsTypeMap);
        }
    }

    public void fancyVisit(Expression exp, Object arg) {
        if (exp instanceof MethodCallExpr) {
            visit((MethodCallExpr) exp, arg);
        } else if (exp instanceof AssignExpr) {
            visit((AssignExpr) exp, arg);
        } else if (exp instanceof VariableDeclarationExpr) {
            visit((VariableDeclarationExpr) exp, arg);
        } else if (exp instanceof ObjectCreationExpr) {
            visit((ObjectCreationExpr) exp, arg);
        }
    }

    @Override
    public void visit(ExpressionStmt n, Object arg) {
        Expression xpr = n.getExpression();
        fancyVisit(xpr, arg);
    }

    @Override
    public void visit(AssignExpr n, Object arg) {
        if (n != null) {
            try {
                Expression target = n.getTarget();
                Expression value = n.getValue();
                fancyVisit(target, arg);
                fancyVisit(value, arg);
                String targetScope = getFullScope(target);
                String valueScope = getFullScope(value);
                ArrayList<Integer> lines = new ArrayList<Integer>();
                lines.add(target.getBeginLine());
                //lines.add(target.getEndLine()); TODO: Maybe add this ?
                updateLineNumbersMap(targetScope, lines);
                lines.clear();
                lines.add(value.getBeginLine());
                updateLineNumbersMap(valueScope, lines);
            } catch (Exception e) {
               // e.printStackTrace();
            }
        }
    }

    private void updateLineNumbersMap(String targetScope, List<Integer> lines) {
        ArrayList<Integer> lineNumbers = lineNumbersMap.get(targetScope);
        if( lineNumbers == null) {
            lineNumbers = new ArrayList<Integer>();
        }
        lineNumbers.addAll(lines);
        lineNumbersMap.put(targetScope, lineNumbers);
    }

    @Override
    public void visit(ObjectCreationExpr n, Object arg1) {

        if (n != null) {
            try {
                String targetScope = fullType(n.getType().toString());
                ArrayList<Integer> lines = new ArrayList<Integer>();
                lines.add(n.getBeginLine());
                //lines.add(target.getEndLine()); TODO: Maybe add this ?
                updateLineNumbersMap(targetScope, lines);
            } catch (Exception e) {
                //e.printStackTrace();
            }
        }

    }

    @Override
    public void visit(MethodCallExpr n, Object arg) {
        Expression s = n.getScope();
        if (s instanceof MethodCallExpr) {
            MethodCallExpr m = (MethodCallExpr) s;
            visit(m, arg);
            // can't get the return type so atleast salvage on call.
            return;
        }
        updateCallStack(s, n.getName());
    }

    private void updateCallStack(Expression s, String name) {
        String fullscope = getFullScope(s);
        if (s != null) {
            ArrayList<Integer> lines = new ArrayList<Integer>();
            lines.add(s.getBeginLine());
            updateLineNumbersMap(fullscope, lines);
        }
        List<String> stack = methodCallStack.get(currentMethod);
        if (stack == null) {
            stack = new ArrayList<String>();
            methodCallStack.put(currentMethod, stack);
        }

        String call = fullscope + "." + name;
        boolean isArrayCall = call.contains("[") && call.contains("]");
        if (!isArrayCall) {
            stack.add(call);
        }
    }

    private String getFullScope(Expression s) {
        String scope = "this";
        if (s != null) {
            scope = s.toString();
        }
        String fullscope = nameVsTypeMap.get(scope);

        // if scope is null, then static call or field decl
        if (fullscope == null)
            fullscope = fieldVariableMap.get(scope);
        if (fullscope == null)
            fullscope = importDeclMap.get(scope);
        if (fullscope == null)
            fullscope = scope;
        return fullscope;
    }

    @Override
    public void visit(VariableDeclarationExpr n, Object arg) {
        //System.out.println("\t \t Variable declare : " + n );
        List<VariableDeclarator> vars = n.getVars();
        for (VariableDeclarator v : vars) {
            String id = v.getId().toString();
            Expression initExpr = v.getInit();
            fancyVisit(initExpr, arg);
            String type = n.getType().toString();
            nameVsTypeMap.put(id, fullType(type));
            ArrayList<Integer> lines = new ArrayList<Integer>();
            lines.add(n.getBeginLine());
            updateLineNumbersMap(fullType(type), lines);
        }

    }

    private String fullType(String type) {
        String fullType = importDeclMap.get(type.toString());
        fullType = fullType == null ? type.toString() : fullType;
        return fullType;
    }

    public List<ImportDeclaration> getImports() {
        return imports;
    }

    public void setImports(List<ImportDeclaration> imports) {
        this.imports = imports;
        importDeclMap = new HashMap<String, String>();
        if (imports != null) {
            for (ImportDeclaration d : imports) {
                String name = d.getName().toString();

                String[] tokens = name.split("\\.");
                if (tokens != null && tokens.length > 0) {
                    importDeclMap.put(tokens[tokens.length - 1], name);
                }
            }

        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void visit(CatchClause n, Object arg) {
        MultiTypeParameter exception = n.getExcept();

        String name = exception.getId().getName();
        for(Type t: exception.getTypes()) {
            nameVsTypeMap.put(name, fullType(t.toString()));
        }

        if (n != null) {
            visit(n.getCatchBlock(), arg);
        }
    }

    public Map<String, List<String>> getMethodCallStack() {
        return methodCallStack;
    }

    public HashMap<String, ArrayList<Integer>> getLineNumbersMap() {
        return lineNumbersMap;
    }

}
