package net.filemaid.util;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.xml.namespace.QName;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public final class XPathUtilities {
    public static Node selectNode(String string, Object object) {
        return (Node)XPathUtilities.evaluateXPath(string, object, XPathConstants.NODE);
    }

    public static String selectString(String string, Object object) {
        return ((String)XPathUtilities.evaluateXPath(string, object, XPathConstants.STRING)).trim();
    }

    public static Stream<Node> streamNodes(String string, Object object) {
        return XPathUtilities.stream((NodeList)XPathUtilities.evaluateXPath(string, object, XPathConstants.NODESET));
    }

    public static Node[] selectNodes(String string, Object object) {
        return (Node[])XPathUtilities.streamNodes(string, object).toArray(Node[]::new);
    }

    public static List<String> selectStrings(String string, Object object) {
        ArrayList<String> arrayList = new ArrayList<String>();
        for (Node node : XPathUtilities.selectNodes(string, object)) {
            String string2 = XPathUtilities.getTextContent(node);
            if (string2.length() <= 0) continue;
            arrayList.add(string2);
        }
        return arrayList;
    }

    public static Node getChild(String string, Node node2) {
        if (node2 == null) {
            return null;
        }
        return XPathUtilities.stream(node2.getChildNodes()).filter(node -> string.equals(node.getNodeName())).findFirst().orElse(null);
    }

    public static Node[] getChildren(String string, Node node2) {
        if (node2 == null) {
            return new Node[0];
        }
        return (Node[])XPathUtilities.stream(node2.getChildNodes()).filter(node -> string.equals(node.getNodeName())).toArray(Node[]::new);
    }

    public static String getAttribute(String string, Node node) {
        Node node2;
        if (node != null && (node2 = node.getAttributes().getNamedItem(string)) != null) {
            return node2.getNodeValue().trim();
        }
        return null;
    }

    public static String getTextContent(String string, Node node) {
        Node node2 = XPathUtilities.getChild(string, node);
        if (node2 == null) {
            return null;
        }
        return XPathUtilities.getTextContent(node2);
    }

    public static String getTextContent(Node node) {
        StringBuilder stringBuilder = new StringBuilder();
        for (Node node2 : XPathUtilities.getChildren("#text", node)) {
            stringBuilder.append(node2.getNodeValue());
        }
        return stringBuilder.toString().trim();
    }

    public static List<String> getListContent(String string, String string2, Node node) {
        ArrayList<String> arrayList = new ArrayList<String>();
        for (Node node2 : XPathUtilities.getChildren(string, node)) {
            String string3 = XPathUtilities.getTextContent(node2);
            if (string3 == null || string3.length() <= 0) continue;
            if (string2 == null) {
                arrayList.add(string3);
                continue;
            }
            for (String string4 : string3.split(string2)) {
                if ((string4 = string4.trim()).length() <= 0) continue;
                arrayList.add(string4);
            }
        }
        return arrayList;
    }

    public static Double getDecimal(String string) {
        try {
            return Double.parseDouble(string);
        }
        catch (NullPointerException | NumberFormatException runtimeException) {
            return null;
        }
    }

    public static Object evaluateXPath(String string, Object object, QName qName) {
        try {
            return XPathFactory.newInstance().newXPath().compile(string).evaluate(object, qName);
        }
        catch (XPathExpressionException xPathExpressionException) {
            throw new IllegalArgumentException(xPathExpressionException);
        }
    }

    public static Stream<Node> streamElements(Node node2) {
        return XPathUtilities.stream(node2.getChildNodes()).filter(node -> node.getNodeType() == 1);
    }

    public static Stream<Node> stream(NodeList nodeList) {
        return IntStream.range(0, nodeList.getLength()).mapToObj(nodeList::item);
    }

    public static <K extends Enum<K>> EnumMap<K, String> getEnumMap(Node node, Class<K> clazz) {
        EnumMap<K, String> enumMap = new EnumMap<K, String>(clazz);
        for (K enum_ : clazz.getEnumConstants()) {
            String string = XPathUtilities.getTextContent(enum_.name(), node);
            if (string == null || string.length() <= 0) continue;
            enumMap.put(enum_, string);
        }
        return enumMap;
    }

    private XPathUtilities() {
        throw new UnsupportedOperationException();
    }
}

