package com.ventum.iiqdaintellij.actions;

import com.intellij.diff.DiffContentFactory;
import com.intellij.diff.DiffManager;
import com.intellij.diff.contents.DocumentContent;
import com.intellij.diff.requests.SimpleDiffRequest;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.vfs.VirtualFile;
import com.ventum.iiqdaintellij.Exceptions.ConnectionException;
import com.ventum.iiqdaintellij.utils.IIQRESTClient;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class CompareAction extends AnAction {

    private String selectedObjectType;
    private String selectedObjectName;
    private AnActionEvent event;

    public CompareAction() {
    }

    public CompareAction(String target) {
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

        VirtualFile[] selectedFiles = FileEditorManager.getInstance(event.getProject()).getSelectedFiles();
        if (selectedFiles.length == 1) {
            VirtualFile currentFile = selectedFiles[0];
            Document document = FileDocumentManager.getInstance().getDocument(currentFile);
            if (document != null) {
                // Get the text content of the document
                String content = document.getText();
                String fileName = currentFile.getName();
                String[] fileNameParts = splitFileName(fileName);
                this.selectedObjectType = fileNameParts[0];
                this.selectedObjectName = fileNameParts[1];
                System.out.println("Type: " + selectedObjectType);
                System.out.println("Name: " + selectedObjectName);

                String selectedObject;
                try {
                    selectedObject = iiqrestClient.getObject(selectedObjectType, selectedObjectName);
                } catch (ConnectionException ex) {
                    throw new RuntimeException(ex);
                }

                // Create a DiffContent for the current file's content
                DiffContentFactory contentFactory = DiffContentFactory.getInstance();
                DocumentContent documentContent = contentFactory.createEditable(event.getProject(), content, FileType.EMPTY_ARRAY[0]);

                // Create a DiffContent for the string to compare with
                DocumentContent externalContent = contentFactory.create(event.getProject(), selectedObject);

                // Create a SimpleDiffRequest for the comparison
                SimpleDiffRequest request = new SimpleDiffRequest("Comparison", documentContent, externalContent, "Current File", "Comparison String");

                // Show the comparison in the DiffView
                DiffManager.getInstance().showDiff(event.getProject(), request);
            }
        } else {
            System.out.println("No file selected");
        }
    }


    private String[] splitFileName(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex != -1) {
            fileName = fileName.substring(0, dotIndex);
        }
        return fileName.split("-");
    }


}

