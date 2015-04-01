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

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiFile;

public class CodeBlock extends AnAction {

    public CodeBlock() {
        super("BetterDocs text box");
    }

    public void actionPerformed(AnActionEvent e) {
        Project project = e.getData(PlatformDataKeys.PROJECT);
        //PsiFile psiFile = null;

        //Editor editor = (Editor) DataManager.getInstance().getDataContext().getData(DataConstants.EDITOR);
        Editor editor = DataKeys.EDITOR.getData(e.getDataContext());
        Document document = editor.getDocument();

        String selectedText;

        //For dealing with selection
        boolean isSelected = editor.getSelectionModel().hasSelection();
        if (!isSelected) {
            selectedText = "Please select some code to get recommendation";
        } else {

            int startLine = document.getLineNumber(editor.getSelectionModel().getSelectionStart());
            int endLine = document.getLineNumber(editor.getSelectionModel().getSelectionEnd());
            int startOffset = document.getLineStartOffset(startLine);
            int endOffset = document.getLineEndOffset(endLine) + document.getLineSeparatorLength(endLine);
            selectedText = document.getCharsSequence().subSequence(startOffset, endOffset).toString();
        }


        int offset = editor.getCaretModel().getOffset();

        String distance = Messages.showInputDialog(project, "approx code distance", "input", Messages.getQuestionIcon());

        try{
            Integer.parseInt(distance);
            Messages.showMessageDialog(getNeighbourCode(document, document.getLineNumber(offset), Integer.parseInt(distance)), "Searchable tokens", Messages.getInformationIcon());
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

    //Pass currentLine as currentLine + 1 because counting starts from 0
    public String getNeighbourCode(Document document, int curLine, int distance) {
        StringBuilder stringBuilder = new StringBuilder();
        int startLine = curLine - distance >= 0 ? curLine - distance : 0;
        int endLine = curLine + distance <= document.getLineCount() - 1 ? curLine + distance : document.getLineCount() - 1;
        for (int i = startLine; i <= endLine; i++) {
            stringBuilder.append(document.getCharsSequence().subSequence(document.getLineStartOffset(i), document.getLineEndOffset(i) + document.getLineSeparatorLength(i)));
        }
        return stringBuilder.toString();
    }
}

