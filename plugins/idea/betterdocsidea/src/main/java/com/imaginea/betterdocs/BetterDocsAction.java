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

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jetbrains.annotations.NotNull;

public class BetterDocsAction extends AnAction {
    private static final String IMPORT_NAME = "importName";
    private static final String LINE_NUMBERS = "lineNumbers";
    private static final String BETTER_DOCS = "BetterDocs";
    private static final String HITS = "hits";
    private static final String SOURCE = "_source";
    private static final String FILE_CONTENT = "fileContent";
    private static final String IMPORT = "import ";
    private static final String SORT_ORDER = "desc";
    private static final String APPLICATION_JSON = "application/json";
    private static final String FILE = "file";
    private static final String TOKENS = "tokens";
    private static final String CUSTOM_TOKENS_IMPORT_NAME = "custom.tokens.importName";
    private static final char CH = '.';
    private static final String EASTIC_SEARCH_SOURCE_FILE_URL = "http://172.16.12.201:9201/sourcefile/_search";
    private static final String ELASTIC_SEARCH_BETTERDOCS_URL = "http://172.16.12.201:9201/betterdocs/_search";

    private Project project;
    private JTree jTree;
    private Editor windowEditor;
    private Editor projectEditor;

    public void setProjectEditor(Editor projectEditor) {
        this.projectEditor = projectEditor;
    }

    public void setWindowEditor(Editor windowEditor) {
        this.windowEditor = windowEditor;
    }

    public void setTree(JTree jTree) {
        this.jTree = jTree;
    }

    public BetterDocsAction() {
        super(BETTER_DOCS, BETTER_DOCS, Messages.getInformationIcon());
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

    private static MutableTreeNode getNodes(String projectName, Iterable<CodeInfo> codeInfoCollection) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(projectName);
        Collection<String> fileNameSet = new HashSet<String>();
        for (CodeInfo codeInfo : codeInfoCollection) {
            if (!fileNameSet.contains(codeInfo.getFileName())) {
                node.add(new DefaultMutableTreeNode(codeInfo));
                fileNameSet.add(codeInfo.getFileName());
            }
        }
        return node;
    }

    public void runAction(final AnActionEvent e) throws IOException {
        final Editor projectEditor = DataKeys.EDITOR.getData(e.getDataContext());

        if (projectEditor != null) {
            setProjectEditor(projectEditor);

            Set<String> imports = getImports(projectEditor.getDocument());
            Set<String> lines = getLines(projectEditor, projectEditor.getDocument());
            Set<String> importsInLines = importsInLines(lines, imports);

            DefaultTreeModel model = (DefaultTreeModel) jTree.getModel();
            DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
            root.removeAllChildren();

            if (!importsInLines.isEmpty()) {
                jTree.setVisible(true);

                String esQueryJson = getESQueryJson(importsInLines);
                String esResultJson = getESResultJson(esQueryJson, ELASTIC_SEARCH_BETTERDOCS_URL);
                Map<String, String> fileTokensMap = getFileTokens(esResultJson);

                Map<String, ArrayList<CodeInfo>> projectNodes = new HashMap<String, ArrayList<CodeInfo>>();
                updateProjectNodes(imports, fileTokensMap, projectNodes);
                updateRoot(root, projectNodes);
                model.reload(root);
                jTree.addTreeSelectionListener(getTreeSelectionListener(root));
            } else {
                jTree.updateUI();
            }
        }
    }

    private Editor getEditor(AnActionEvent e) {
        final Editor projectEditor = DataKeys.EDITOR.getData(e.getDataContext());
        return projectEditor;
    }

    private void updateProjectNodes(Collection<String> imports, Map<String, String> fileTokensMap, Map<String, ArrayList<CodeInfo>> projectNodes) {
        for (Map.Entry<String, String> entry : fileTokensMap.entrySet()) {
            String fileName = entry.getKey();
            String tokens = entry.getValue();

            ArrayList<Integer> lineNumbers = getLineNumbers(imports, tokens);
            String contents = getContentsForFile(fileName);
            CodeInfo codeInfo = new CodeInfo(fileName, lineNumbers, contents);

            String projectName = fileName.substring(0, fileName.indexOf('/', fileName.indexOf('/') + 1));
            if (projectNodes.containsKey(projectName)) {
                projectNodes.get(projectName).add(codeInfo);
            } else {
                projectNodes.put(projectName, new ArrayList<CodeInfo>(Collections.singletonList(codeInfo)));
            }
        }
    }

    private static DefaultMutableTreeNode updateRoot(DefaultMutableTreeNode root, Map<String, ArrayList<CodeInfo>> projectNodes) {
        for (Map.Entry<String, ArrayList<CodeInfo>> entry : projectNodes.entrySet()) {
            root.add(getNodes(entry.getKey(), entry.getValue()));
        }
        return root;
    }

    private TreeSelectionListener getTreeSelectionListener(final TreeNode root) {
        return new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent treeSelectionEvent) {
                DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode)
                        jTree.getLastSelectedPathComponent();

                if (selectedNode == null) {

                } else if (selectedNode.isLeaf() && root.getChildCount() > 0) {
                    final CodeInfo codeInfo = (CodeInfo) selectedNode.getUserObject();
                    final Document windowEditorDocument = windowEditor.getDocument();

                    writeToDocument(codeInfo, windowEditorDocument);

                    final List<Integer> linesForFolding = codeInfo.getLineNumbers();
                    linesForFolding.add(windowEditorDocument.getLineCount() + 1);
                    java.util.Collections.sort(linesForFolding);
                    addFoldings(windowEditorDocument, linesForFolding);
                }
            }
        };
    }

    private void writeToDocument(final CodeInfo codeInfo, final Document windowEditorDocument) {
        new WriteCommandAction(project) {
            @Override
            protected void run(@NotNull Result result) throws Throwable {
                windowEditorDocument.setReadOnly(false);
                windowEditorDocument.setText(codeInfo.getContents());
                windowEditorDocument.setReadOnly(true);
            }
        }.execute();

    }

    private void addFoldings(final Document windowEditorDocument, final Iterable<Integer> linesForFolding) {
        windowEditor.getFoldingModel().runBatchFoldingOperation(new Runnable() {
            @Override
            public void run() {
                int prevLine = 0;
                for (int line : linesForFolding) {
                    int currentLine = line - 1;
                    if (prevLine < windowEditorDocument.getLineCount()) {

                        int startOffset = windowEditorDocument.getLineStartOffset(prevLine);
                        int endOffset = windowEditorDocument.getLineEndOffset(currentLine - 1);

                        if (startOffset < endOffset) {
                            try {
                                windowEditor.getFoldingModel()
                                        .addFoldRegion(startOffset, endOffset, "...")
                                        .setExpanded(false);
                            } catch (NullPointerException e) {
                                //e.printStackTrace();
                            }
                        }
                        prevLine = currentLine + 1;
                    }
                }
            }
        });
    }

    private ArrayList<Integer> getLineNumbers(Collection<String> imports, String tokens) {
        ArrayList<Integer> lineNumbers = new ArrayList<Integer>();
        JsonReader reader = new JsonReader(new StringReader(tokens));
        reader.setLenient(true);
        JsonArray tokensArray = new JsonParser().parse(reader).getAsJsonArray();

        for (JsonElement token : tokensArray) {
            JsonObject jObject = token.getAsJsonObject();
            String importName = jObject.getAsJsonPrimitive(IMPORT_NAME).getAsString();
            if (imports.contains(importName)) {
                JsonArray lineNumbersArray = jObject.getAsJsonArray(LINE_NUMBERS);
                for (JsonElement lineNumber : lineNumbersArray) {
                    lineNumbers.add(lineNumber.getAsInt());
                }
            }
        }
        return lineNumbers;
    }


    private String getContentsForFile(String file) {
        String esFileQueryJson = getJsonForFileContent(file);
        String esFileResultJson = getESResultJson(esFileQueryJson, EASTIC_SEARCH_SOURCE_FILE_URL);

        JsonReader reader = new JsonReader(new StringReader(esFileResultJson));
        reader.setLenient(true);
        JsonElement jsonElement = new JsonParser().parse(reader);
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        JsonObject hitsObject = jsonObject.getAsJsonObject(HITS);
        JsonArray hitsArray = hitsObject.getAsJsonArray(HITS);
        JsonObject hitObject = hitsArray.get(0).getAsJsonObject();
        JsonObject sourceObject = hitObject.getAsJsonObject(SOURCE);
        String fileContent = sourceObject.getAsJsonPrimitive(FILE_CONTENT).getAsString();
        return fileContent;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    private static Set<String> importsInLines(Iterable<String> lines, Iterable<String> imports) {
        Set<String> importsInLines = new HashSet<String>();

        for (String line : lines) {
            for (String nextImport : imports) {
                if (line.contains(nextImport.substring(nextImport.lastIndexOf(CH) + 1))) {
                    importsInLines.add(nextImport);
                }
            }
        }
        return importsInLines;
    }

    private static Set<String> getLines(Editor projectEditor, Document document) {
        Set<String> lines = new HashSet<String>();
        int startLine = 0;
        int endLine = document.getLineCount() - 1;

        for (int i = startLine; i <= endLine; i++) {
            String line = document.getCharsSequence().subSequence(document.getLineStartOffset(i), document.getLineEndOffset(i)).toString();
            if (!line.contains(IMPORT)) {
                lines.add(line);
            }
        }
        return lines;
    }

    private static Set<String> getImports(Document document) {
        int startLine = 0;
        int endLine = document.getLineCount() - 1;
        Set<String> imports = new HashSet<String>();
        for (int i = startLine; i <= endLine; i++) {
            String line = document.getCharsSequence().subSequence(document.getLineStartOffset(i), document.getLineEndOffset(i) + document.getLineSeparatorLength(i)).toString();
            if (line.contains(IMPORT) && !line.contains("*")) {
                imports.add(line.replace(IMPORT, "").replace(";", "").trim());
            }
        }
        return imports;
    }

    private String getESQueryJson(Set<String> importsInLines) {
        ESQuery esQuery = new ESQuery();
        ESQuery.Query query = new ESQuery.Query();
        esQuery.setQuery(query);
        esQuery.setFrom(0);
        esQuery.setSize(3);

        List<ESQuery.Sort> sortList = new ArrayList<ESQuery.Sort>();

        ESQuery.Sort sort = new ESQuery.Sort();

        ESQuery.Score score = new ESQuery.Score();
        score.setOrder(SORT_ORDER);

        sort.setScore(score);

        sortList.add(sort);
        esQuery.setSort(sortList);

        ESQuery.Bool bool = new ESQuery.Bool();
        query.setBool(bool);

        List<ESQuery.Must> mustList = new ArrayList<ESQuery.Must>();

        ESQuery.Must must;
        ESQuery.Term term;

        for (String nextImport : importsInLines) {
            must = new ESQuery.Must();
            bool.setMust(mustList);
            bool.setMustNot(new ArrayList<ESQuery.Must>());
            bool.setShould(new ArrayList<ESQuery.Must>());
            term = new ESQuery.Term();
            must.setTerm(term);
            term.setImportName(nextImport);
            mustList.add(must);
        }

        Gson gson = new Gson();
        return gson.toJson(esQuery).replaceAll(IMPORT_NAME, CUSTOM_TOKENS_IMPORT_NAME);
    }


    private static String getESResultJson(String esQueryJson, String url) {
        StringBuilder stringBuilder = new StringBuilder();
        try {
            HttpClient httpClient = new DefaultHttpClient();
            HttpPost postRequest = new HttpPost(url);
            StringEntity input = new StringEntity(esQueryJson);
            input.setContentType(APPLICATION_JSON);
            postRequest.setEntity(input);
            HttpResponse response = httpClient.execute(postRequest);
            if (response.getStatusLine().getStatusCode() != 200) {
                throw new RuntimeException("Failed : HTTP error code : " +
                        response.getStatusLine().getStatusCode());
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(
                    (response.getEntity().getContent())));
            String output;
            while ((output = br.readLine()) != null) {
                stringBuilder.append(output);
            }
            httpClient.getConnectionManager().shutdown();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return stringBuilder.toString();
    }

    private static Map<String, String> getFileTokens(String esResultJson) {
        Map<String, String> fileTokenMap = new HashMap<String, String>();
        JsonReader reader = new JsonReader(new StringReader(esResultJson));
        reader.setLenient(true);
        JsonElement jsonElement = new JsonParser().parse(reader);
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        JsonObject hitsObject = jsonObject.getAsJsonObject(HITS);
        JsonArray hitsArray = hitsObject.getAsJsonArray(HITS);

        for (JsonElement hits : hitsArray) {
            JsonObject hitObject = hits.getAsJsonObject();
            JsonObject sourceObject = hitObject.getAsJsonObject(SOURCE);
            String fileName = sourceObject.getAsJsonPrimitive(FILE).getAsString();
            String tokens = sourceObject.get(TOKENS).toString();
            fileTokenMap.put(fileName, tokens);
        }
        return fileTokenMap;
    }

    private static String getJsonForFileContent(String fileName) {
        ESFileContent esFileContent = new ESFileContent();
        ESFileContent.Query query = new ESFileContent.Query();
        esFileContent.setQuery(query);
        ESFileContent.Term term = new ESFileContent.Term();
        query.setTerm(term);
        term.setFileName(fileName);
        Gson gson = new Gson();
        return gson.toJson(esFileContent);
    }
}
