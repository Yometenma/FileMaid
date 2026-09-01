package net.filemaid.web;

import java.io.Serializable;
import java.time.Instant;

public class Extra
implements Serializable {
    protected String type;
    protected String name;
    protected String site;
    protected String key;
    protected String language;
    protected Long size;
    protected Boolean official;
    protected Instant date;

    public Extra() {
    }

    public Extra(String string, String string2, String string3, String string4, String string5, Long l, Boolean bl, Instant instant) {
        this.type = string;
        this.name = string2;
        this.site = string3;
        this.key = string4;
        this.language = string5;
        this.size = l;
        this.official = bl;
        this.date = instant;
    }

    public String getType() {
        return this.type;
    }

    public String getName() {
        return this.name;
    }

    public String getSite() {
        return this.site;
    }

    public String getKey() {
        return this.key;
    }

    public String getLanguage() {
        return this.language;
    }

    public Long getSize() {
        return this.size;
    }

    public Boolean isOfficial() {
        return this.official;
    }

    public Instant getDate() {
        return this.date;
    }

    public String toString() {
        return "[" + this.name + " | " + this.type + " | " + this.site + "::" + this.key + "]";
    }
}

