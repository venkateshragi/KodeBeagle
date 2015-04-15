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
import com.intellij.openapi.ui.Messages;
import javax.swing.JSplitPane;

public class EditorToggleAction extends AnAction {
    private JSplitPane jSplitPane;

    public void setjSplitPane(JSplitPane jSplitPane) {
        this.jSplitPane = jSplitPane;
    }

    public EditorToggleAction() {
        super("Move", "Move", Messages.getQuestionIcon());
    }

    @Override
    public void actionPerformed(AnActionEvent anActionEvent) {
        double location = (double) jSplitPane.getDividerLocation();
        if (location / jSplitPane.getWidth() < 0.5) {
            jSplitPane.setDividerLocation(0.3);
        } else {
            jSplitPane.setDividerLocation(0.08);
        }
    }
}
