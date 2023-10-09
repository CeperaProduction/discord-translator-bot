package me.cepera.discord.bot.translator.config;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.netty.util.internal.ThrowableUtil;

public class ConfigReader {

    private static final Logger LOGGER = LogManager.getLogger(ConfigReader.class);

    private final ObjectMapper objectMapper;

    public ConfigReader() {
        objectMapper = new ObjectMapper();
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    private byte[] readConfigBytes(String name, Class<?> configClass) throws Exception{
        Path path = Paths.get("config", name+".json");
        byte[] bytes;
        if(Files.notExists(path)) {
            Files.createDirectories(path.getParent());
            bytes = objectMapper.writeValueAsBytes(configClass.newInstance());
            Files.write(path, bytes, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        }else {
            bytes = Files.readAllBytes(path);
        }
        if(bytes.length > 2) {
            return bytes;
        }
        return "{}".getBytes(StandardCharsets.UTF_8);
    }

    public <T> T readConfig(String name, Class<T> configClass) {
        try {
            return objectMapper.readValue(readConfigBytes(name, configClass), configClass);
        } catch (Exception e) {
            LOGGER.error("Error while read config {}: {}", e, ThrowableUtil.stackTraceToString(e));
            try {
                return configClass.newInstance();
            } catch (Exception ex) {
                throw new RuntimeException(e);
            }
        }
    }

}
