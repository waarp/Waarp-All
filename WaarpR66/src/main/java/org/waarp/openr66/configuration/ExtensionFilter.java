package org.waarp.openr66.configuration;

import java.io.File;
import java.io.FilenameFilter;

/**
 * Extension Filter based on extension
 * 
 * @author Frederic Bregier
 * 
 */
public class ExtensionFilter implements FilenameFilter {
    String filter;

    public ExtensionFilter(String filter) {
        this.filter = filter;
    }

    public boolean accept(File arg0, String arg1) {
        return arg1.endsWith(filter);
    }

}
