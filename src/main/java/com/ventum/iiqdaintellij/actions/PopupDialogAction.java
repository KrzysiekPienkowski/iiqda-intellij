package com.ventum.iiqdaintellij.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.ventum.iiqdaintellij.utils.IIQPlugin;
import com.ventum.iiqdaintellij.utils.IIQRESTClient;
import groovyjarjarantlr4.v4.runtime.misc.NotNull;

import java.io.IOException;

public class PopupDialogAction extends AnAction {


    public PopupDialogAction(String target) {
        super(target);
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
        // Using the event, evaluate the context,
        // and enable or disable the action.
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        IIQRESTClient client;
        try {
            client = new IIQRESTClient(event.getProject(), this.getTemplateText());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        IIQPlugin.getSelectedFile(event);
    }


    // Override getActionUpdateThread() when you target 2022.3 or later!

}
