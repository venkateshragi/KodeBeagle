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

package com.imaginea.kodebeagle.impl.visitor;

import com.google.common.collect.Sets;
import com.imaginea.kodebeagle.base.object.WindowObjects;
import com.intellij.psi.JavaRecursiveElementVisitor;
import com.intellij.psi.PsiAssignmentExpression;
import com.intellij.psi.PsiCatchSection;
import com.intellij.psi.PsiDeclarationStatement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiNewExpression;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiReturnStatement;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiTypeElement;
import com.intellij.psi.impl.source.tree.JavaElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.siyeh.ig.psiutils.ClassUtils;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PsiJavaElementVisitor extends JavaRecursiveElementVisitor {
    private Map<String, Set<String>> importVsMethods = new HashMap<>();
    private final Set<String> emptySet = Sets.newHashSet();
    private int startOffset;
    private int endOffset;

    public final Map<String, Set<String>> getImportVsMethods() {
        return importVsMethods;
    }

    public PsiJavaElementVisitor(final int start, final int end) {
        super();
        startOffset = start;
        endOffset = end;
    }

    private void addInMap(final String qualifiedName, final Set<String> methodNames) {
        if (importVsMethods.containsKey(qualifiedName)) {
            Set<String> methods = importVsMethods.get(qualifiedName);
            methods.addAll(methodNames);
            importVsMethods.put(qualifiedName, methods);
        } else {
            importVsMethods.put(qualifiedName, methodNames);
        }
    }

    @Override
    public final void visitElement(final PsiElement element) {
        super.visitElement(element);
        if (startOffset <= element.getTextOffset() && element.getTextOffset() <= endOffset) {
            if (element.getNode().getElementType().equals(JavaElementType.FIELD)) {
                visitPsiFields((PsiField) element);
            } else if (element.getNode().getElementType().
                    equals(JavaElementType.DECLARATION_STATEMENT)) {
                visitPsiDeclarationStatement((PsiDeclarationStatement) element);
            } else if (element.getNode().getElementType().equals(JavaElementType.CATCH_SECTION)) {
                visitPsiCatchSection((PsiCatchSection) element);
            } else if (element.getNode().getElementType().
                    equals(JavaElementType.RETURN_STATEMENT)) {
                visitPsiReturnStatement((PsiReturnStatement) element);
            } else {
                visitExpression(element);
            }
        }
    }

    private void visitExpression(final PsiElement element) {
        if (element.getNode().getElementType().equals(JavaElementType.NEW_EXPRESSION)) {
            visitPsiNewExpression((PsiNewExpression) element);
        } else if (element.getNode().getElementType().
                equals(JavaElementType.METHOD_CALL_EXPRESSION)) {
            visitPsiMethodCallExpression((PsiMethodCallExpression) element);
        } else if (element.getNode().getElementType().
                equals(JavaElementType.ASSIGNMENT_EXPRESSION)) {
            visitPsiAssignmentExpression((PsiAssignmentExpression) element);
        } else if (element.getNode().getElementType().
                equals(JavaElementType.REFERENCE_EXPRESSION)) {
            visitPsiReferenceExpression((PsiReferenceExpression) element);
        }
    }

    private void visitPsiReferenceExpression(final PsiReferenceExpression element) {
        PsiExpression psiExpression = element.getQualifierExpression();
        if (psiExpression != null) {
            PsiType psiType = psiExpression.getType();
            if (psiType != null) {
                String qualifiedName = removeSpecialSymbols(psiType.getCanonicalText());
                addInMap(qualifiedName, emptySet);
            }
        }
    }

    private void visitPsiAssignmentExpression(final PsiAssignmentExpression
                                                      assignmentExpression) {
        PsiType lExpressionType = assignmentExpression.getLExpression().getType();
        if (lExpressionType != null && !ClassUtils.isPrimitive(lExpressionType)) {
            String type = removeSpecialSymbols(lExpressionType.getCanonicalText());
            addInMap(type, emptySet);
        }
        PsiExpression rExpression = assignmentExpression.getRExpression();
        if (rExpression != null) {
            PsiType rExpressionType = rExpression.getType();
            if (rExpressionType != null && !ClassUtils.isPrimitive(rExpressionType)) {
                String type = removeSpecialSymbols(rExpressionType.getCanonicalText());
                addInMap(type, emptySet);
            }
        }
    }

    private void visitPsiDeclarationStatement(final PsiDeclarationStatement
                                                      declarationStatement) {
        Collection<PsiTypeElement> typeElements =
                PsiTreeUtil.findChildrenOfType(declarationStatement, PsiTypeElement.class);
        for (PsiTypeElement element : typeElements) {
            String type = removeSpecialSymbols(element.getType().getCanonicalText());
            addInMap(type, emptySet);
        }
    }

    private void visitPsiNewExpression(final PsiNewExpression element) {
        if (element.getType() != null) {
            PsiType psiType = element.getType();
            if (psiType != null && !ClassUtils.isPrimitive(psiType)) {
                String type = removeSpecialSymbols(psiType.getCanonicalText());
                addInMap(type, emptySet);
            }
        }
    }

    private void visitPsiFields(final PsiField psiField) {
        if (!ClassUtils.isPrimitive(psiField.getType())) {
            String type = removeSpecialSymbols(psiField.getType().getCanonicalText());
            if (psiField.getInitializer() != null) {
                PsiExpression psiExpression = psiField.getInitializer();
                if (psiExpression != null) {
                    PsiType psiType = psiExpression.getType();
                    if (psiType != null && !ClassUtils.isPrimitive(psiType)) {
                        String psiFieldInitializer =
                                removeSpecialSymbols(psiType.getCanonicalText());
                        addInMap(psiFieldInitializer, emptySet);
                    }
                }
            }
            addInMap(type, emptySet);
        }
    }

    private Set<String> getMethods(final PsiReferenceExpression methodExpr) {
        Set<String> methods = new HashSet<>();
        if (WindowObjects.getInstance().isIncludeMethods()) {
            PsiIdentifier[] identifiers =
                    PsiTreeUtil.getChildrenOfType(methodExpr, PsiIdentifier.class);
            if (identifiers != null) {
                for (PsiIdentifier identifier : identifiers) {
                    methods.add(identifier.getText());
                }
            }
        }
        return methods;
    }

    private void visitPsiMethodCallExpression(final PsiMethodCallExpression element) {
        PsiReferenceExpression methodExpr = element.getMethodExpression();
        if (methodExpr.getQualifierExpression() != null) {
            PsiExpression psiExpression = methodExpr.getQualifierExpression();
            if (psiExpression != null) {
                PsiType psiType = psiExpression.getType();
                if (psiType != null && !ClassUtils.isPrimitive(psiExpression.getType())) {
                    String type = removeSpecialSymbols(psiType.getCanonicalText());
                    addInMap(type, getMethods(methodExpr));
                } else if (psiExpression.getReference() != null
                        && !ClassUtils.isPrimitive(psiExpression.getType())) {
                    PsiReference psiReference = psiExpression.getReference();
                    if (psiReference != null) {
                        String type = removeSpecialSymbols(psiReference.getCanonicalText());
                        addInMap(type, getMethods(methodExpr));
                    }
                }
            }
        }
    }

    private void visitPsiCatchSection(final PsiCatchSection element) {
        PsiType catchType = element.getCatchType();
        if (catchType != null) {
            String qualifiedName = removeSpecialSymbols(catchType.getCanonicalText());
            addInMap(qualifiedName, emptySet);
        }
    }

    private void visitPsiReturnStatement(final PsiReturnStatement element) {
        PsiExpression returnValue = element.getReturnValue();
        if (returnValue != null) {
            PsiType returnType = returnValue.getType();
            if (returnType != null) {
                String qualifiedName = removeSpecialSymbols(returnType.getCanonicalText());
                addInMap(qualifiedName, emptySet);
            }
        }
    }

    private String removeSpecialSymbols(final String pType) {
        String type = pType;
        if (type != null && type.contains("<")) {
            type = type.substring(0, type.indexOf("<"));
        } else if (type != null && type.contains("[")) {
            type = type.substring(0, type.indexOf("["));
        }
        return type;
    }
}
