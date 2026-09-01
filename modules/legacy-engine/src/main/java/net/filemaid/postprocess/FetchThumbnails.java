package net.filemaid.postprocess;

import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.URL;
import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageInputStream;
import net.filemaid.Deployment;
import net.filemaid.MediaTypes;
import net.filemaid.Resource;
import net.filemaid.WebServices;
import net.filemaid.postprocess.Apply;
import net.filemaid.postprocess.ApplyMetadata;
import net.filemaid.postprocess.Feedback;
import net.filemaid.util.ByteBufferOutputStream;
import net.filemaid.util.FileUtilities;
import net.filemaid.web.Datasource;
import net.filemaid.web.Episode;
import net.filemaid.web.EpisodeDetails;
import net.filemaid.web.EpisodeListProvider;
import net.filemaid.web.EpisodeUtilities;
import net.filemaid.web.Movie;
import net.filemaid.web.MovieDetails;
import org.imgscalr.Scalr;

public enum FetchThumbnails implements ApplyMetadata
{
    DEFAULT{

        @Override
        protected File getThumbnailFile(File file) {
            return new File(file.getParentFile(), FileUtilities.getName(file) + ".jpg");
        }

        @Override
        protected void generateThumbnailFile(File file, File file2, BufferedImage bufferedImage, Feedback feedback) throws Exception {
            this.write(file2, this.fit(bufferedImage, 4096, 2160), file.lastModified(), feedback);
        }
    }
    ,
    SYNO{

        @Override
        protected File getThumbnailFile(File file) {
            return new File(file.getParentFile(), "@eaDir/" + file.getName() + "/SYNOVIDEO_VIDEO_SCREENSHOT.jpg");
        }

        @Override
        protected void generateThumbnailFile(File file, File file2, BufferedImage bufferedImage, Feedback feedback) throws Exception {
            this.write(file2, this.fit(bufferedImage, 512, 512), file.lastModified(), feedback);
        }
    }
    ,
    QNAP{

        @Override
        protected File getThumbnailFile(File file) {
            return this.getThumbnailFile(file, "default");
        }

        protected File getThumbnailFile(File file, String string) {
            return new File(file.getParentFile(), ".@__thumb/" + string + file.getName());
        }

        @Override
        protected void generateThumbnailFile(File file, File file2, BufferedImage bufferedImage, Feedback feedback) throws Exception {
            long l = file.lastModified();
            bufferedImage = this.fit(bufferedImage, 1200, 1200);
            this.write(this.getThumbnailFile(file, "s800"), bufferedImage, l, feedback);
            bufferedImage = this.fit(bufferedImage, 600, 600);
            this.write(file2, bufferedImage, l, feedback);
            bufferedImage = this.fit(bufferedImage, 260, 260);
            this.write(this.getThumbnailFile(file, "s100"), bufferedImage, l, feedback);
        }
    };


    protected abstract File getThumbnailFile(File var1);

    protected abstract void generateThumbnailFile(File var1, File var2, BufferedImage var3, Feedback var4) throws Exception;

    @Override
    public boolean accept(File file, Object object) {
        return MediaTypes.VIDEO_FILES.accept(file);
    }

    @Override
    public void apply(File file, File file2, Movie movie, Feedback feedback) throws Exception {
        this.apply(file2, Resource.lazy(() -> this.getMovieArtwork(movie)), feedback);
    }

    @Override
    public void apply(File file, File file2, Episode episode, Feedback feedback) throws Exception {
        this.apply(file2, Resource.lazy(() -> this.getEpisodeArtwork(episode)), feedback);
    }

    private void apply(File file, Resource<URL> resource, Feedback feedback) throws Exception {
        File file2 = this.getThumbnailFile(file);
        if (file2.exists()) {
            return;
        }
        FileUtilities.createFolders(file2.getParentFile());
        URL uRL = resource.get();
        if (uRL == null) {
            feedback.trace("No artwork", file);
            return;
        }
        feedback.info(uRL, file);
        BufferedImage bufferedImage = this.read(uRL);
        if (bufferedImage == null) {
            feedback.warning("Invalid image file: " + uRL, file);
            return;
        }
        this.generateThumbnailFile(file, file2, bufferedImage, feedback);
    }

    private URL getMovieArtwork(Movie movie) throws Exception {
        MovieDetails movieDetails = WebServices.TheMovieDB.getMovieInfo(movie, movie.getLanguage(), true);
        return movieDetails == null ? null : movieDetails.getPoster();
    }

    private URL getEpisodeArtwork(Episode episode) throws Exception {
        for (EpisodeListProvider episodeListProvider : WebServices.getEpisodeListProviders()) {
            if (!EpisodeUtilities.isInstance((Datasource)episodeListProvider, episode)) continue;
            EpisodeDetails episodeDetails = episodeListProvider.getEpisodeInfo(episode, episode.getSeriesInfo().getLanguage());
            return episodeDetails == null ? null : episodeDetails.getImage();
        }
        return null;
    }

    protected BufferedImage read(URL uRL) throws Exception {
        return ImageIO.read(new MemoryCacheImageInputStream(new ByteArrayInputStream(this.cache(uRL))));
    }

    protected void write(File file, BufferedImage bufferedImage, long l, Feedback feedback) throws Exception {
        ByteBufferOutputStream byteBufferOutputStream = new ByteBufferOutputStream(65536);
        if (ImageIO.write((RenderedImage)bufferedImage, "jpg", byteBufferOutputStream)) {
            feedback.file(bufferedImage.getWidth() + "x" + bufferedImage.getHeight(), file);
            FileUtilities.writeFile(byteBufferOutputStream.getByteBuffer(), file);
            file.setLastModified(l);
        }
    }

    protected BufferedImage fit(BufferedImage bufferedImage, int n, int n2) {
        if (n < bufferedImage.getWidth() || n2 < bufferedImage.getHeight()) {
            return Scalr.resize((BufferedImage)bufferedImage, (Scalr.Method)Scalr.Method.ULTRA_QUALITY, (Scalr.Mode)Scalr.Mode.AUTOMATIC, (int)n, (int)n2, (BufferedImageOp[])new BufferedImageOp[0]);
        }
        return bufferedImage;
    }

    public static Apply platform(Deployment deployment) {
        switch (deployment) {
            case SPK: {
                return SYNO;
            }
            case QPKG: {
                return QNAP;
            }
        }
        return DEFAULT;
    }
}

