package care.solve.fabric;

import care.solve.fabric.config.HFProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
@EnableConfigurationProperties
public class App {
    public static void main( String[] args ) {
        ConfigurableApplicationContext context = SpringApplication.run(App.class, args);

        HFProperties hfProperties = context.getBean(HFProperties.class);

        System.out.println(hfProperties);
    }
}
