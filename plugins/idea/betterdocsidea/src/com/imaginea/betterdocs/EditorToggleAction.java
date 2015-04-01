package com.imaginea.betterdocs;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.Messages;

import javax.swing.JSplitPane;

/**
 * Created by prudhvib on 1/4/15.
 */
public class EditorToggleAction extends AnAction{
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
        if(location / jSplitPane.getWidth() < 0.5) {
            jSplitPane.setDividerLocation(0.3);
        } else {
            jSplitPane.setDividerLocation(0.08);
        }
    }
}

