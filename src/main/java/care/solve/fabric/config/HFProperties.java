package care.solve.fabric.config;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.util.List;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "fabric")
@Data
public class HFProperties {

    @Data
    public static class CertificateAuthority {
        private String url;
    }

    @Data
    public static class EventHub {
        private String grpcUrl;
    }

    @Data
    public static class User {
        private String keystoreFile;
        private String certFile;
    }

    @Data
    public static class Peer {
        private String name;
        private String grpcUrl;
        private String tlsCertFile;
        private EventHub eventHub;
    }

    @Data
    public static class Orderer {
        private String name;
        private String grpcUrl;
        private String tlsCertFile;
    }

    @Data
    public static class Organization {
        private String name;
        private String mspId;
        private User admin;
        private List<User> users;
        private List<Peer> peers;
        private CertificateAuthority ca;
    }

    @Data
    public static class Channel {
        private String name;
        private String genesisBlockFile;
    }

    private Map<String, Organization> orgs;
    private List<Orderer> orderers;
    private Channel privateChannel;
    private Channel generalChannel;
    private String endorsementPolicy;
}
