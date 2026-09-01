package net.filemaid.util;

import groovy.json.JsonOutput;
import groovy.lang.Closure;
import groovy.util.Node;
import groovy.util.NodeBuilder;
import groovy.xml.MarkupBuilder;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;

public enum Builder {
    XML{

        @Override
        public String toString(Closure closure) {
            StringWriter stringWriter = new StringWriter();
            MarkupBuilder markupBuilder = new MarkupBuilder((Writer)stringWriter);
            closure.rehydrate(closure.getDelegate(), (Object)markupBuilder, (Object)markupBuilder).call();
            return stringWriter.toString();
        }
    }
    ,
    JSON{

        @Override
        public String toString(Closure closure) {
            return JsonOutput.toJson((Closure)closure);
        }
    }
    ,
    INI{

        @Override
        public String toString(Closure closure) {
            final StringBuilder stringBuilder = new StringBuilder();
            final StringBuilder stringBuilder2 = new StringBuilder();
            NodeBuilder nodeBuilder = new NodeBuilder(){

                protected void nodeCompleted(Object object3, Object object4) {
                    if (object3 == null) {
                        Node node = (Node)object4;
                        if (node.value() instanceof List || node.attributes().size() > 0) {
                            stringBuilder2.append(stringBuilder2.length() == 0 ? "" : "\r\n").append("[").append(node.name()).append("]").append("\r\n");
                            node.attributes().forEach((object, object2) -> stringBuilder2.append(object).append("=").append(object2).append("\r\n"));
                            for (Object e : node.children()) {
                                Node node2 = (Node)e;
                                stringBuilder2.append(node2.name()).append("=").append(node2.text()).append("\r\n");
                            }
                        } else {
                            stringBuilder.append(node.name()).append("=").append(node.value()).append("\r\n");
                        }
                    }
                }
            };
            closure.rehydrate(closure.getDelegate(), (Object)nodeBuilder, (Object)nodeBuilder).call();
            if (stringBuilder.length() == 0) {
                return stringBuilder2.toString();
            }
            if (stringBuilder2.length() == 0) {
                return stringBuilder.toString();
            }
            return String.join((CharSequence)"\r\n", stringBuilder, stringBuilder2);
        }
    };


    public abstract String toString(Closure var1);
}

