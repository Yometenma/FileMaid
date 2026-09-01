package net.filemaid.ui.list;

import java.io.File;
import java.util.List;
import java.util.Map;
import net.filemaid.format.Define;
import net.filemaid.format.MediaBindingBean;
import net.filemaid.media.XattrMetaInfo;
import net.filemaid.util.EntryList;
import net.filemaid.util.FunctionList;

public class IndexedBindingBean
extends MediaBindingBean {
    private int i;
    private int from;
    private int to;

    public IndexedBindingBean(Object object, int n, int n2, int n3, List<?> list) {
        super(IndexedBindingBean.getInfoObject(object), IndexedBindingBean.getMediaFile(object), IndexedBindingBean.getContext(list));
        this.i = n;
        this.from = n2;
        this.to = n3;
    }

    @Override
    @Define(value={"i"})
    public Integer getModelIndex() {
        return this.i;
    }

    @Define(value={"from"})
    public Integer getFromIndex() {
        return this.from;
    }

    @Define(value={"to"})
    public Integer getToIndex() {
        return this.to;
    }

    private static Object getInfoObject(Object object) {
        File file;
        Object object2;
        if (object instanceof File && (object2 = XattrMetaInfo.xattr.getMetaInfo(file = (File)object)) != null) {
            return object2;
        }
        return object;
    }

    private static File getMediaFile(Object object) {
        return object instanceof File ? (File)object : new File(object.toString());
    }

    private static Map<File, Object> getContext(List<?> list) {
        List<Object> list2 = FunctionList.of(list, IndexedBindingBean::getInfoObject);
        List<File> list3 = FunctionList.of(list, IndexedBindingBean::getMediaFile);
        return EntryList.of(list3, list2);
    }
}

