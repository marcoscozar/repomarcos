package com.ggm.ad.examen_ut6;

import com.ggm.ad.examen_ut6.model.Actor;
import com.ggm.ad.examen_ut6.model.Pelicula;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

public class HibernateUtil {
    private static final SessionFactory sessionFactory = buildSessionFactoryProgrammatically();

    private static SessionFactory buildSessionFactory() {
        try {
            // Cargar la configuración desde hibernate.cfg.xml
            return new Configuration().configure().buildSessionFactory();
        } catch (Throwable ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }
    private static SessionFactory buildSessionFactoryProgrammatically() {
        try {
            // Crear una configuración de Hibernate sin hibernate.cfg.xml
            Configuration configuration = new Configuration();

            // Configuración de la conexión a la base de datos
            configuration.setProperty("hibernate.connection.driver_class", "com.mysql.cj.jdbc.Driver");
            configuration.setProperty("hibernate.connection.url", "jdbc:mysql://localhost:3306/ordinaria_ut6");
            configuration.setProperty("hibernate.connection.username", "root");
            configuration.setProperty("hibernate.connection.password", "");

            // Dialecto de Hibernate y otras propiedades
            configuration.setProperty("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");

            configuration.setProperty("hibernate.show_sql", "false");
            configuration.setProperty("hibernate.format_sql", "true");
            configuration.setProperty("hibernate.hbm2ddl.auto", "update");

            // Registrar clases de entidad
//            configuration.addAnnotatedClass(Client.class);
            configuration.addAnnotatedClass(Actor.class);
            configuration.addAnnotatedClass(Pelicula.class);
            // Agrega todas las clases de entidad aquí, si tienes más de una

            // Construir y devolver la SessionFactory
            return configuration.buildSessionFactory();
        } catch (Throwable ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    public static SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    public static void shutdown() {
        getSessionFactory().close();
    }
}

