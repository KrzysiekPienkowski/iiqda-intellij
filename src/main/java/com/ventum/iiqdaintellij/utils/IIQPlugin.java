package com.ventum.iiqdaintellij.utils;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * The activator class controls the plug-in life cycle
 * The IIQ-specific pieces have been abstracted so that on lazy initialisation
 * They will only be instantiated if we have specified the location of identityiq.jar
 */

public class IIQPlugin {

    public static List<String> getTargetEnvironments(Project project) {
        List<String> fileNameList = new ArrayList<>();
        File baseDir = new File(project.getBasePath());
        addFileNames(baseDir, fileNameList);
        return fileNameList;
    }

    private static void addFileNames(File dir, List<String> fileNameList) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                String name = file.getName();
                if (name.endsWith(IIQDAConstants.TARGET_SUFFIX) && !name.equals("reverse.target.properties")) {
                    String target = name.substring(0, name.indexOf(IIQDAConstants.TARGET_SUFFIX));
                    fileNameList.add(target);

                }
            }
        }
    }

    public static VirtualFile getSelectedFile(AnActionEvent event) {
        Editor editor = event.getData(PlatformDataKeys.EDITOR);
        VirtualFile file = null;
        if (editor != null) {
            // Get the currently opened file from the editor
            file = FileEditorManager.getInstance(event.getProject()).getSelectedEditor().getFile();
        }
        return file;
    }
}
