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

import com.intellij.openapi.application.Result;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.FoldRegion;
import org.jetbrains.annotations.NotNull;

public class WindowEditorOps {
    private static WindowObjects windowObjects = WindowObjects.getInstance();

    public final void addFoldings(final Document windowEditorDocument,
                                  final Iterable<Integer> linesForFolding) {
        final Editor windowEditor = windowObjects.getWindowEditor();
        windowEditor.getFoldingModel().runBatchFoldingOperation(new Runnable() {
            @Override
            public void run() {
                int prevLine = 0;

                cleanFoldingRegions(windowEditor);

                for (int line : linesForFolding) {
                    int currentLine = line - 1;
                    if (prevLine < windowEditorDocument.getLineCount()) {

                        int startOffset = windowEditorDocument.getLineStartOffset(prevLine);
                        int endOffset = windowEditorDocument.getLineEndOffset(currentLine - 1);

                        if (startOffset < endOffset && windowEditor.getFoldingModel() != null) {
                            windowEditor.getFoldingModel()
                                    .addFoldRegion(startOffset, endOffset, "...")
                                    .setExpanded(false);
                        }
                        prevLine = currentLine + 1;
                    }
                }
            }
        });
    }

    public final void writeToDocument(final CodeInfo codeInfo,
                                      final Document windowEditorDocument) {
        new WriteCommandAction(windowObjects.getProject()) {
            @Override
            protected void run(@NotNull final Result result) throws Throwable {
                windowEditorDocument.setReadOnly(false);
                windowEditorDocument.setText(codeInfo.getContents());
                windowEditorDocument.setReadOnly(true);
            }
        } .execute();
    }

    protected final void cleanFoldingRegions(final Editor windowEditor) {
        FoldRegion[] foldRegions = windowEditor.getFoldingModel().getAllFoldRegions();
        for (FoldRegion currentRegion : foldRegions) {
            windowEditor.getFoldingModel().removeFoldRegion(currentRegion);
        }
    }
}
