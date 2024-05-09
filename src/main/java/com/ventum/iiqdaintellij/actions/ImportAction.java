package com.ventum.iiqdaintellij.actions;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;

public class ImportAction extends AnAction
{
    @Override
    public void actionPerformed(AnActionEvent e)
    {
        BrowserUtil.browse("https://stackoverflow.com/questions/ask");
    }
}