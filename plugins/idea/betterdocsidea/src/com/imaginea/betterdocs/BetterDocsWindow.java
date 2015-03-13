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
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.packaging.impl.artifacts.JarFromModulesTemplate;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiPackage;
import com.intellij.ui.components.JBScrollPane;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JComponent;
import javax.swing.JPanel;

import java.awt.Component;
import java.awt.Color;
import java.util.HashSet;
import java.util.Set;

/**
 * BetterDocsWindow
 */
public class BetterDocsWindow implements ToolWindowFactory
{
    @Override
    public void createToolWindowContent(Project project, ToolWindow toolWindow)
    {
        toolWindow.setIcon(Messages.getInformationIcon());
        Component component = toolWindow.getComponent();

        JTextArea betterDocsText = new JTextArea(1, 1);
        betterDocsText.setBackground(new Color(255, 255, 0));
        betterDocsText.setEditable(false);
        betterDocsText.setLineWrap(false);

        JBScrollPane scrollPane = new JBScrollPane();
        scrollPane.setViewportView(betterDocsText);
        scrollPane.setAutoscrolls(true);

        JTextField distance = new JTextField(1);
        distance.setBackground(new Color(10, 100, 100));
        distance.setEditable(true);


        BetterDocsAction action = new BetterDocsAction();
        action.setTextPane(betterDocsText);
        
        DefaultActionGroup group = new DefaultActionGroup();
        group.add(action);
        JComponent toolBar = ActionManager.getInstance().createActionToolbar("BetterDocs", group, true).getComponent();

        FormLayout layout = new FormLayout(
                "left:pref, fill:pref:grow",
                "fill:pref:grow");
        CellConstraints cc = new CellConstraints();

        JPanel p = new JPanel(layout);
        p.add(toolBar, cc.xy (1, 1));
        p.add(scrollPane, cc.xy (2, 1));
        p.add(distance, cc.xy (1, 1));

        component.getParent().add(p);
    }
}
