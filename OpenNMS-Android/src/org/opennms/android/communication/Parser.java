package org.opennms.android.communication;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;

public class Parser {

    static XPath xPath = XPathFactory.newInstance().newXPath();

    public static NodeList getXmlNodeListForExpression(String expression, String xml) throws XPathExpressionException {
        InputSource inputSource = new InputSource(new StringReader(xml));
        return (NodeList) xPath.evaluate(expression, inputSource, XPathConstants.NODESET);
    }

    public static Node getXmlNodeForExpression(String expression, Node widgetNode) throws XPathExpressionException {
        return (Node) xPath.evaluate(expression, widgetNode, XPathConstants.NODE);
    }

}
