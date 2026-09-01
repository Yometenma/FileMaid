package net.filemaid;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

@XmlRootElement(name="history")
public class History {
    @XmlElement(name="sequence")
    protected List<Sequence> sequences;
    public static final Comparator<Sequence> DATE_DESCENDING = Comparator.comparing(Sequence::date, Comparator.reverseOrder());

    public History() {
        this.sequences = new ArrayList<Sequence>();
    }

    public History(Collection<Sequence> collection) {
        this.sequences = new ArrayList<Sequence>(collection);
    }

    public List<Sequence> sequences() {
        return Collections.unmodifiableList(this.sequences);
    }

    public void add(Collection<Element> collection) {
        Sequence sequence = new Sequence();
        sequence.date = new Date();
        sequence.elements = new ArrayList<Element>(collection);
        this.sequences.add(sequence);
    }

    public History add(History history) {
        this.sequences.addAll(history.sequences());
        return this;
    }

    public History merge(History history) {
        for (Sequence sequence : history.sequences()) {
            if (this.sequences.contains(sequence)) continue;
            this.sequences.add(sequence);
        }
        return this;
    }

    public int size() {
        return this.sequences().stream().mapToInt(sequence -> sequence.elements.size()).sum();
    }

    public void clear() {
        this.sequences.clear();
    }

    public boolean equals(Object object) {
        if (object instanceof History) {
            History history = (History)object;
            return this.sequences.equals(history.sequences);
        }
        return false;
    }

    public int hashCode() {
        return this.sequences.hashCode();
    }

    public Map<File, File> getRenameMap() {
        LinkedHashMap<File, File> linkedHashMap = new LinkedHashMap<File, File>(this.size());
        for (Sequence sequence : this.sequences) {
            for (Element element : sequence.elements()) {
                File file;
                File file2 = new File(element.from());
                if (!file2.isAbsolute()) {
                    file2 = new File(element.dir(), file2.getPath());
                }
                if (!(file = new File(element.to())).isAbsolute()) {
                    file = new File(element.dir(), file.getPath());
                }
                linkedHashMap.put(file2, file);
            }
        }
        return linkedHashMap;
    }

    public Map<File, File> getReverseRenameMap() {
        HashMap<File, File> hashMap = new HashMap<File, File>(this.size());
        this.getRenameMap().forEach((file, file2) -> hashMap.put((File)file2, (File)file));
        return hashMap;
    }

    public Stream<History> split(Comparator<Sequence> comparator) {
        return this.sequences.stream().sorted(comparator).map(sequence -> new History(Collections.singleton(sequence)));
    }

    public static void exportHistory(History history, boolean bl, OutputStream outputStream) throws Exception {
        Marshaller marshaller = JAXBContext.newInstance((Class[])new Class[]{History.class}).createMarshaller();
        marshaller.setProperty("jaxb.formatted.output", (Object)Boolean.TRUE);
        marshaller.setProperty("jaxb.fragment", (Object)bl);
        marshaller.marshal((Object)history, outputStream);
    }

    public static History importHistory(InputStream inputStream) throws Exception {
        Unmarshaller unmarshaller = JAXBContext.newInstance((Class[])new Class[]{History.class}).createUnmarshaller();
        return (History)unmarshaller.unmarshal(inputStream);
    }

    public static class Sequence {
        @XmlAttribute(name="date", required=true)
        protected Date date;
        @XmlElement(name="rename", required=true)
        protected List<Element> elements;

        public Date date() {
            return this.date;
        }

        public List<Element> elements() {
            return this.elements == null ? Collections.emptyList() : Collections.unmodifiableList(this.elements);
        }

        public boolean equals(Object object) {
            if (object instanceof Sequence) {
                Sequence sequence = (Sequence)object;
                return this.date.equals(sequence.date) && this.elements.equals(sequence.elements);
            }
            return false;
        }

        public int hashCode() {
            return Objects.hash(this.elements, this.date);
        }
    }

    public static class Element {
        @XmlAttribute(name="dir", required=true)
        protected File dir;
        @XmlAttribute(name="from", required=true)
        protected String from;
        @XmlAttribute(name="to", required=true)
        protected String to;

        public Element() {
        }

        public Element(String string, String string2, File file) {
            this.from = string;
            this.to = string2;
            this.dir = file;
        }

        public File dir() {
            return this.dir;
        }

        public String from() {
            return this.from;
        }

        public String to() {
            return this.to;
        }

        public boolean equals(Object object) {
            if (object instanceof Element) {
                Element element = (Element)object;
                return this.to.equals(element.to) && this.from.equals(element.from) && this.dir.getPath().equals(element.dir.getPath());
            }
            return false;
        }

        public int hashCode() {
            return Objects.hash(this.to, this.from, this.dir);
        }
    }
}

