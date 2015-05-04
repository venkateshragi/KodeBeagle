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
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaDirectoryService;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.containers.ContainerUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.jetbrains.annotations.NotNull;

public class EditorDocOps {
    private static final String IMPORT = "import ";
    public static final char DOT = '.';

    public final Set<String> importsInLines(final Iterable<String> lines,
                                            final Iterable<String> imports) {
        Set<String> importsInLines = new HashSet<String>();

        for (String line : lines) {
            for (String nextImport : imports) {
                if (line.contains(nextImport.substring(nextImport.lastIndexOf(DOT) + 1))) {
                    importsInLines.add(nextImport);
                }
            }
        }
        return importsInLines;
    }

    public final Set<String> getLines(final Editor projectEditor, final int distance) {
        Set<String> lines = new HashSet<String>();
        Document document = projectEditor.getDocument();
        int head = 0;
        int tail = document.getLineCount() - 1;

        if (projectEditor.getSelectionModel().hasSelection()) {
            head = document.getLineNumber(projectEditor.getSelectionModel().getSelectionStart());
            tail = document.getLineNumber(projectEditor.getSelectionModel().getSelectionEnd());
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
            String line = document.getCharsSequence().
                                    subSequence(document.getLineStartOffset(j),
                                                    document.getLineEndOffset(j)).toString();
            if (!line.contains(IMPORT)) {
                lines.add(line);
            }
        }
        return lines;
    }

    public final Set<String> getImports(final Document document) {
        int startLine = 0;
        int endLine = document.getLineCount() - 1;
        Set<String> imports = new HashSet<String>();
        for (int i = startLine; i <= endLine; i++) {
            int lineStart = document.getLineStartOffset(i);
            int lineEnd = document.getLineEndOffset(i) + document.getLineSeparatorLength(i);
            String line = document.getCharsSequence().
                                    subSequence(lineStart, lineEnd).toString();
            if (line.contains(IMPORT) && !line.contains("*")) {
                imports.add(line.replace(IMPORT, "").replace(";", "").trim());
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
                if (nextImport.startsWith(internalImport)) {
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
}
