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

package com.imaginea.betterdocs;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaDirectoryService;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.containers.ContainerUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.jetbrains.annotations.NotNull;

public class EditorDocOps {
    public static final char DOT = '.';
    private static final String IMPORT_LIST = "IMPORT_LIST";
    private static final String IMPORT_STATEMENT = "IMPORT_STATEMENT";

    public final Set<String> importsInLines(final Iterable<String> lines,
                                            final Iterable<String> imports) {
        Set<String> importsInLines = new HashSet<String>();

        for (String line : lines) {
            StringTokenizer stringTokenizer = new StringTokenizer(line);
            while (stringTokenizer.hasMoreTokens()) {
                String token = stringTokenizer.nextToken();
                for (String nextImport : imports) {
                    String shortImportName = nextImport.substring(nextImport.lastIndexOf(DOT) + 1);
                    if (token.equals(shortImportName)) {
                        importsInLines.add(nextImport);
                    }
                }
            }
        }
        return importsInLines;
    }

    public final Set<String> getLines(final Editor projectEditor, final int distance) {
        Set<String> lines = new HashSet<String>();
        Document document = projectEditor.getDocument();
        String regex = "(\\s*(import|\\/?\\*|//)\\s+.*)|\\\".*";
        SelectionModel selectionModel = projectEditor.getSelectionModel();
        int head = 0;
        int tail = document.getLineCount() - 1;

        if (selectionModel.hasSelection()) {
            head = document.getLineNumber(selectionModel.getSelectionStart());
            tail = document.getLineNumber(selectionModel.getSelectionEnd());
            /*Selection model gives one more line if line is selected completely.
              By Checking if complete line is slected and decreasing tail*/
            if ((document.getLineStartOffset(tail) == selectionModel.getSelectionEnd())) {
                tail--;
            }

        } else {
            int currentLine = document.getLineNumber(projectEditor.getCaretModel().getOffset());

            if (currentLine - distance >= 0) {
                head = currentLine - distance;
            }

            if (currentLine + distance <= document.getLineCount() - 1) {
                tail = currentLine + distance;
            }
        }

        for (int j = head; j <= tail; j++) {
            String line =
                    document.getCharsSequence().subSequence(document.getLineStartOffset(j),
                            document.getLineEndOffset(j)).toString();
            String cleanedLine = line.replaceFirst(regex, "").replaceAll("\\W+", " ").trim();
            if (!cleanedLine.isEmpty()) {
                lines.add(cleanedLine);
            }
        }
        return lines;
    }

    public final Set<String> getImports(@NotNull final Document document,
                                        @NotNull final Project project) {
        PsiDocumentManager psiInstance = PsiDocumentManager.getInstance(project);
        Set<String> imports = new HashSet<String>();

        if (psiInstance != null && (psiInstance.getPsiFile(document)) != null) {
            PsiFile psiFile = psiInstance.getPsiFile(document);
            if (psiFile != null) {
                PsiElement[] psiRootChildren = psiFile.getChildren();
                    for (PsiElement element : psiRootChildren) {
                        if (element.getNode().getElementType().toString().equals(IMPORT_LIST)) {
                            PsiElement[] importListChildren = element.getChildren();
                            for (PsiElement importElement : importListChildren) {
                                if (importElement.getNode().getElementType().
                                        toString().equals(IMPORT_STATEMENT)) {
                                    PsiElement[] importsElementList = importElement.getChildren();
                                    // As second element contains the import value
                                    if (importsElementList[2] != null) {
                                        imports.add(importsElementList[2].getNode().getText());
                                    }
                                }
                            }
                        }
                    }
            }
        }
        return imports;
    }

    public final Set<String> getInternalImports(@NotNull final Project project) {
        final Set<String> internalImports = new HashSet<String>();
        GlobalSearchScope scope = GlobalSearchScope.projectScope(project);
        final List<VirtualFile> sourceRoots = new ArrayList<VirtualFile>();
        final ProjectRootManager projectRootManager = ProjectRootManager.getInstance(project);
        ContainerUtil.addAll(sourceRoots, projectRootManager.getContentSourceRoots());
        final PsiManager psiManager = PsiManager.getInstance(project);

        for (final VirtualFile root : sourceRoots) {
            final PsiDirectory directory = psiManager.findDirectory(root);
            if (directory != null) {
                final PsiDirectory[] subdirectories = directory.getSubdirectories();
                for (PsiDirectory subdirectory : subdirectories) {
                         final JavaDirectoryService jDirService =
                                JavaDirectoryService.getInstance();
                        if (jDirService != null && subdirectory != null
                                && jDirService.getPackage(subdirectory) != null) {
                            final PsiPackage rootPackage = jDirService.getPackage(subdirectory);
                            getCompletePackage(rootPackage, scope, internalImports);
                        }
                }
            }
        }
        return internalImports;
    }

    private void getCompletePackage(final PsiPackage completePackage,
                                    final GlobalSearchScope scope,
                                    final Set<String> internalImports) {
        PsiPackage[] subPackages = completePackage.getSubPackages(scope);

        if (subPackages.length != 0) {
            for (PsiPackage psiPackage : subPackages) {
                getCompletePackage(psiPackage, scope, internalImports);
            }
        } else {
            internalImports.add(completePackage.getQualifiedName());
        }
    }

    public final Set<String> excludeInternalImports(final Set<String> imports,
                                                     final Set<String> internalImports) {
        boolean matchFound;
        Set<String> externalImports = new HashSet<String>();
        for (String nextImport : imports) {
            matchFound = false;
            for (String internalImport : internalImports) {
                if (nextImport.startsWith(internalImport)
                        || internalImport.startsWith(nextImport)) {
                    matchFound = true;
                    break;
                }
            }
            if (!matchFound) {
                externalImports.add(nextImport);
            }
        }
        return externalImports;
    }

    public final Set<String> excludeConfiguredImports(final Set<String> imports,
            final String excludeImport) {
        Set<String> excludeImports = getExcludeImports(excludeImport);
        Set<String> excludedImports = new HashSet<>();
        imports.removeAll(excludeImports);
        excludedImports.addAll(imports);
        for (String importStatement : excludeImports) {
            try {
                Pattern pattern = Pattern.compile(importStatement);
                for (String nextImport : imports) {
                    Matcher matcher = pattern.matcher(nextImport);
                    if (matcher.find()) {
                        excludedImports.remove(nextImport);
                    }
                }
            } catch (PatternSyntaxException e) {
               e.printStackTrace();
           }
        }
        return excludedImports;
    }

    private Set<String> getExcludeImports(final String excludeImport) {
        Set<String> excludeImports = new HashSet<String>();

        if (!excludeImport.isEmpty()) {
            String[] excludeImportsArray = excludeImport.split(",");

            for (String imports : excludeImportsArray) {
                  String replacingDot = imports.replace(".", "\\.");
                  excludeImports.add(replacingDot.replaceAll("\\*", ".*").trim());
            }
        }
        return excludeImports;
    }
}
