package net.filemaid.subtitle;

import net.filemaid.MediaTypes;
import net.filemaid.subtitle.MPlayerReader;
import net.filemaid.subtitle.MicroDVDReader;
import net.filemaid.subtitle.SamiDecoder;
import net.filemaid.subtitle.SubRipReader;
import net.filemaid.subtitle.SubStationAlphaReader;
import net.filemaid.subtitle.SubViewerReader;
import net.filemaid.subtitle.SubtitleDecoder;
import net.filemaid.subtitle.TMPlayerReader;
import net.filemaid.util.ExtensionFileFilter;

public enum SubtitleFormat {
    SubRip{

        @Override
        public SubtitleDecoder getDecoder() {
            return SubRipReader::decode;
        }

        @Override
        public ExtensionFileFilter getFilter() {
            return MediaTypes.SRT;
        }
    }
    ,
    MicroDVD{

        @Override
        public SubtitleDecoder getDecoder() {
            return MicroDVDReader::decode;
        }

        @Override
        public ExtensionFileFilter getFilter() {
            return MediaTypes.SUB;
        }
    }
    ,
    SubViewer{

        @Override
        public SubtitleDecoder getDecoder() {
            return SubViewerReader::decode;
        }

        @Override
        public ExtensionFileFilter getFilter() {
            return MediaTypes.SUB;
        }
    }
    ,
    SubStationAlpha{

        @Override
        public SubtitleDecoder getDecoder() {
            return SubStationAlphaReader::decode;
        }

        @Override
        public ExtensionFileFilter getFilter() {
            return MediaTypes.SSA;
        }
    }
    ,
    SAMI{

        @Override
        public SubtitleDecoder getDecoder() {
            return SamiDecoder::decode;
        }

        @Override
        public ExtensionFileFilter getFilter() {
            return MediaTypes.SAMI;
        }
    }
    ,
    MPL2{

        @Override
        public SubtitleDecoder getDecoder() {
            return MPlayerReader::decode;
        }

        @Override
        public ExtensionFileFilter getFilter() {
            return new ExtensionFileFilter("mpl", "txt");
        }
    }
    ,
    TMPlayer{

        @Override
        public SubtitleDecoder getDecoder() {
            return TMPlayerReader::decode;
        }

        @Override
        public ExtensionFileFilter getFilter() {
            return new ExtensionFileFilter("tmp", "txt");
        }
    };


    public abstract SubtitleDecoder getDecoder();

    public abstract ExtensionFileFilter getFilter();
}

