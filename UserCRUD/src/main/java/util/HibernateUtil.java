package util;

import config.AppConfig;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

public class HibernateUtil {
    private static SessionFactory sessionFactory;

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (sessionFactory != null) {
                System.out.println("Закрытие пула соединений Hibernate...");
                sessionFactory.close();
            }
        }));
    }

    public static synchronized SessionFactory getSessionFactory() {
        if (sessionFactory == null) {
            try {
                Configuration config = new Configuration().configure("hibernate.cfg.xml");

                String url = AppConfig.getDbProperties("url");
                if (!url.contains("reWriteBatchedInserts=true")) {
                    url += (url.contains("?") ? "&" : "?") + "reWriteBatchedInserts=true";
                }
                config.setProperty("hibernate.connection.url", url);
                config.setProperty("hibernate.connection.username", AppConfig.getDbProperties("username"));
                config.setProperty("hibernate.connection.password", AppConfig.getDbProperties("password"));
                config.setProperty("hibernate.connection.driver_class", AppConfig.getDbProperties("driver"));

                config.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");

                sessionFactory = config.buildSessionFactory();
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("Ошибка инициализации Hibernate");
            }
        }
        return sessionFactory;
    }

    // Метод для тестов, чтобы "сбросить" состояние между запусками
    public static void reset() {
        if (sessionFactory != null) {
            sessionFactory.close();
            sessionFactory = null;
        }
    }
}
