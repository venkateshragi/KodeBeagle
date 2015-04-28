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

import com.intellij.icons.AllIcons;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.treeStructure.Tree;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;

public class MainWindow implements ToolWindowFactory, Disposable {

    private static final String COLUMN_SPECS = "pref, pref:grow";
    private static final String ROW_SPECS = "pref";
    private static final String PROJECTS = "Projects";
    private static final String JAVA = "java";
    private static final double DIVIDER_LOCATION = 0.5;
    private Editor windowEditor;

    @Override
    public final void createToolWindowContent(final Project project, final ToolWindow toolWindow) {
        toolWindow.setIcon(AllIcons.Toolwindows.Documentation);
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(PROJECTS);

        JTree jTree = new Tree(root);
        jTree.setVisible(false);
        jTree.setAutoscrolls(true);

        Document document = EditorFactory.getInstance().createDocument("");
        windowEditor = EditorFactory.getInstance().
                        createEditor(document, project, FileTypeManager.getInstance().
                                getFileTypeByExtension(JAVA), false);

        RefreshAction action = new RefreshAction();
        WindowObjects windowObjects = WindowObjects.getInstance();

        windowObjects.setTree(jTree);
        windowObjects.setWindowEditor(windowEditor);

        DefaultActionGroup group = new DefaultActionGroup();
        group.add(action);
        JComponent toolBar = ActionManager.getInstance().
                                            createActionToolbar("BetterDocs", group, true).
                                            getComponent();

        FormLayout layout = new FormLayout(
                COLUMN_SPECS,
                ROW_SPECS);

        CellConstraints cc = new CellConstraints();

        JBScrollPane jTreeScrollPane = new JBScrollPane();
        jTreeScrollPane.setViewportView(jTree);
        jTreeScrollPane.setAutoscrolls(true);

        JPanel jPanel = new JPanel(layout);
        jPanel.setVisible(true);
        jPanel.add(toolBar, cc.xy(1, 1));
        jPanel.add(jTreeScrollPane, cc.xy(2, 1));

        JBScrollPane jbScrollPane = new JBScrollPane();
        jbScrollPane.setViewportView(windowEditor.getComponent());

        final JSplitPane jSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                                                        jbScrollPane, jPanel);
        jSplitPane.setDividerLocation(DIVIDER_LOCATION);

        toolWindow.getComponent().getParent().add(jSplitPane);
    }

    @Override
    public final void dispose() {
        if (windowEditor != null) {
            EditorFactory.getInstance().releaseEditor(windowEditor);
        }
    }
}
