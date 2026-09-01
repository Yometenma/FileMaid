package net.filemaid.format;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.stream.Stream;
import net.filemaid.format.StringBinding;
import net.filemaid.util.AlphanumComparator;

public class AutoScaleInteger
implements StringBinding,
Comparable<Object> {
    protected final long value;
    protected final Scale scale;

    public AutoScaleInteger(long l, Scale scale) {
        this.value = l;
        this.scale = scale;
    }

    public long getValue() {
        return this.value;
    }

    public Scale getScale() {
        return this.scale;
    }

    public AutoScaleInteger scale(Scale scale) {
        return new AutoScaleInteger(this.value, scale);
    }

    public AutoScaleInteger getG() {
        return this.scale(Scale.G);
    }

    public AutoScaleInteger getM() {
        return this.scale(Scale.M);
    }

    public AutoScaleInteger getK() {
        return this.scale(Scale.K);
    }

    public long toLong() {
        return this.value / (long)this.scale.factor;
    }

    public double toDouble() {
        return (double)this.value / (double)this.scale.factor;
    }

    public int toInteger() {
        return (int)this.toLong();
    }

    public float toFloat() {
        return (float)this.toDouble();
    }

    public BigDecimal toNumber() {
        return BigDecimal.valueOf(this.toDouble());
    }

    @Override
    public int compareTo(Object object) {
        if (object instanceof Integer) {
            Number number = (Number)object;
            return Integer.compare(this.toInteger(), number.intValue());
        }
        if (object instanceof Number) {
            Number number = (Number)object;
            return Double.compare(this.toDouble(), number.doubleValue());
        }
        if (object instanceof AutoScaleInteger) {
            AutoScaleInteger autoScaleInteger = (AutoScaleInteger)object;
            return Long.compare(this.value, autoScaleInteger.value);
        }
        return AlphanumComparator.getInstance().compare(this.toString(), object.toString());
    }

    public boolean equals(Object object) {
        return object instanceof Number && this.compareTo(object) == 0 || object instanceof CharSequence && this.toString().equals(object);
    }

    public Object asType(Class<?> clazz) {
        if (clazz == Integer.TYPE || clazz == Integer.class) {
            return this.toInteger();
        }
        if (clazz == Long.TYPE || clazz == Long.class) {
            return this.toLong();
        }
        if (clazz == Float.TYPE || clazz == Float.class) {
            return Float.valueOf(this.toFloat());
        }
        if (clazz == Double.TYPE || clazz == Double.class) {
            return this.toDouble();
        }
        if (clazz == Number.class) {
            return this.toNumber();
        }
        if (clazz == String.class) {
            return this.toString();
        }
        return clazz.cast(this);
    }

    public Number div(Number number) {
        return BigDecimal.valueOf(this.toDouble() / number.doubleValue());
    }

    public Number round(int n) {
        return BigDecimal.valueOf(this.toDouble()).setScale(n, RoundingMode.HALF_UP);
    }

    public String format(String string) {
        return this.format(string, Locale.ROOT);
    }

    public String format(String string, String string2) {
        return this.format(string, Locale.forLanguageTag(string2));
    }

    public String format(String string, Locale locale) {
        DecimalFormat decimalFormat = new DecimalFormat(string, new DecimalFormatSymbols(locale));
        return decimalFormat.format(this.toDouble());
    }

    public String getUnit(Scale scale) {
        return scale == Scale.AUTO ? null : scale.name();
    }

    @Override
    public String toString() {
        return this.scale == Scale.AUTO ? this.toString(Scale.find(this.value)) : this.toString(this.scale);
    }

    public String toString(Scale scale) {
        String string = this.getUnit(scale);
        if (string == null) {
            return String.format(Locale.ROOT, "%,d", this.value);
        }
        double d = (double)this.value / (double)scale.factor;
        if (d < 9.5 && scale.factor > 1) {
            return String.format(Locale.ROOT, "%,.1f %s", d, string);
        }
        return String.format(Locale.ROOT, "%,.0f %s", d, string);
    }

    public static enum Scale {
        AUTO(1),
        K(1000),
        M(1000000),
        G(1000000000);

        public final int factor;

        private Scale(int n2) {
            this.factor = n2;
        }

        public static Scale find(long l) {
            return Stream.of(G, M, K).filter(scale -> l >= (long)scale.factor).findFirst().orElse(AUTO);
        }
    }
}

