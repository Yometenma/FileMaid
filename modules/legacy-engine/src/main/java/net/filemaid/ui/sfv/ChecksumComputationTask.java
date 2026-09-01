package net.filemaid.ui.sfv;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CancellationException;
import javax.swing.SwingWorker;
import net.filemaid.hash.Hash;
import net.filemaid.hash.HashType;

class ChecksumComputationTask
extends SwingWorker<Map<HashType, String>, Void> {
    private final File file;
    private final HashType hashType;

    public ChecksumComputationTask(File file, HashType hashType) {
        this.file = file;
        this.hashType = hashType;
    }

    @Override
    protected Map<HashType, String> doInBackground() throws Exception {
        Hash hash = this.hashType.newHash();
        long l = this.file.length();
        try (FileInputStream fileInputStream = new FileInputStream(this.file);){
            byte[] byArray = new byte[0x400000];
            long l2 = 0L;
            int n = 0;
            while ((n = ((InputStream)fileInputStream).read(byArray)) >= 0) {
                hash.update(byArray, 0, n);
                this.setProgress((int)((l2 += (long)n) * 100L / l));
                if (!this.isCancelled() && !Thread.interrupted()) continue;
                throw new CancellationException();
            }
        }
        return Collections.singletonMap(this.hashType, hash.digest());
    }
}

