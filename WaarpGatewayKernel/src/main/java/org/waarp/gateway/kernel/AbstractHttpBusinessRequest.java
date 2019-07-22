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
package org.waarp.gateway.kernel;

import java.util.LinkedHashMap;

/**
 * @author Frederic Bregier
 * 
 */
public abstract class AbstractHttpBusinessRequest {
    protected LinkedHashMap<String, AbstractHttpField> fields;
    protected HttpPage page;

    /**
     * Default constructor
     * 
     * @param fields
     * @param page
     */
    public AbstractHttpBusinessRequest(LinkedHashMap<String, AbstractHttpField> fields,
            HttpPage page) {
        this.fields = fields;
        this.page = page;
    }

    /**
     * 
     * @return the LinkedHashMap<String, AbstractHttpField> associated with the current request
     */
    public LinkedHashMap<String, AbstractHttpField> getLinkedHashMapHttpFields() {
        return fields;
    }

    /**
     * 
     * @return the HTML header or null if set through definition
     */
    public abstract String getHeader();

    /**
     * 
     * @return the HTML Footer or null if set through definition
     */
    public abstract String getFooter();

    /**
     * 
     * @return True if the HTML output will be a Form
     */
    public abstract boolean isForm();

    /**
     * 
     * @return the HTML Begin of Form (including URI) or null if set through definition
     */
    public abstract String getBeginForm();

    /**
     * 
     * @return the HTML End of Form or null if set through definition
     */
    public abstract String getEndForm();

    /**
     * 
     * @param field
     * @return the HTML field form or null if set standard
     */
    public abstract String getFieldForm(AbstractHttpField field);

    /**
     * 
     * @return the HTML Next Field in Form or null if set through definition
     */
    public abstract String getNextFieldInForm();

    /**
     * Called if fieldtovalidate is true
     * 
     * @param field
     * @return True if the field is valid
     */
    public abstract boolean isFieldValid(AbstractHttpField field);

    /**
     * Note that mandatory fields is tested outside of the AbstractHttpBusinessRequest
     * 
     * @return True if according to known informations this request is valid
     */
    public abstract boolean isRequestValid();

    /**
     * 
     * @return the string that contains the ContentType in HTML format (as "text/html")
     */
    public abstract String getContentType();

    /**
     * Utility mainly used for PUT where the content is coming out of the decoder
     * 
     * @return the main FileUpload as AbstractHttpField for the current request
     */
    public abstract AbstractHttpField getMainFileUpload();

    /**
     * Used at the end of the request to release the resources
     */
    public void cleanRequest() {
        for (AbstractHttpField field : fields.values()) {
            field.clean();
        }
        fields.clear();
    }
}
