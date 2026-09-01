package net.filemaid.archive;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import net.filemaid.archive.ExtractOutProvider;
import net.filemaid.archive.ExtractOutStream;
import net.sf.sevenzipjbinding.ExtractAskMode;
import net.sf.sevenzipjbinding.ExtractOperationResult;
import net.sf.sevenzipjbinding.IArchiveExtractCallback;
import net.sf.sevenzipjbinding.IInArchive;
import net.sf.sevenzipjbinding.ISequentialOutStream;
import net.sf.sevenzipjbinding.PropID;
import net.sf.sevenzipjbinding.SevenZipException;

class ExtractCallback
implements IArchiveExtractCallback {
    private IInArchive inArchive;
    private ExtractOutProvider extractOut;
    private ExtractOutStream output = null;

    public ExtractCallback(IInArchive iInArchive, ExtractOutProvider extractOutProvider) {
        this.inArchive = iInArchive;
        this.extractOut = extractOutProvider;
    }

    public ISequentialOutStream getStream(int n, ExtractAskMode extractAskMode) throws SevenZipException {
        if (extractAskMode != ExtractAskMode.EXTRACT) {
            return null;
        }
        boolean bl = (Boolean)this.inArchive.getProperty(n, PropID.IS_FOLDER);
        if (bl) {
            return null;
        }
        String string = (String)this.inArchive.getProperty(n, PropID.PATH);
        try {
            OutputStream outputStream = this.extractOut.getStream(new File(string));
            if (outputStream == null) {
                return null;
            }
            this.output = new ExtractOutStream(outputStream);
            return this.output;
        }
        catch (IOException iOException) {
            throw new SevenZipException((Throwable)iOException);
        }
    }

    public void prepareOperation(ExtractAskMode extractAskMode) throws SevenZipException {
    }

    public void setOperationResult(ExtractOperationResult extractOperationResult) throws SevenZipException {
        if (this.output != null) {
            try {
                this.output.close();
            }
            catch (IOException iOException) {
                throw new SevenZipException((Throwable)iOException);
            }
            finally {
                this.output = null;
            }
        }
        if (extractOperationResult != ExtractOperationResult.OK) {
            throw new SevenZipException("Extraction Error: " + extractOperationResult);
        }
    }

    public void setCompleted(long l) throws SevenZipException {
    }

    public void setTotal(long l) throws SevenZipException {
    }
}

