package org.apache.fop.events.model;

import java.io.Serializable;

import org.apache.xmlgraphics.util.XMLizable;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Represents an event parameter.
 */
public class Parameter implements Serializable, XMLizable {

    private static final long serialVersionUID = 6062500277953887099L;

    private final Class type;
    private final String name;

    /**
     * Creates a new event parameter.
     *
     * @param type
     *            the parameter type
     * @param name
     *            the parameter name
     */
    public Parameter(final Class type, final String name) {
        this.type = type;
        this.name = name;
    }

    /**
     * Returns the parameter type.
     *
     * @return the parameter type
     */
    public Class getType() {
        return this.type;
    }

    /**
     * Returns the parameter name.
     *
     * @return the parameter name
     */
    public String getName() {
        return this.name;
    }

    /** {@inheritDoc} */
    @Override
    public void toSAX(final ContentHandler handler) throws SAXException {
        final AttributesImpl atts = new AttributesImpl();
        atts.addAttribute("", "type", "type", "CDATA", getType().getName());
        atts.addAttribute("", "name", "name", "CDATA", getName());
        final String elName = "parameter";
        handler.startElement("", elName, elName, atts);
        handler.endElement("", elName, elName);
    }

}