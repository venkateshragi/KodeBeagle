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

package com.imaginea.kodebeagle.ui;

import com.intellij.icons.AllIcons;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ex.ApplicationManagerEx;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.Gray;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.UIUtil;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JPanel;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LegalNotice extends DialogWrapper {
    private Project project;

    protected static final String KODE_BEAGLE_IDEA_LEGAL_NOTICE = "KodeBeagleIdeaLegalNotice";
    private static final String KODEBEAGLEIDEA = "kodebeagleidea";
    private static final String DECLINE = "Decline";
    private static final String LEGAL_NOTICE_TITLE = "KodeBeagle";
    private static final String ACCEPT = "Accept";
    private static final String DIV_STYLE_MARGIN_5PX = "<div style='margin:5px;'>";
    private static final String DIV = "</div>";
    private static final String KODEBEAGLE_NOTICE_FILENAME = "KODEBEAGLE_NOTICE";
    private static final Dimension MESSAGE_EDITOR_PANE_PREFERRED_SIZE = new Dimension(500, 100);
    private static final BorderLayout LEGAL_NOTICE_LAYOUT = new BorderLayout(10, 0);
    private static boolean legalNoticeAccepted =
            PropertiesComponent.getInstance().getBoolean(KODE_BEAGLE_IDEA_LEGAL_NOTICE,
                    false);


    public static boolean isLegalNoticeAccepted() {
        return legalNoticeAccepted;
    }

    protected final void showLegalNotice() {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                new LegalNotice(project).show();
            }
        });
    }

    protected LegalNotice(@Nullable final Project pProject) {
        super(pProject);
        this.project = pProject;
        setTitle(LEGAL_NOTICE_TITLE);
        setOKButtonText(ACCEPT);
        init();
        pack();
    }

    @Nullable
    @Override
    protected final JComponent createCenterPanel() {

        JPanel iconPanel = new JBPanel(new BorderLayout());
        iconPanel.add(new JBLabel(AllIcons.General.WarningDialog), BorderLayout.NORTH);

        JEditorPane messageEditorPane = new JEditorPane();
        messageEditorPane.setEditorKit(UIUtil.getHTMLEditorKit());
        messageEditorPane.setEditable(false);
        messageEditorPane.setPreferredSize(MESSAGE_EDITOR_PANE_PREFERRED_SIZE);
        messageEditorPane.setBorder(BorderFactory.createLineBorder(Gray._200));
        String text = DIV_STYLE_MARGIN_5PX + getLegalNoticeMessage() + DIV;
        messageEditorPane.setText(text);

        JPanel legalNoticePanel = new JPanel(LEGAL_NOTICE_LAYOUT);
        legalNoticePanel.add(iconPanel, BorderLayout.WEST);
        legalNoticePanel.add(messageEditorPane, BorderLayout.CENTER);
        return legalNoticePanel;
    }

    @NotNull
    @Override
    protected final Action[] createActions() {
        DialogWrapperAction declineAction = new DialogWrapperAction(DECLINE) {
            @Override
            protected final void doAction(final ActionEvent e) {
                PluginManagerCore.disablePlugin(KODEBEAGLEIDEA);
                ApplicationManagerEx.getApplicationEx().restart(true);
            }
        };
        return new Action[]{getOKAction(), declineAction, getCancelAction()};
    }

    @Override
    protected final void doOKAction() {
        super.doOKAction();
        PropertiesComponent.getInstance().setValue(KODE_BEAGLE_IDEA_LEGAL_NOTICE,
                Boolean.TRUE.toString());
    }

    @Override
    public final void doCancelAction() {
        super.doCancelAction();
        PluginManagerCore.disablePlugin(KODEBEAGLEIDEA);
        ApplicationManagerEx.getApplicationEx().restart(true);
    }

    private String getLegalNoticeMessage() {
        ClassLoader classLoader = this.getClass().getClassLoader();
        InputStream stream = classLoader.getResourceAsStream(KODEBEAGLE_NOTICE_FILENAME);
        StringBuilder legalNoticeMessage = new StringBuilder();
        try {
            String line;
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            if (stream != null) {
                while ((line = reader.readLine()) != null) {
                    legalNoticeMessage.append(line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                stream.close();
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        return String.format(legalNoticeMessage.toString());
    }
}
