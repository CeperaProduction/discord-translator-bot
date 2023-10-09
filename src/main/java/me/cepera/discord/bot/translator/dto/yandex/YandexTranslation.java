package me.cepera.discord.bot.translator.dto.yandex;

import java.util.Objects;

public class YandexTranslation {

    private String text;

    private String detectedLanguageCode;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getDetectedLanguageCode() {
        return detectedLanguageCode;
    }

    public void setDetectedLanguageCode(String detectedLanguageCode) {
        this.detectedLanguageCode = detectedLanguageCode;
    }

    @Override
    public int hashCode() {
        return Objects.hash(detectedLanguageCode, text);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        YandexTranslation other = (YandexTranslation) obj;
        return Objects.equals(detectedLanguageCode, other.detectedLanguageCode) && Objects.equals(text, other.text);
    }

    @Override
    public String toString() {
        return "YandexTranslation [text=" + text + ", detectedLanguageCode=" + detectedLanguageCode + "]";
    }

}
