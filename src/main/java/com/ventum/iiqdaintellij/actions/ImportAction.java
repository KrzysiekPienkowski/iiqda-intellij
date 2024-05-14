package com.ventum.iiqdaintellij.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.ui.CollectionComboBoxModel;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.ventum.iiqdaintellij.Exceptions.ConnectionException;
import com.ventum.iiqdaintellij.utils.CoreUtils;
import com.ventum.iiqdaintellij.utils.IIQRESTClient;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class ImportAction extends AnAction {

    private String selectedObjectType;
    private String selectedObjectName;
    private AnActionEvent event;


    public ImportAction() {
    }

    public ImportAction(String target) {
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
        // Define items for the dropdown list
        List<String> items = null;
        try {
            items = iiqrestClient.getObjectTypes();
        } catch (ConnectionException ex) {
            throw new RuntimeException(ex);
        }

        // Create a ComboBox with the defined items
        ComboBox<String> comboBoxObjectTypes = new ComboBox<>(new CollectionComboBoxModel<>(items));
        JList<String> dataListJList = new JBList<>();
        JScrollPane scrollPane = new JBScrollPane(dataListJList);
        scrollPane.setPreferredSize(new Dimension(500, 500));
        JButton button = new JButton("Finish");
        button.setEnabled(false);

        // Create a panel to contain the ComboBox
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(comboBoxObjectTypes, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(button, BorderLayout.SOUTH);

        // Show the ComboBox in a dialog
        JFrame frame = new JFrame("Importing from: " + this.getTemplateText());
        frame.getContentPane().add(panel);
        frame.setSize(550, 800);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        comboBoxObjectTypes.addActionListener(actionEvent -> {
            selectedObjectType = (String) comboBoxObjectTypes.getSelectedItem();
            if (selectedObjectType != null) {
                System.out.println("Selected item: " + selectedObjectType);
                List<String> objectNames;
                try {
                    objectNames = iiqrestClient.getObjects(selectedObjectType);
                    dataListJList.setModel(getModelFromList(objectNames));

                } catch (ConnectionException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });

        dataListJList.addListSelectionListener(actionEvent -> {
            selectedObjectName = dataListJList.getSelectedValue();
            if (selectedObjectName != null) {
                button.setEnabled(true);
            }
        });


        button.addActionListener(actionEvent -> {
            selectedObjectName = dataListJList.getSelectedValue();
            if (selectedObjectName != null) {
                String selectedObject;
                try {
                    selectedObject = iiqrestClient.getObject(selectedObjectType, selectedObjectName);
                } catch (ConnectionException ex) {
                    throw new RuntimeException(ex);
                }
                String cleanedObject = CoreUtils.clean(selectedObject);
                cleanedObject = CoreUtils.addCDATA(cleanedObject);
                String savedFilePath = saveStringToFile(cleanedObject);
                VirtualFileManager.getInstance().syncRefresh();
                FileEditorManager.getInstance(event.getProject()).openFile(CoreUtils.getVirtualFileFromPath(savedFilePath), true);
                frame.dispose();
            } else {
                JOptionPane.showMessageDialog(panel, "No value selected.");
            }
        });
    }

    private String saveStringToFile(String fileContent) {
        DataContext dataContext = event.getDataContext();
        VirtualFile virtualFile = CommonDataKeys.VIRTUAL_FILE.getData(dataContext);
        String currentDirectory = virtualFile.getPath();
        String filePath = saveFile(fileContent, currentDirectory);
        return filePath;
    }

    private String saveFile(String fileContent, String currentDirectory) {
        String fileName = "/" + selectedObjectType + "-" + selectedObjectName + ".xml";
        String filePath = currentDirectory + fileName;
        try {
            FileWriter writer = new FileWriter(filePath);
            writer.write(fileContent);
            writer.close();
            System.out.println("Content has been written to the file.");
        } catch (IOException e) {
            e.printStackTrace();
        }

        return filePath;
    }

    private DefaultListModel getModelFromList(List list) {
        DefaultListModel<String> listModel = new DefaultListModel<>();
        listModel.addAll(list);
        return listModel;
    }
}

