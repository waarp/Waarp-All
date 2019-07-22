package org.waarp.openr66.dao.xml;

import javax.xml.namespace.QName;
import javax.xml.xpath.XPathVariableResolver;
import java.util.HashMap;
import java.util.Map;

public class SimpleVariableResolver implements XPathVariableResolver {

    private static final Map<QName, Object> vars = new HashMap<QName, Object>();

    public void addVariable(QName name, Object value) {
        vars.put(name, value);
    }

    public Object resolveVariable(QName name) {
        return vars.get(name);
    }
}