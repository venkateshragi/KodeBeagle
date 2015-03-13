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

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiPackage;

import javax.swing.SwingUtilities;
import javax.swing.JTextArea;

public class BetterDocsAction extends AnAction {

    JTextArea textPane;
    int distance;
    Project project;

    public BetterDocsAction() {
        super("BetterDocs", "BetterDocs", Messages.getInformationIcon());
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        setProject(e.getProject());
        runAction(e);
    }

    public void runAction(AnActionEvent e) {
        PsiFile pf = DataKeys.PSI_FILE.getData(e.getDataContext());
        Editor editor = DataKeys.EDITOR.getData(e.getDataContext());

        if (editor == null) {
            //no editor currently available
            return;
        }

        String tokens = editor.getDocument().getCharsSequence().toString();

        if (textPane != null) {
            setText(tokens);
        }
    }

    public void setText(String str) {
        final String buf = str;
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                textPane.setText(buf);
                textPane.repaint();
            }
        });
    }

    public void setTextPane(JTextArea textPane) {
        this.textPane = textPane;
    }

    public void setDistance(int distance) {
        this.distance = distance;
    }

    private String getNeighbourCode(int currentLine, int distance) {
        return null;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    private String getLocalPackages(Editor editor, Project project) {
        PsiPackage pack = JavaPsiFacade.getInstance(project).findPackage("");
        PsiPackage[] subPackages = pack.getSubPackages();

        int length = subPackages.length;
        StringBuilder sb = new StringBuilder();

        for(int i = 0; i < length; i++) {
            sb.append(subPackages[i].toString() + "\n");
        }
        return sb.toString();
    }
}
