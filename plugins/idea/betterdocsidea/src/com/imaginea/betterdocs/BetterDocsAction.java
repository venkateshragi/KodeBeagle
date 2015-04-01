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
import com.intellij.openapi.application.Result;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiPackage;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortOrder;
import org.jetbrains.annotations.NotNull;

import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BetterDocsAction extends AnAction {
    Project project;
    JTree jTree;
    Editor windowEditor;

    public void setWindowEditor(Editor windowEditor) {
        this.windowEditor = windowEditor;
    }

    public void setTree(JTree jTree) {
        this.jTree = jTree;
    }

    public BetterDocsAction() {
        super("BetterDocs", "BetterDocs", Messages.getInformationIcon());
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
        setProject(anActionEvent.getProject());

        try {
            runAction(anActionEvent);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    private DefaultMutableTreeNode getNodes(String projectName, List<CodeInfo> codeInfoList) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(projectName.replace("/", "-"));
        Set<String> fileNameSet = new HashSet<String>();
        Iterator codeInfoIterator = codeInfoList.iterator();
        while (codeInfoIterator.hasNext()) {
            CodeInfo codeInfo = (CodeInfo) codeInfoIterator.next();
            if (!fileNameSet.contains(codeInfo.getFileName())) {
                node.add(new DefaultMutableTreeNode(codeInfo));
                fileNameSet.add(codeInfo.getFileName());
            }
        }
        return node;
    }

    public void runAction(final AnActionEvent e) throws IOException {
        Editor projectEditor = DataKeys.EDITOR.getData(e.getDataContext());

        if (projectEditor == null) {
            //no editor currently available
            return;
        }

        Set<String> importsSet = getImportsAsSet(projectEditor.getDocument());
        Set<String> linesSet = getLinesAsSet(projectEditor.getDocument(), 0, projectEditor.getDocument().getLineCount() - 1);
        Set<String> importsInLinesSet = importsInLines(linesSet, importsSet);
        jTree.setVisible(true);

        DefaultTreeModel model = (DefaultTreeModel) jTree.getModel();
        final DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
        root.removeAllChildren();

        Map<String, ArrayList<CodeInfo>> projectNodes = new HashMap<String, ArrayList<CodeInfo>>();
        SearchHit[] results = getQueryResults(importsInLinesSet);

        if (results.length > 0) {
            for (SearchHit hit : results) {
                Map<String, Object> result = hit.getSource();
                String fileName = result.get("file").toString();
                String tokens = result.get("tokens").toString();

                ArrayList<Integer> lineNumbers = getLineNumbers(importsSet, tokens);
                String contents = getContentsForFile(fileName);
                CodeInfo codeInfo = new CodeInfo(fileName, lineNumbers, contents);

                String projectName = fileName.substring(0, fileName.indexOf("/", fileName.indexOf("/") + 1));
                if (projectNodes.containsKey(projectName)) {
                    projectNodes.get(projectName).add(codeInfo);
                } else {
                    projectNodes.put(projectName, new ArrayList<CodeInfo>(Arrays.asList(codeInfo)));
                }
            }
        }

        Iterator projectNodesIterator = projectNodes.entrySet().iterator();

        while (projectNodesIterator.hasNext()) {
            Map.Entry currentProject = (Map.Entry) projectNodesIterator.next();
            root.add(getNodes(currentProject.getKey().toString(), (List<CodeInfo>) currentProject.getValue()));
        }

        model.reload(root);

        jTree.addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent treeSelectionEvent) {
                DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode)
                        jTree.getLastSelectedPathComponent();

                if (selectedNode == null) {

                } else if (selectedNode.isLeaf() && root.getChildCount() > 0) {

                    final CodeInfo codeInfo = (CodeInfo) selectedNode.getUserObject();
                    final Document windowEditorDocument = windowEditor.getDocument();
                    new WriteCommandAction(project) {
                        @Override
                        protected void run(@NotNull Result result) throws Throwable {
                            windowEditorDocument.setReadOnly(false);
                            windowEditorDocument.setText(codeInfo.getContents());
                            windowEditorDocument.setReadOnly(true);
                        }
                    }.execute();

                    final ArrayList<Integer> linesForFolding = codeInfo.getLineNumbers();
                    linesForFolding.add(windowEditorDocument.getLineCount() + 1);
                    java.util.Collections.sort(linesForFolding);

                    windowEditor.getFoldingModel().runBatchFoldingOperation(new Runnable() {
                        @Override
                        public void run() {
                            Iterator linesFoldingIterator = linesForFolding.iterator();
                            int prevLine = 0;
                            while (linesFoldingIterator.hasNext()) {
                                int currentLine = Integer.parseInt(linesFoldingIterator.next().toString()) - 1;
                                if (prevLine < windowEditorDocument.getLineCount()) {

                                    int startOffset = windowEditorDocument.getLineStartOffset(prevLine);
                                    int endOffset = windowEditorDocument.getLineEndOffset(currentLine - 1);

                                    if (startOffset < endOffset) {
                                        try {
                                            windowEditor.getFoldingModel()
                                                    .addFoldRegion(startOffset, endOffset, "...")
                                                    .setExpanded(false);

                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    }
                                    prevLine = currentLine + 1;
                                }
                            }
                        }
                    });
                }
            }
        });
    }

    private ArrayList<Integer> getLineNumbers(Set<String> importsSet, String tokens) {

        ArrayList<Integer> lineNumbers = new ArrayList<Integer>();

        Matcher tokenMatcher = Pattern.compile(
                Pattern.quote("{")
                        + "(.*?)"
                        + Pattern.quote("}")
        ).matcher(tokens);

        while (tokenMatcher.find()) {
            String set = tokenMatcher.group(1);
            String importName = set.substring(set.lastIndexOf("importName=") + "importName=".length());
            if (importsSet.contains(importName)) {
                Pattern pattern = Pattern.compile("\\d+");
                Matcher lineMatcher = pattern.matcher(set);
                while (lineMatcher.find()) {
                    lineNumbers.add(Integer.parseInt(lineMatcher.group()));
                }
            }

        }

        return lineNumbers;
    }


    private String getContentsForFile(String file) {
        Settings settings = getSettings();

        TransportClient transportClient = new TransportClient(settings);
        Client client = transportClient.addTransportAddress(new InetSocketTransportAddress("172.16.12.201", 9301));
        SearchResponse searchResponse = client.prepareSearch("sourcefile")
                .setSearchType(SearchType.QUERY_AND_FETCH)
                .setQuery(QueryBuilders.termQuery("fileName", file))
                .setFrom(0).setSize(1).setExplain(true)
                .execute()
                .actionGet();
        SearchHit[] results = searchResponse.getHits().getHits();
        client.close();
        transportClient.close();
        for (SearchHit hit : results) {
            Map<String, Object> result = hit.getSource();
            //Few files have /r as EOL which results in illegal EOL Exception.
            return result.get("fileContent").toString().replaceAll("\r", "\n");
        }
        return "Could not load File now...Please try again";
    }

    private Settings getSettings() {
        return ImmutableSettings.settingsBuilder()
                .put("path.conf", "/home/prudhvib/Downloads/elasticsearch-1.4.1.jar/config/names.txt")
                .put("cluster.name", "betterdocs")
                .classLoader(Settings.class.getClassLoader())
                .build();
    }

    public void setProject(Project project) {
        this.project = project;
    }


    private SearchHit[] getQueryResults(Set<String> output) {
        Settings settings = getSettings();

        TransportClient transportClient = new TransportClient(settings);
        Client client = transportClient.addTransportAddress(new InetSocketTransportAddress("172.16.12.201", 9301));
        SearchResponse searchResponse = client.prepareSearch("betterdocs")
                .setSearchType(SearchType.QUERY_AND_FETCH)
                .setQuery(getBoolQueryBuilder(output))
                .addSort("score", SortOrder.ASC)
                .setFrom(0).setSize(10).setExplain(true)
                .execute()
                .actionGet();
        SearchHit[] results = searchResponse.getHits().getHits();

        return results;
    }

    private BoolQueryBuilder getBoolQueryBuilder(Set<String> importsInLinesSet) {
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        Iterator importsIterator = importsInLinesSet.iterator();
        while (importsIterator.hasNext()) {
            boolQueryBuilder = setMustToken(boolQueryBuilder, importsIterator.next().toString());
        }
        return boolQueryBuilder;
    }

    private BoolQueryBuilder setMustToken(BoolQueryBuilder boolQueryBuilder, String searchItem) {
        return boolQueryBuilder.must(QueryBuilders.termQuery("custom.tokens.importName", searchItem));
    }


    private Set<String> importsInLines(Set<String> linesSet, Set<String> importSet) {
        Iterator iterator = linesSet.iterator();
        Set<String> importsInLinesSet = new HashSet<String>();
        while (iterator.hasNext()) {
            String line = iterator.next().toString();
            Iterator importsIterator = importSet.iterator();
            while (importsIterator.hasNext()) {
                String nextImport = importsIterator.next().toString();
                if (line.contains(nextImport.substring(nextImport.lastIndexOf(".") + 1))) {
                    importsInLinesSet.add(nextImport);
                }
            }
        }
        return importsInLinesSet;
    }

    private Set<String> getLinesAsSet(Document document, int startLine, int endLine) {
        Set<String> linesSet = new HashSet<String>();
        for (int i = startLine; i <= endLine; i++) {
            String line = document.getCharsSequence().subSequence(document.getLineStartOffset(i), document.getLineEndOffset(i)).toString();
            if (!line.contains("import ")) {
                linesSet.add(line);
            }
        }
        return linesSet;
    }

    private Set<String> getImportsAsSet(Document document) {
        int startLine = 0;
        int endLine = document.getLineCount() - 1;
        Set<String> importsSet = new HashSet<String>();
        for (int i = startLine; i <= endLine; i++) {
            String line = document.getCharsSequence().subSequence(document.getLineStartOffset(i), document.getLineEndOffset(i) + document.getLineSeparatorLength(i)).toString();
            if (line.contains("import ") && !line.contains("*")) {
                importsSet.add(line.replace("import", "").replace(";", "").trim());
            }
        }
        return importsSet;
    }


    private String getLocalPackages(Editor editor, Project project) {
        PsiPackage pack = JavaPsiFacade.getInstance(project).findPackage("");
        PsiPackage[] subPackages = pack.getSubPackages();

        int length = subPackages.length;
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < length; i++) {
            sb.append(subPackages[i].toString() + "\n");
        }
        return sb.toString();
    }
}

