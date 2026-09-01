package net.filemaid.hash;

import java.io.File;
import java.io.IOException;
import java.util.zip.CRC32;
import net.filemaid.CategoryFileFilter;
import net.filemaid.MediaTypes;
import net.filemaid.hash.ChecksumHash;
import net.filemaid.hash.Hash;
import net.filemaid.hash.MessageDigestHash;
import net.filemaid.hash.SfvFormat;
import net.filemaid.hash.VerificationFileReader;
import net.filemaid.hash.VerificationFormat;

public enum HashType {
    SFV{

        @Override
        public Hash newHash() {
            return new ChecksumHash(new CRC32());
        }

        @Override
        public String getAlgorithm() {
            return "CRC32";
        }

        @Override
        public VerificationFormat getFormat() {
            return new SfvFormat();
        }

        @Override
        public CategoryFileFilter getFilter() {
            return new CategoryFileFilter("SFV", MediaTypes.SFV);
        }

        public String toString() {
            return "SFV";
        }
    }
    ,
    MD5{

        @Override
        public Hash newHash() {
            return new MessageDigestHash(this.getAlgorithm());
        }

        @Override
        public String getAlgorithm() {
            return "MD5";
        }

        @Override
        public VerificationFormat getFormat() {
            return new VerificationFormat();
        }

        @Override
        public CategoryFileFilter getFilter() {
            return new CategoryFileFilter("md5sum", MediaTypes.MD5);
        }

        public String toString() {
            return "MD5";
        }
    }
    ,
    SHA1{

        @Override
        public Hash newHash() {
            return new MessageDigestHash(this.getAlgorithm());
        }

        @Override
        public String getAlgorithm() {
            return "SHA-1";
        }

        @Override
        public VerificationFormat getFormat() {
            return new VerificationFormat("SHA1");
        }

        @Override
        public CategoryFileFilter getFilter() {
            return new CategoryFileFilter("sha1sum", MediaTypes.SHA1);
        }

        public String toString() {
            return "SHA1";
        }
    }
    ,
    SHA256{

        @Override
        public Hash newHash() {
            return new MessageDigestHash(this.getAlgorithm());
        }

        @Override
        public String getAlgorithm() {
            return "SHA-256";
        }

        @Override
        public VerificationFormat getFormat() {
            return new VerificationFormat("SHA256");
        }

        @Override
        public CategoryFileFilter getFilter() {
            return new CategoryFileFilter("sha256sum", MediaTypes.SHA256);
        }

        public String toString() {
            return "SHA2";
        }
    }
    ,
    SHA3_384{

        @Override
        public Hash newHash() {
            return new MessageDigestHash(this.getAlgorithm());
        }

        @Override
        public String getAlgorithm() {
            return "SHA3-384";
        }

        @Override
        public VerificationFormat getFormat() {
            return new VerificationFormat("SHA3-384");
        }

        @Override
        public CategoryFileFilter getFilter() {
            return new CategoryFileFilter("sha3sum", MediaTypes.SHA3);
        }

        public String toString() {
            return "SHA3";
        }
    };


    public abstract Hash newHash();

    public abstract String getAlgorithm();

    public abstract VerificationFormat getFormat();

    public abstract CategoryFileFilter getFilter();

    public VerificationFileReader newReader(File file) throws IOException {
        return VerificationFileReader.open(file, this.getFormat());
    }
}

