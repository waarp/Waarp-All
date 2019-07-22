/**
 * This file is part of Waarp Project.
 * 
 * Copyright 2009, Frederic Bregier, and individual contributors by the @author tags. See the
 * COPYRIGHT.txt in the distribution for a full listing of individual contributors.
 * 
 * All Waarp Project is free software: you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 * 
 * Waarp is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with Waarp . If not, see
 * <http://www.gnu.org/licenses/>.
 */
package org.waarp.openr66.serveraction;

import java.io.File;

import javax.swing.filechooser.FileFilter;

/**
 * File extension filter
 * 
 * @author "Frederic Bregier"
 * 
 */
public class FileExtensionFilter extends FileFilter {
    private String description = "All Files";
    private String extension = "";

    public FileExtensionFilter(String extension, String description) {
        this.description = description;
        this.extension = extension;
    }

    @Override
    public boolean accept(File f) {
        if (f.isDirectory()) {
            return true;
        }
        String extension = FileExtensionFilter.getExtension(f);
        if (extension != null) {
            if (this.extension.equalsIgnoreCase(extension)) {
                return true;
            } else {
                return false;
            }
        }
        return false;
    }

    // Description du filtre
    public String getDescription() {
        return description;
    }

    /**
     * Get extension file
     * 
     * @param f
     * @return the extension
     */
    public static String getExtension(File f) {
        String ext = null;
        String s = f.getName();
        int i = s.lastIndexOf('.');
        if (i > 0 && i < s.length() - 1) {
            ext = s.substring(i + 1).toLowerCase();
        }
        return ext;
    }
}
