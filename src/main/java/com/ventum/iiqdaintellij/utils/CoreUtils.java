package com.ventum.iiqdaintellij.utils;

import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

public class CoreUtils {

    private static String[] attributesToClean = new String[]{"id", "created", "modified"};

    public static VirtualFile getVirtualFileFromPath(String filePath) {
        LocalFileSystem localFileSystem = LocalFileSystem.getInstance();
        return localFileSystem.findFileByPath(filePath);
    }

    public static String clean(String object) {
        List<String> components = new ArrayList<String>();
        for (String property : attributesToClean) {
            components.add(String.format("(?:\\b%s=\"[^\"]+\")", property));
        }
        Pattern compiledCleanPattern = Pattern.compile(CoreUtils.join(components, "|"));
        return compiledCleanPattern.matcher(object).replaceAll("");
    }

    private static String join(Collection<String> c, String delimiter) {
        if (null == c)
            return null;

        StringBuffer buf = new StringBuffer();
        Iterator<String> iter = c.iterator();
        while (iter.hasNext()) {
            buf.append(iter.next());
            if (iter.hasNext())
                buf.append(delimiter);
        }
        return buf.toString();

    }

    public static @NotNull String addCDATA(String cleanedObject) {
        cleanedObject = cleanedObject.replaceAll("<Source>", "<Source><![CDATA[");
        cleanedObject = cleanedObject.replaceAll("</Source>", "]]></Source>");
        return cleanedObject;
    }
}
