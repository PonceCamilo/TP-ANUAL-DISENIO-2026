package ar.utn.donatrack.donaciones;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ServicioDonacionesApp {
    public static void main(String[] args) {
        SpringApplication.run(ServicioDonacionesApp.class, args);
    }
}
