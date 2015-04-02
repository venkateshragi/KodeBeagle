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

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.components.JBScrollPane;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import groovy.swing.impl.DefaultAction;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.border.Border;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

public class BetterDocsWindow implements ToolWindowFactory {

    @Override
    public void createToolWindowContent(Project project, ToolWindow toolWindow) {
        toolWindow.setIcon(Messages.getInformationIcon());
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Projects");

        JTree jTree = new JTree(root);
        jTree.setVisible(false);
        jTree.setAutoscrolls(true);
        jTree.setForeground(new Color(100, 155, 155));

        Document document = EditorFactory.getInstance().createDocument("");
        Editor windowEditor = EditorFactory.getInstance().createEditor(document, project, FileTypeManager.getInstance().getFileTypeByExtension("java"), false);

        BetterDocsAction action = new BetterDocsAction();
        action.setTree(jTree);
        action.setWindowEditor(windowEditor);

        DefaultActionGroup group = new DefaultActionGroup();
        group.add(action);
        JComponent toolBar = ActionManager.getInstance().createActionToolbar("BetterDocs", group, true).getComponent();

        EditorToggleAction toggleAction = new EditorToggleAction();
        DefaultActionGroup moveGroup = new DefaultActionGroup();
        moveGroup.add(toggleAction);


        JComponent moveBar = ActionManager.getInstance().createActionToolbar("Move", moveGroup, true).getComponent();

        FormLayout layout = new FormLayout(
                "pref, pref:grow",
                "pref, pref");

        CellConstraints cc = new CellConstraints();

        JBScrollPane jTreeScrollPane = new JBScrollPane();
        jTreeScrollPane.setViewportView(jTree);
        jTreeScrollPane.setAutoscrolls(true);
        jTreeScrollPane.setBackground(new Color(255, 0, 0));

        JPanel jPanel = new JPanel(layout);
        jPanel.setVisible(true);
        jPanel.add(toolBar , cc.xy(1, 1));
        jPanel.add(moveBar, cc.xy(1, 2));
        jPanel.add(jTreeScrollPane , cc.xy(2, 1));

        JPanel windowPanel = new JPanel();
        windowPanel.setVisible(true);
        windowPanel.add(windowEditor.getComponent());
        windowPanel.setPreferredSize(new Dimension(500, 500));

        final JSplitPane jSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, jPanel, windowEditor.getComponent());
        jSplitPane.setDividerLocation(0.1);
        toggleAction.setjSplitPane(jSplitPane);

        toolWindow.getComponent().getParent().add(jSplitPane);
    }
}

