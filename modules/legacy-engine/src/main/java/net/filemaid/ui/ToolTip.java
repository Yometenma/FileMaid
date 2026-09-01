package net.filemaid.ui;

import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.swing.JComponent;
import net.filemaid.Logging;
import net.filemaid.WebServices;
import net.filemaid.util.StringUtilities;
import net.filemaid.util.ui.SwingUI;
import net.filemaid.web.Link;
import net.filemaid.web.Movie;
import net.filemaid.web.MovieDetails;
import net.filemaid.web.SearchResult;
import net.filemaid.web.SearchResultDetails;

public enum ToolTip {
    HTML;


    public String format(Object object) {
        if (object instanceof SearchResult) {
            try {
                SearchResult searchResult;
                SearchResult searchResult2 = (SearchResult)object;
                StringBuilder stringBuilder = new StringBuilder(64);
                stringBuilder.append("<html><h3>").append(SwingUI.escapeHTML(searchResult2.toString())).append("</h3>");
                if (searchResult2 instanceof SearchResultDetails) {
                    searchResult = (SearchResultDetails)searchResult2;
                    this.appendTooltipParagraph(stringBuilder, "First Aired", ((SearchResultDetails)searchResult).getFirstAired());
                    this.appendTooltipParagraph(stringBuilder, "Language", ((SearchResultDetails)searchResult).getOriginalLanguage());
                    this.appendTooltipParagraph(stringBuilder, "Country", ((SearchResultDetails)searchResult).getCountry());
                    this.appendTooltipParagraph(stringBuilder, "Network", ((SearchResultDetails)searchResult).getNetwork());
                    this.appendTooltipParagraph(stringBuilder, "Status", ((SearchResultDetails)searchResult).getStatus());
                    this.appendTooltipParagraph(stringBuilder, "Overview", ((SearchResultDetails)searchResult).getOverview());
                    this.appendTooltipParagraph(stringBuilder, "Popularity", ((SearchResultDetails)searchResult).getPopularity());
                }
                if (searchResult2 instanceof Movie) {
                    searchResult = (Movie)searchResult2;
                    try {
                        MovieDetails movieDetails = WebServices.requestPool().async(() -> ToolTip.lambda$format$0((Movie)searchResult2)).get(500L, TimeUnit.MILLISECONDS);
                        if (!searchResult2.getName().contains(movieDetails.getOriginalName())) {
                            this.appendTooltipParagraph(stringBuilder, "Original Name", movieDetails.getOriginalName());
                        }
                        this.appendTooltipParagraph(stringBuilder, "Release Date", movieDetails.getReleased());
                        this.appendTooltipParagraph(stringBuilder, "Language", movieDetails.getOriginalLanguage());
                        this.appendTooltipParagraph(stringBuilder, "Country", movieDetails.getProductionCountries());
                        this.appendTooltipParagraph(stringBuilder, "Production", movieDetails.getProductionCompanies());
                        this.appendTooltipParagraph(stringBuilder, "Director", movieDetails.getDirector());
                        this.appendTooltipParagraph(stringBuilder, "Rating", movieDetails.getCertification());
                        this.appendTooltipParagraph(stringBuilder, "Genre", movieDetails.getGenres());
                        this.appendTooltipParagraph(stringBuilder, "Overview", movieDetails.getOverview());
                        this.appendTooltipParagraph(stringBuilder, "Runtime", movieDetails.getRuntime());
                        this.appendTooltipParagraph(stringBuilder, "Popularity", movieDetails.getPopularity());
                    }
                    catch (InterruptedException | TimeoutException exception) {
                        this.appendTooltipSection(stringBuilder, "...");
                        Logging.debug.finest(Logging.cause((Object)this, searchResult, exception));
                    }
                    catch (Exception exception) {
                        this.appendTooltipSection(stringBuilder, Logging.cause(exception));
                        Logging.debug.warning(Logging.cause((Object)this, searchResult, exception));
                    }
                    this.appendTooltipParagraph(stringBuilder, "IMDb ID", Link.IMDb.getID(((Movie)searchResult).getImdbId()));
                    this.appendTooltipParagraph(stringBuilder, "TMDb ID", Link.TheMovieDB.getID(((Movie)searchResult).getTmdbId()));
                } else {
                    this.appendTooltipParagraph(stringBuilder, "ID", searchResult2.getId());
                }
                this.appendTooltipParagraph(stringBuilder, "AKA", searchResult2.getAliasNames());
                return stringBuilder.append("</html>").toString();
            }
            catch (Exception exception) {
                Logging.trace((Object)this, exception);
            }
        }
        if (object instanceof File) {
            File file = (File)object;
            return file.getAbsolutePath();
        }
        return null;
    }

    private StringBuilder appendTooltipSection(StringBuilder stringBuilder, Object object) {
        return object == null ? stringBuilder : stringBuilder.append("<p style='width:250px; margin:6px'>").append(SwingUI.escapeHTML(object.toString())).append("</p>");
    }

    private StringBuilder appendTooltipParagraph(StringBuilder stringBuilder, String string, String string2) {
        return string2 == null || string2.isEmpty() ? stringBuilder : stringBuilder.append("<p style='width:250px; margin:3px'><b>").append(string).append(":</b> ").append(SwingUI.escapeHTML(string2)).append("</p>");
    }

    private StringBuilder appendTooltipParagraph(StringBuilder stringBuilder, String string, Object object) {
        return object == null ? stringBuilder : this.appendTooltipParagraph(stringBuilder, string, object.toString());
    }

    private StringBuilder appendTooltipParagraph(StringBuilder stringBuilder, String string, String[] stringArray) {
        return stringArray == null || stringArray.length == 0 ? stringBuilder : this.appendTooltipParagraph(stringBuilder, string, StringUtilities.join(stringArray, (CharSequence)" | "));
    }

    private StringBuilder appendTooltipParagraph(StringBuilder stringBuilder, String string, List<String> list) {
        return list == null || list.isEmpty() ? stringBuilder : this.appendTooltipParagraph(stringBuilder, string, StringUtilities.join(list, (CharSequence)" | "));
    }

    public void setToolTip(JComponent jComponent, Object object) {
        if (object != jComponent.getClientProperty(ToolTip.class)) {
            jComponent.putClientProperty(ToolTip.class, object);
            jComponent.putClientProperty((Object)this, null);
        }
    }

    public String getToolTip(JComponent jComponent) {
        String string = (String)jComponent.getClientProperty((Object)this);
        if (string == null) {
            string = this.format(jComponent.getClientProperty(ToolTip.class));
            jComponent.putClientProperty((Object)this, string);
        }
        return string;
    }

    private static /* synthetic */ MovieDetails lambda$format$0(Movie movie) throws Exception {
        return WebServices.TheMovieDB.getMovieInfo(movie, movie.getLanguage(), false);
    }
}

