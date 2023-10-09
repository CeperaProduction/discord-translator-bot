package me.cepera.discord.bot.translator.dto.yandex;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class YandexTranslateRequest {

    private String sourceLanguageCode;

    private String targetLanguageCode;

    private String format;

    private List<String> texts = new ArrayList<>();

    private Boolean speller;

    public String getSourceLanguageCode() {
        return sourceLanguageCode;
    }

    public void setSourceLanguageCode(String sourceLanguageCode) {
        this.sourceLanguageCode = sourceLanguageCode;
    }

    public String getTargetLanguageCode() {
        return targetLanguageCode;
    }

    public void setTargetLanguageCode(String targetLanguageCode) {
        this.targetLanguageCode = targetLanguageCode;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public List<String> getTexts() {
        return texts;
    }

    public void setTexts(List<String> texts) {
        this.texts = texts;
    }

    public Boolean getSpeller() {
        return speller;
    }

    public void setSpeller(Boolean speller) {
        this.speller = speller;
    }

    @Override
    public int hashCode() {
        return Objects.hash(format, sourceLanguageCode, speller, targetLanguageCode, texts);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        YandexTranslateRequest other = (YandexTranslateRequest) obj;
        return Objects.equals(format, other.format) && Objects.equals(sourceLanguageCode, other.sourceLanguageCode)
                && Objects.equals(speller, other.speller)
                && Objects.equals(targetLanguageCode, other.targetLanguageCode) && Objects.equals(texts, other.texts);
    }

    @Override
    public String toString() {
        return "YandexTranslateRequest [sourceLanguageCode=" + sourceLanguageCode + ", targetLanguageCode="
                + targetLanguageCode + ", format=" + format + ", texts=" + texts + ", speller=" + speller + "]";
    }

}
