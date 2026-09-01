package net.filemaid.format;

import java.util.Map;
import net.filemaid.format.ExpressionFormatMethods;

public interface StringBinding
extends CharSequence {
    default public String lower() {
        return ExpressionFormatMethods.lower(this.toString());
    }

    default public String upper() {
        return ExpressionFormatMethods.upper(this.toString());
    }

    default public String replaceAll(String string) {
        return ExpressionFormatMethods.replaceAll(this.toString(), string);
    }

    default public String removeAll(String string) {
        return ExpressionFormatMethods.removeAll(this.toString(), string);
    }

    default public String space(String string) {
        return ExpressionFormatMethods.space(this.toString(), string);
    }

    default public String upperInitial() {
        return ExpressionFormatMethods.upperInitial(this.toString());
    }

    default public String lowerTrail() {
        return ExpressionFormatMethods.lowerTrail(this.toString());
    }

    default public String before(String string) {
        return ExpressionFormatMethods.before(this.toString(), string);
    }

    default public String after(String string) {
        return ExpressionFormatMethods.after(this.toString(), string);
    }

    default public String replace(CharSequence charSequence, CharSequence charSequence2) {
        return this.toString().replace(charSequence, charSequence2);
    }

    default public String replace(Map<?, ?> map) {
        return ExpressionFormatMethods.replace(this.toString(), map);
    }

    @Override
    default public int length() {
        return this.toString().length();
    }

    @Override
    default public char charAt(int n) {
        return this.toString().charAt(n);
    }

    @Override
    default public CharSequence subSequence(int n, int n2) {
        return this.toString().subSequence(n, n2);
    }

    default public String negative(String string) {
        return "-" + string;
    }
}

