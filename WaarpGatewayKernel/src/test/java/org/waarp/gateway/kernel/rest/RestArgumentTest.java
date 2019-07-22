package org.waarp.gateway.kernel.rest;

import static org.junit.Assert.*;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

public class RestArgumentTest {

    @Test
    public void testAddFilterNPE() {
        RestArgument ra = new RestArgument(null);
        ra.addFilter(null);

        assertEquals("filters should be an empty ObjectNode",
            new ObjectNode(JsonNodeFactory.instance), ra.getFilter());
    }
}
