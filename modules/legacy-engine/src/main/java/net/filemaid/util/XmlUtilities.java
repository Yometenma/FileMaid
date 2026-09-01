package net.filemaid.util;

import java.io.File;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import net.filemaid.util.ByteBufferOutputStream;
import net.filemaid.util.FileUtilities;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public final class XmlUtilities {
    private static final String XML_DECLARATION = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";

    public static Document newDocument() throws Exception {
        return DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
    }

    public static Element root(String string) throws Exception {
        Document document = XmlUtilities.newDocument();
        document.setXmlStandalone(true);
        Element element = document.createElement(string);
        document.appendChild(element);
        return element;
    }

    public static Element element(Node node, String string) {
        Element element = node.getOwnerDocument().createElement(string);
        node.appendChild(element);
        return element;
    }

    public static Element text(Node node, String string, Object object) {
        Element element = XmlUtilities.element(node, string);
        element.appendChild(node.getOwnerDocument().createTextNode(object == null ? "" : object.toString()));
        return element;
    }

    public static Element attr(Element element, String string, Object object) {
        element.setAttribute(string, object == null ? "" : object.toString());
        return element;
    }

    public static String writeDocument(Node node) throws Exception {
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty("omit-xml-declaration", "yes");
        transformer.setOutputProperty("indent", "no");
        StringWriter stringWriter = new StringWriter();
        transformer.transform(new DOMSource(node), new StreamResult(stringWriter));
        return stringWriter.toString();
    }

    public static File writeDocument(Document document, File file) throws Exception {
        ByteBufferOutputStream byteBufferOutputStream = new ByteBufferOutputStream(65536);
        byteBufferOutputStream.write(XML_DECLARATION.getBytes(StandardCharsets.UTF_8));
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty("omit-xml-declaration", "yes");
        transformer.setOutputProperty("indent", "yes");
        transformer.transform(new DOMSource(document), new StreamResult(byteBufferOutputStream));
        return FileUtilities.writeFile(byteBufferOutputStream.getByteBuffer(), file);
    }

    private XmlUtilities() {
        throw new UnsupportedOperationException();
    }
}

