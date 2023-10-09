package me.cepera.discord.bot.translator.model;

import java.util.Objects;

public class TranslateLanguage {

    private final String code;

    private final String name;

    private final String countryCode;

    public TranslateLanguage(String code, String name, String countryCode) {
        this.code = code;
        this.name = name;
        this.countryCode = countryCode;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getCountryCode() {
        return countryCode;
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, countryCode, name);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        TranslateLanguage other = (TranslateLanguage) obj;
        return Objects.equals(code, other.code) && Objects.equals(countryCode, other.countryCode)
                && Objects.equals(name, other.name);
    }

    @Override
    public String toString() {
        return "TranslateLanguage [code=" + code + ", name=" + name + ", countryCode=" + countryCode + "]";
    }

}
