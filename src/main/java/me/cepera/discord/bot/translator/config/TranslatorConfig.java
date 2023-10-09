package me.cepera.discord.bot.translator.config;

import java.util.Objects;

import io.netty.util.internal.StringUtil;

public class TranslatorConfig {

    private String apiKey;

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public int hashCode() {
        return Objects.hash(apiKey);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        TranslatorConfig other = (TranslatorConfig) obj;
        return Objects.equals(apiKey, other.apiKey);
    }

    @Override
    public String toString() {
        return "YandexTranslatorConfig [apiKey="+(StringUtil.isNullOrEmpty(apiKey) ? "none" : "<key>")+"]";
    }

}
