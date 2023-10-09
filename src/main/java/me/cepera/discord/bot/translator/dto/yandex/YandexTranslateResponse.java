package me.cepera.discord.bot.translator.dto.yandex;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class YandexTranslateResponse extends YandexApiResponse{

    private List<YandexTranslation> translations = new ArrayList<YandexTranslation>();

    public List<YandexTranslation> getTranslations() {
        return translations;
    }

    public void setTranslations(List<YandexTranslation> translations) {
        this.translations = translations;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Objects.hash(translations);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        YandexTranslateResponse other = (YandexTranslateResponse) obj;
        return Objects.equals(translations, other.translations);
    }

    @Override
    public String toString() {
        return "YandexTranslateResponse [translations=" + translations + ", code=" + code + ", message=" + message
                + "]";
    }

}
