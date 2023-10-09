package me.cepera.discord.bot.translator.model;

import java.util.Objects;

public class TranslatedText {

    private final String text;

    private final TranslateLanguage originalLanguage;

    public TranslatedText(String text, TranslateLanguage originalLanguage) {
        this.text = text;
        this.originalLanguage = originalLanguage;
    }

    public String getText() {
        return text;
    }

    public TranslateLanguage getOriginalLanguage() {
        return originalLanguage;
    }

    @Override
    public int hashCode() {
        return Objects.hash(originalLanguage, text);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        TranslatedText other = (TranslatedText) obj;
        return Objects.equals(originalLanguage, other.originalLanguage) && Objects.equals(text, other.text);
    }

    @Override
    public String toString() {
        return "TranslatedText [text=" + text + ", originalLanguage=" + originalLanguage + "]";
    }

}
