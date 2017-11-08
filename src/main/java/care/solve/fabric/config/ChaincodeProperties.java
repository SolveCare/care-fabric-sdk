package care.solve.fabric.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:chaincode.properties")
@ConfigurationProperties(prefix = "chaincode")
@Data
public class ChaincodeProperties {

    private String baseDir;
    private String sourceDir;
    private String endorsementPolicyFile;

}
