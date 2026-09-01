package net.filemaid.postprocess;

import java.io.File;
import java.util.List;
import java.util.Locale;
import net.filemaid.Resource;
import net.filemaid.WebServices;
import net.filemaid.media.MediaFileUtilities;
import net.filemaid.postprocess.ApplyMetadata;
import net.filemaid.postprocess.Feedback;
import net.filemaid.util.StringUtilities;
import net.filemaid.web.AnimeLists;
import net.filemaid.web.Datasource;
import net.filemaid.web.Episode;
import net.filemaid.web.EpisodeUtilities;
import net.filemaid.web.LookupException;
import net.filemaid.web.Movie;
import net.filemaid.web.Person;
import net.filemaid.web.Series;
import net.filemaid.web.SeriesDetails;
import net.filemaid.web.SeriesInfo;
import net.filemaid.web.XDB;

public abstract class ApplyFolder
implements ApplyMetadata {
    @Override
    public void apply(File file, File file2, Movie movie, Feedback feedback) throws Exception {
        this.applyMovieFolder(movie, movie.getLanguage(), file2.getParentFile(), feedback);
    }

    @Override
    public void apply(File file, File file2, Episode episode, Feedback feedback) throws Exception {
        if (episode.getSpecial() != null) {
            return;
        }
        SID sID = SID.from(episode);
        Locale locale = episode.getSeriesInfo().getLanguage();
        Integer n = episode.getSeason();
        File file3 = file2.getParentFile();
        if (n != null && StringUtilities.matchIntegers(file3.getName()).contains(n) && !MediaFileUtilities.isStructureRoot(file3.getParentFile())) {
            this.applySeasonFolder(sID, n, locale, file3, feedback);
            this.applySeriesFolder(sID, locale, file3.getParentFile(), feedback);
        } else {
            this.applySeriesFolder(sID, locale, file3, feedback);
        }
    }

    protected abstract void applyMovieFolder(Movie var1, Locale var2, File var3, Feedback var4) throws Exception;

    protected abstract void applySeriesFolder(SID var1, Locale var2, File var3, Feedback var4) throws Exception;

    protected abstract void applySeasonFolder(SID var1, int var2, Locale var3, File var4, Feedback var5) throws Exception;

    protected static class SID {
        public final ID[] ids;

        public SID(ID ... iDArray) {
            this.ids = iDArray;
        }

        public Resource<Integer> id(XDB xDB) {
            for (ID iD : this.ids) {
                if (iD.db() != xDB) continue;
                return iD::id;
            }
            return null;
        }

        public SeriesDetails getSeriesDetails(Locale locale) throws Exception {
            for (ID iD : this.ids) {
                if (iD.db() == XDB.TheMovieDB) {
                    return WebServices.TheMovieDB_TV.getSeriesInfo(iD.id(), locale);
                }
                if (iD.db() != XDB.TheTVDB) continue;
                return WebServices.TheTVDB.getSeriesInfo(iD.id(), locale);
            }
            return null;
        }

        public List<Person> getCrew(Locale locale) throws Exception {
            for (ID iD : this.ids) {
                if (iD.db() == XDB.TheMovieDB) {
                    return WebServices.TheMovieDB_TV.getCredits(iD.id(), locale);
                }
                if (iD.db() != XDB.TheTVDB) continue;
                return WebServices.TheTVDB.getCharacters(iD.id(), locale);
            }
            return null;
        }

        public Series getExternalSeries() throws Exception {
            int n = 0;
            ID[] iDArray = this.ids;
            int n2 = iDArray.length;
            if (n < n2) {
                ID iD = iDArray[n];
                return iD.db().getExternalSeries(iD.id());
            }
            return null;
        }

        public static SID from(Episode episode) throws Exception {
            SeriesInfo seriesInfo = episode.getSeriesInfo();
            Integer n = seriesInfo.getId();
            if (EpisodeUtilities.isInstance((Datasource)WebServices.TheMovieDB_TV, seriesInfo)) {
                ID iD = ID.of(XDB.TheMovieDB, seriesInfo::getId);
                ID iD2 = ID.of(XDB.TheTVDB, SID.mapSeriesID(n, XDB.TheMovieDB, XDB.TheTVDB));
                return new SID(iD, iD2);
            }
            if (EpisodeUtilities.isInstance((Datasource)WebServices.TheTVDB, seriesInfo)) {
                ID iD = ID.of(XDB.TheTVDB, seriesInfo::getId);
                ID iD3 = ID.of(XDB.TheMovieDB, SID.mapSeriesID(n, XDB.TheTVDB, XDB.TheMovieDB));
                return new SID(iD, iD3);
            }
            if (EpisodeUtilities.isInstance((Datasource)WebServices.AniDB, seriesInfo)) {
                ID iD = ID.of(XDB.AniDB, seriesInfo::getId);
                ID iD4 = ID.of(XDB.TheMovieDB, SID.mapSeriesID(n, episode.getSeason(), episode.getEpisode(), AnimeLists.DB.AniDB, AnimeLists.DB.TheMovieDB));
                ID iD5 = ID.of(XDB.TheTVDB, SID.mapSeriesID(n, episode.getSeason(), episode.getEpisode(), AnimeLists.DB.AniDB, AnimeLists.DB.TheTVDB));
                return new SID(iD, iD4, iD5);
            }
            throw new LookupException("Failed to lookup external ID for " + seriesInfo);
        }

        private static Resource<Integer> mapSeriesID(Integer n, XDB xDB, XDB xDB2) throws Exception {
            return Resource.lazy(() -> {
                Integer n2;
                if (n != null && (n2 = xDB.getExternalId(n, xDB2)) != null) {
                    return n2;
                }
                throw new LookupException("Failed to lookup " + xDB2 + " ID for " + xDB + " ID " + n);
            });
        }

        private static Resource<Integer> mapSeriesID(Integer n, Integer n2, Integer n3, AnimeLists.DB dB, AnimeLists.DB dB2) throws Exception {
            return Resource.lazy(() -> {
                int n4;
                if (n != null && (n4 = WebServices.AnimeList.map(n, n2 == null ? 1 : n2, n3 == null ? 1 : n3, dB, dB2)) > 0) {
                    return n4;
                }
                throw new LookupException("Failed to lookup " + dB2 + " ID for " + dB + " ID " + n);
            });
        }

        private static class ID {
            private final XDB db;
            private final Resource<Integer> id;

            public ID(XDB xDB, Resource<Integer> resource) {
                this.db = xDB;
                this.id = resource;
            }

            public XDB db() {
                return this.db;
            }

            public Integer id() throws Exception {
                return this.id.get();
            }

            public static ID of(XDB xDB, Resource<Integer> resource) {
                return new ID(xDB, resource);
            }
        }
    }
}

