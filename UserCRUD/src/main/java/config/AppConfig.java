package config;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Map;

public class AppConfig {
    private static Map<String, Object> config;

    static {
        Yaml yaml = new Yaml();
        try (InputStream ios = AppConfig.class.getClassLoader().getResourceAsStream("application.yaml")) {
            config = yaml.load(ios);
        } catch (Exception e) {
            throw new RuntimeException("Не удалось загрузить application.yaml", e);
        }
    }

    public static String getDbProperties(String key) {
        Map<String, String> db = (Map<String, String>) config.get("database");
        String value = db.get(key);

        if (value != null && value.startsWith("${") && value.endsWith("}")) {
            String envVar = value.substring(2, value.length() - 1);
            return System.getenv(envVar);
        }
        return value;
    }
}
