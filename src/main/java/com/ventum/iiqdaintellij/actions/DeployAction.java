package com.ventum.iiqdaintellij.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.ventum.iiqdaintellij.utils.IIQPlugin;
import com.ventum.iiqdaintellij.utils.IIQRESTClient;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class DeployAction extends AnAction {

    private AnActionEvent event;

    public DeployAction() {
    }

    public DeployAction(String target) {
        super(target);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {

        this.event = e;
        IIQRESTClient iiqrestClient;
        try {
            iiqrestClient = new IIQRESTClient(this.event.getProject(), this.getTemplateText());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        VirtualFile currentFile = IIQPlugin.getSelectedFile(event);
            Document document = FileDocumentManager.getInstance().getDocument(currentFile);
            if (document != null) {
                // Get the text content of the document
                String content = document.getText();
                iiqrestClient.sendFile(content);
            }
    }
}

