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
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.io.IOException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.ToolTipManager;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellRenderer;

import org.jetbrains.annotations.NotNull;

public class RefreshAction extends AnAction {
    private static final String BETTER_DOCS = "BetterDocs";
    protected static final String EMPTY_ES_URL = "Please set/modify proper esURL in idea settings";
    protected static final String ES_URL = "esURL";
    protected static final String DISTANCE = "distance";
    protected static final String SIZE = "size";
    private static final String BETTERDOCS_SEARCH = "/betterdocs/_search?source=";
    protected static final String ES_URL_DEFAULT = "http://labs.imaginea.com/betterdocs";
    protected static final int DISTANCE_DEFAULT_VALUE = 10;
    protected static final int SIZE_DEFAULT_VALUE = 30;
    private static final String EDITOR_ERROR = "Could not get any active editor";
    private static final String INFO = "info";
    private static final String FORMAT = "%s %s %s";
    private static final String QUERYING = "Querying";
    private static final String FOR = "for";
    protected static final String EXCLUDE_IMPORT_LIST = "Exclude imports";
    protected static final String HELP_MESSAGE =
            "<html><center>No Results to display.<br> Please Select Some code and hit <img src='"
                    + AllIcons.Actions.Refresh + "'> </center> </html>";
    private static final String REPO_STARS = "Repo Stars";
    private static final String BANNER_FORMAT = "%s %s %s";
    private static final String HTML_U = "<html><u>";
    private static final String U_HTML = "</u></html>";

    private WindowObjects windowObjects = WindowObjects.getInstance();
    private ProjectTree projectTree = new ProjectTree();
    private EditorDocOps editorDocOps = new EditorDocOps();
    private ESUtils esUtils = new ESUtils();
    private JSONUtils jsonUtils = new JSONUtils();
    private PropertiesComponent propertiesComponent = PropertiesComponent.getInstance();

    public RefreshAction() {
        super(BETTER_DOCS, BETTER_DOCS, AllIcons.Actions.Refresh);
    }

    @Override
    public final void actionPerformed(@NotNull final AnActionEvent anActionEvent) {
        windowObjects.setProject(anActionEvent.getProject());

        windowObjects.setDistance(propertiesComponent.
                                    getOrInitInt(DISTANCE, DISTANCE_DEFAULT_VALUE));
        windowObjects.setSize(propertiesComponent.getOrInitInt(SIZE, SIZE_DEFAULT_VALUE));
        windowObjects.setEsURL(propertiesComponent.getValue(ES_URL, ES_URL_DEFAULT));

        try {
            runAction(anActionEvent);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    public final void runAction(final AnActionEvent anActionEvent) throws IOException {
        Project project = windowObjects.getProject();
        final Editor projectEditor = FileEditorManager.getInstance(project).getSelectedTextEditor();

        if (projectEditor != null) {
            windowObjects.getFileNameContentsMap().clear();
            windowObjects.getFileNameNumbersMap().clear();
            JTree jTree = windowObjects.getjTree();

            Set<String> imports = editorDocOps.getImports(projectEditor.getDocument(), project);

            if (propertiesComponent.isValueSet(EXCLUDE_IMPORT_LIST)) {
                String excludeImport = propertiesComponent.getValue(EXCLUDE_IMPORT_LIST);

                if (excludeImport != null) {
                    imports = editorDocOps.excludeConfiguredImports(imports, excludeImport);
                }
            }

            Set<String> lines = editorDocOps.getLines(projectEditor, windowObjects.getDistance());
            Set<String> internalImports = editorDocOps.getInternalImports(project);
            Set<String> externalImports =
                            editorDocOps.excludeInternalImports(imports, internalImports);
            Set<String> importsInLines = editorDocOps.importsInLines(lines, externalImports);

            DefaultTreeModel model = (DefaultTreeModel) jTree.getModel();
            DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
            root.removeAllChildren();
            jTree.setVisible(true);

            if (!importsInLines.isEmpty()) {
                windowObjects.getEditorPanel().removeAll();
                String esQueryJson = jsonUtils.getESQueryJson(importsInLines,
                                        windowObjects.getSize());
                String esResultJson = esUtils.getESResultJson(esQueryJson,
                                        windowObjects.getEsURL() + BETTERDOCS_SEARCH);

                if (!esResultJson.equals(EMPTY_ES_URL)) {
                    Map<String, String> fileTokensMap = esUtils.getFileTokens(esResultJson);
                    Map<String, ArrayList<CodeInfo>> projectNodes =
                                    new HashMap<String, ArrayList<CodeInfo>>();

                    projectTree.updateProjectNodes(imports, fileTokensMap, projectNodes);
                    projectTree.updateRoot(root, projectNodes);

                    Notifications.Bus.notify(new Notification(BETTER_DOCS,
                            String.format(FORMAT, QUERYING, windowObjects.getEsURL(), FOR),
                            importsInLines.toString(),
                            NotificationType.INFORMATION));

                    if (!projectNodes.isEmpty()) {
                        model.reload(root);
                        jTree.addTreeSelectionListener(projectTree.getTreeSelectionListener(root));
                        ToolTipManager.sharedInstance().registerComponent(jTree);
                        jTree.setCellRenderer(new ToolTipTreeCellRenderer());
                        jTree.addMouseListener(projectTree.getMouseListener(root));
                        windowObjects.getjTreeScrollPane().setViewportView(jTree);
                        buildCodePane(projectNodes);
                    } else {
                        showHelpInfo(HELP_MESSAGE);
                    }
                } else {
                    showHelpInfo(EMPTY_ES_URL);
                }
            } else {
                showHelpInfo(HELP_MESSAGE);
                jTree.updateUI();
            }
        } else {
            showHelpInfo(EDITOR_ERROR);
        }
    }

    private void showHelpInfo(String info) {
        windowObjects.getjTreeScrollPane().setViewportView(new JLabel(info));
    }

    protected final void buildCodePane(final Map<String, ArrayList<CodeInfo>> projectNodes) {
        // Take this from SettignsPanel
        int maxEditors = 10;
        int count = 0;
        JPanel editorPanel = windowObjects.getEditorPanel();
        List<CodeInfo> resultList = new ArrayList<CodeInfo>();

        for (Map.Entry<String, ArrayList<CodeInfo>> entry : projectNodes.entrySet()) {
            List<CodeInfo> codeInfoList = entry.getValue();
            for (CodeInfo codeInfo : codeInfoList) {
                if (count++ < maxEditors) {
                    resultList.add(codeInfo);
                }
            }
        }

        Collections.sort(resultList, new Comparator<CodeInfo>() {
            @Override
            public int compare(final CodeInfo o1, final CodeInfo o2) {
                Set<Integer> o1HashSet = new HashSet<Integer>(o1.getLineNumbers());
                Set<Integer> o2HashSet = new HashSet<Integer>(o2.getLineNumbers());
                return o2HashSet.size() - o1HashSet.size();
            }
        });

        for (CodeInfo codeInfo : resultList) {
            String fileContents;
            String fileName = codeInfo.getFileName();
            if (windowObjects.getFileNameContentsMap().containsKey(fileName)) {
                fileContents = windowObjects.getFileNameContentsMap().get(fileName);
            } else {
                fileContents = esUtils.getContentsForFile(codeInfo.getFileName());
                windowObjects.getFileNameContentsMap().put(fileName, fileContents);
            }

            Document tinyEditorDoc = EditorFactory.getInstance().
                    createDocument(fileContents);
            StringBuilder stringBuilder = new StringBuilder();
            Set<Integer> lineNumbersSet = new HashSet<Integer>(codeInfo.getLineNumbers());
            List<Integer> lineNumbersList = new ArrayList<Integer>(lineNumbersSet);
            Collections.sort(lineNumbersList);

            //Storing the prev line Numbers for displaying as blocks
            int prev = lineNumbersList.get(0);

            for (int line : lineNumbersList) {
                //Document is 0 indexed
                line = line - 1;
                if (line < tinyEditorDoc.getLineCount() - 1) {
                    if (prev != line - 1) {
                        stringBuilder.append(System.lineSeparator());
                        prev = line;
                    }
                    int startOffset = tinyEditorDoc.getLineStartOffset(line);
                    int endOffset = tinyEditorDoc.getLineEndOffset(line)
                            + tinyEditorDoc.getLineSeparatorLength(line);
                    String code = tinyEditorDoc.getCharsSequence().
                            subSequence(startOffset, endOffset).
                            toString().trim()
                            + System.lineSeparator();
                    stringBuilder.append(code);
                }
            }

            createEditor(editorPanel, codeInfo.toString(), codeInfo.getFileName(),
                    stringBuilder.toString());
        }
    }

    private void createEditor(final JPanel editorPanel, final String displayFileName,
                              final String fileName, final String contents) {
        Document tinyEditorDoc;
        tinyEditorDoc =
                EditorFactory.getInstance().createDocument(contents);
        tinyEditorDoc.setReadOnly(true);
        Project project = windowObjects.getProject();
        FileType fileType =
                FileTypeManager.getInstance().getFileTypeByExtension(MainWindow.JAVA);

        Editor tinyEditor =
                EditorFactory.getInstance().
                        createEditor(tinyEditorDoc, project, fileType, false);

        JPanel expandPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        int startIndex = fileName.indexOf('/');
        int endIndex = fileName.indexOf('/', startIndex + 1);

        String projectName = fileName.substring(0, endIndex);

        int repoId = windowObjects.getRepoNameIdMap().get(projectName);
        String stars;
        if (windowObjects.getRepoStarsMap().containsKey(projectName)) {
            stars = windowObjects.getRepoStarsMap().get(projectName).toString();
        } else {
            String repoStarsJson = jsonUtils.getRepoStarsJSON(repoId);
            stars = esUtils.getRepoStars(repoStarsJson);
            windowObjects.getRepoStarsMap().put(projectName, stars);
        }

        JLabel infoLabel = new JLabel(String.format(BANNER_FORMAT,
                projectName, REPO_STARS, stars));
        JButton expandButton = new JButton();
        expandButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, infoLabel.getMinimumSize().height));
        //expandButton.setPreferredSize(new Dimension(Integer.MAX_VALUE, infoLabel.getMinimumSize().height));
        expandButton.setBorderPainted(false);
        expandButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        expandButton.setText(String.format(BANNER_FORMAT, HTML_U, displayFileName , U_HTML));
        expandButton.setActionCommand(fileName);

        expandButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JButton jButton = (JButton) e.getSource();
                VirtualFile virtualFile = editorDocOps.getVirtualFile(displayFileName, windowObjects.getFileNameContentsMap().get(jButton.getActionCommand()));
                FileEditorManager.getInstance(windowObjects.getProject()).
                        openFile(virtualFile, true, true);
                Document document =
                        EditorFactory.getInstance().createDocument(windowObjects.getFileNameContentsMap().get(jButton.getActionCommand()));
                editorDocOps.addHighlighting(windowObjects.getFileNameNumbersMap().get(jButton.getActionCommand()), document);
                editorDocOps.gotoLine(windowObjects.getFileNameNumbersMap().get(jButton.getActionCommand()).get(0), document);
            }
        });

        expandPanel.add(expandButton);
        expandPanel.add(infoLabel);
        expandPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, expandButton.getMinimumSize().height));
        expandPanel.revalidate();
        expandPanel.repaint();

        editorPanel.add(expandPanel);

        editorPanel.add(tinyEditor.getComponent());
        editorPanel.revalidate();
        editorPanel.repaint();
    }
}
