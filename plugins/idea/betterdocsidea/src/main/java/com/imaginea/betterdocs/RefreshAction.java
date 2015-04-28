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
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
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
    private static final String EDITOR_ERROR = "Could not get editor any active editor";
    private static final String INFO = "info";

    private WindowObjects windowObjects = WindowObjects.getInstance();
    private ProjectTree projectTree = new ProjectTree();
    private EditorDocOps editorDocOps = new EditorDocOps();
    private ESUtils esUtils = new ESUtils();
    private JSONUtils jsonUtils = new JSONUtils();

    public RefreshAction() {
        super(BETTER_DOCS, BETTER_DOCS, AllIcons.Actions.Refresh);
    }

    @Override
    public final void actionPerformed(@NotNull final AnActionEvent anActionEvent) {
        windowObjects.setProject(anActionEvent.getProject());
        PropertiesComponent propertiesComponent = PropertiesComponent.getInstance();

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
            JTree jTree = windowObjects.getjTree();

            Set<String> imports = editorDocOps.getImports(projectEditor.getDocument());
            Set<String> lines = editorDocOps.getLines(projectEditor, windowObjects.getDistance());
            Set<String> importsInLines = editorDocOps.importsInLines(lines, imports);

            DefaultTreeModel model = (DefaultTreeModel) jTree.getModel();
            DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
            root.removeAllChildren();
            jTree.setVisible(true);

            if (!importsInLines.isEmpty()) {
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

                    model.reload(root);
                    jTree.addTreeSelectionListener(projectTree.getTreeSelectionListener(root));
                } else {
                    Messages.showInfoMessage(EMPTY_ES_URL, INFO);
                }
            } else {
                jTree.updateUI();
            }
        } else {
            Messages.showMessageDialog(EDITOR_ERROR, INFO, Messages.getErrorIcon());
        }
    }
}
