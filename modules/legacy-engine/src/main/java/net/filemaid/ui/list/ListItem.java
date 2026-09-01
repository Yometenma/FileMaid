package net.filemaid.ui.list;

import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import javax.swing.SwingWorker;
import net.filemaid.Logging;
import net.filemaid.Parallelism;
import net.filemaid.format.ExpressionFormat;
import net.filemaid.ui.list.IndexedBindingBean;

public class ListItem
extends SwingWorker<String, Void> {
    private final int index;
    private final Object object;
    private final int i;
    private final int from;
    private final int to;
    private final List<?> context;
    private final ExpressionFormat format;
    private String value;
    private Exception error;
    private static final Parallelism evaluatorPool = new Parallelism("Evaluator", Parallelism.THREAD_POOL_SIZE.min(), new BalanceQueue());

    public ListItem(int n, Object object, int n2, int n3, int n4, List<?> list, ExpressionFormat expressionFormat) {
        this.index = n;
        this.object = object;
        this.i = n2;
        this.from = n3;
        this.to = n4;
        this.context = list;
        this.format = expressionFormat;
        this.value = null;
        this.error = null;
    }

    public void prefill(String string) {
        this.value = string;
        this.error = null;
    }

    public void prefill(ListItem listItem) {
        this.value = listItem.value;
        this.error = listItem.error;
    }

    public int getIndex() {
        return this.index;
    }

    public Object getObject() {
        return this.object;
    }

    public String getFormattedValue() {
        if (this.isPending()) {
            this.schedule();
        }
        try {
            return (String)this.get();
        }
        catch (Exception exception) {
            return Logging.cause(exception).toString();
        }
    }

    @Override
    protected String doInBackground() throws Exception {
        try {
            this.value = this.format.isEmpty() ? this.value : this.format.format(new IndexedBindingBean(this.object, this.i, this.from, this.to, this.context));
            this.error = null;
        }
        catch (Exception exception) {
            this.value = Logging.cause(exception).toString();
            this.error = exception;
        }
        return this.value;
    }

    public boolean error() {
        return this.error != null;
    }

    public String toString() {
        return this.value;
    }

    public boolean isPending() {
        return this.getState() == SwingWorker.StateValue.PENDING;
    }

    public ListItem withFormat(ExpressionFormat expressionFormat) {
        return new ListItem(this.index, this.object, this.i, this.from, this.to, this.context, expressionFormat);
    }

    public CompletableFuture<String> schedule() {
        return ListItem.evaluatorPool().async(this);
    }

    public static Parallelism evaluatorPool() {
        return evaluatorPool;
    }

    private static class BalanceQueue
    extends LinkedBlockingDeque<Runnable> {
        private final Random random = new Random(0L);

        private BalanceQueue() {
        }

        @Override
        public Runnable poll(long l, TimeUnit timeUnit) throws InterruptedException {
            return this.random.nextBoolean() ? (Runnable)this.pollFirst(l, timeUnit) : (Runnable)this.pollLast(l, timeUnit);
        }
    }
}

