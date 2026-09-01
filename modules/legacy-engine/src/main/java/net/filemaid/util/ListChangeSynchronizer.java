package net.filemaid.util;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;
import java.util.List;

public class ListChangeSynchronizer<E>
implements ListEventListener<E> {
    private final List<E> target;

    public ListChangeSynchronizer(EventList<E> eventList, List<E> list) {
        this.target = list;
        eventList.addListEventListener((ListEventListener)this);
    }

    public void listChanged(ListEvent<E> listEvent) {
        EventList<E> eventList = listEvent.getSourceList();
        while (listEvent.next()) {
            int n = listEvent.getIndex();
            int n2 = listEvent.getType();
            switch (n2) {
                case 2: {
                    this.target.add(n, eventList.get(n));
                    break;
                }
                case 1: {
                    this.target.set(n, eventList.get(n));
                    break;
                }
                case 0: {
                    this.target.remove(n);
                }
            }
        }
    }

    public static <E> ListChangeSynchronizer<E> syncEventListToList(EventList<E> eventList, List<E> list) {
        return new ListChangeSynchronizer<E>(eventList, list);
    }
}

