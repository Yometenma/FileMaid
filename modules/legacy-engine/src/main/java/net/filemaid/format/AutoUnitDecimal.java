package net.filemaid.format;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;
import net.filemaid.format.StringBinding;
import net.filemaid.util.AlphanumComparator;

public abstract class AutoUnitDecimal
implements StringBinding,
Comparable<Object> {
    protected final double value;

    public AutoUnitDecimal(double d) {
        this.value = d;
    }

    public double getValue() {
        return this.value;
    }

    public long toLong() {
        return (long)this.value;
    }

    public double toDouble() {
        return this.value;
    }

    public int toInteger() {
        return (int)this.value;
    }

    public float toFloat() {
        return (float)this.value;
    }

    public BigDecimal toNumber() {
        return BigDecimal.valueOf(this.value);
    }

    @Override
    public int compareTo(Object object) {
        if (object instanceof Number) {
            Number number = (Number)object;
            return Double.compare(this.value, number.doubleValue());
        }
        if (object instanceof AutoUnitDecimal) {
            AutoUnitDecimal autoUnitDecimal = (AutoUnitDecimal)object;
            return Double.compare(this.value, autoUnitDecimal.value);
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
        return BigDecimal.valueOf(this.value / number.doubleValue());
    }

    public Number round(int n) {
        return BigDecimal.valueOf(this.value).setScale(n, RoundingMode.HALF_UP);
    }

    public String format(String string) {
        return this.format(string, Locale.ROOT);
    }

    public String format(String string, String string2) {
        return this.format(string, Locale.forLanguageTag(string2));
    }

    public String format(String string, Locale locale) {
        DecimalFormat decimalFormat = new DecimalFormat(string, new DecimalFormatSymbols(locale));
        return decimalFormat.format(this.value);
    }

    public String format(Map<Object, String> map) {
        for (Map.Entry<Object, String> entry : map.entrySet()) {
            if (!this.equals(entry.getKey())) continue;
            return entry.getValue();
        }
        return this.toString();
    }

    public boolean isFraction() {
        return this.value % 1.0 != 0.0;
    }

    public int getFractionDigits() {
        return this.isFraction() ? 3 : 0;
    }

    public String getNumber() {
        NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.ROOT);
        numberFormat.setGroupingUsed(true);
        int n = this.getFractionDigits();
        numberFormat.setMinimumFractionDigits(n);
        numberFormat.setMaximumFractionDigits(n);
        return numberFormat.format(this.value);
    }

    public abstract String getUnit();

    @Override
    public String toString() {
        return this.getNumber() + this.getUnit();
    }
}

