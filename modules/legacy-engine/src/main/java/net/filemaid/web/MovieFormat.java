package net.filemaid.web;

import java.text.FieldPosition;
import java.text.Format;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.filemaid.web.Movie;
import net.filemaid.web.MoviePart;

public class MovieFormat
extends Format {
    public static final MovieFormat DEFAULT = new MovieFormat();
    private final Pattern moviePattern = Pattern.compile("([^\\p{Punct}]+?)[\\p{Punct}\\s]+(\\d{4})(?:[\\p{Punct}\\s]+|$)");
    private final Pattern partPattern = Pattern.compile("(?:Part|CD)\\D?(\\d)$", 2);

    @Override
    public StringBuffer format(Object object, StringBuffer stringBuffer, FieldPosition fieldPosition) {
        MoviePart moviePart;
        Movie movie = (Movie)object;
        stringBuffer.append(movie.getName());
        if (movie.getYear() > 0) {
            stringBuffer.append(" (").append(movie.getYear()).append(")");
        }
        if (movie instanceof MoviePart && (moviePart = (MoviePart)movie).getPartCount() > 1) {
            stringBuffer.append(".CD").append(moviePart.getPartIndex());
        }
        return stringBuffer;
    }

    @Override
    public Movie parseObject(String string, ParsePosition parsePosition) {
        String string2 = string;
        int n = -1;
        int n2 = -1;
        Matcher matcher = this.partPattern.matcher(string2);
        if (matcher.find()) {
            n = Integer.parseInt(matcher.group(1));
            string2 = matcher.replaceFirst("");
        }
        if ((matcher = this.moviePattern.matcher(string2)).matches()) {
            String string3 = matcher.group(1).trim();
            int n3 = Integer.parseInt(matcher.group(2));
            Movie movie = Movie.NameYear(string3, n3);
            if (n >= 0) {
                movie = new MoviePart(movie, n, n2);
            }
            parsePosition.setIndex(string.length());
            return movie;
        }
        parsePosition.setErrorIndex(0);
        return null;
    }

    @Override
    public Movie parseObject(String string) throws ParseException {
        return (Movie)super.parseObject(string);
    }
}

