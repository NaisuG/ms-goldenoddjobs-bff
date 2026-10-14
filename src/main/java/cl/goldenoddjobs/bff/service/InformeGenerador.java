package cl.goldenoddjobs.bff.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;


@Component
public class InformeGenerador {

    private final JdbcTemplate jdbc;

    public InformeGenerador(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public String resumenGeneral() {
        long solicitudes = contar("solicitud");
        long postulaciones = contar("postulacion");
        return "Solicitudes en la plataforma: " + solicitudes + ". Postulaciones recibidas: " + postulaciones + ".";
    }

    private long contar(String tabla) {
        try {
            Long n = jdbc.queryForObject("select count(*) from " + tabla, Long.class);
            return n == null ? 0 : n;
        } catch (RuntimeException e) {
            return 0;
        }
    }
}
