package care.solve.fabric.entity;

import care.solve.fabric.config.HFConfig;
import care.solve.fabric.config.HFProperties;
import lombok.Builder;
import lombok.Data;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.Peer;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;

import java.net.URL;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

@Data
@Builder
public class SampleOrganization {
    private String name;
    private List<HFProperties.Peer> peers;
    private SampleUser adminUser;
    private SampleUser user;
    private HFClient peerAdminHFClient;

    public List<Peer> getPeers() {
        return peers.stream()
                .map(peerConf -> this.confToPeer(peerAdminHFClient, peerConf))
                .collect(Collectors.toList());
    }


    private Peer confToPeer(HFClient peerAdminHFClient, HFProperties.Peer peerProps) {
        try {
            return peerAdminHFClient.newPeer(
                    peerProps.getName(),
                    peerProps.getGrpcUrl(),
                    constructPeerProperties(peerProps.getName(), peerProps.getTlsCertFile())
            );
        } catch (InvalidArgumentException e) {
            String errMsg = String.format("Error while constructing peer: %s", peerProps);
            throw new RuntimeException(errMsg, e);
        }
    }


    private Properties constructPeerProperties(final String peerName, final String peerTLSCertFile) {
        URL resource = HFConfig.class.getResource(peerTLSCertFile);

        Properties properties = new Properties();
        properties.setProperty("pemFile", resource.toString());
        properties.setProperty("hostnameOverride", peerName);
        properties.setProperty("sslProvider", "openSSL");
        properties.setProperty("negotiationType", "TLS");

        return properties;
    }
}
