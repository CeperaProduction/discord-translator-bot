package me.cepera.discord.bot.translator.dto.yandex;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class YandexListLanguagesResponse {

    private List<YandexLanguage> languages = new ArrayList<>();

    public List<YandexLanguage> getLanguages() {
        return languages;
    }

    public void setLanguages(List<YandexLanguage> languages) {
        this.languages = languages;
    }

    @Override
    public int hashCode() {
        return Objects.hash(languages);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        YandexListLanguagesResponse other = (YandexListLanguagesResponse) obj;
        return Objects.equals(languages, other.languages);
    }

    @Override
    public String toString() {
        return "YandexListLanguagesResponse [languages=" + languages + "]";
    }

}
