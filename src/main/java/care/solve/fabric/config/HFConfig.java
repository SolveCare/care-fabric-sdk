package care.solve.fabric.config;

import care.solve.fabric.service.ChannelService;
import org.hyperledger.fabric.sdk.ChaincodeEndorsementPolicy;
import org.hyperledger.fabric.sdk.ChaincodeID;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.EventHub;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.Orderer;
import org.hyperledger.fabric.sdk.Peer;
import org.hyperledger.fabric.sdk.User;
import org.hyperledger.fabric.sdk.exception.ChaincodeEndorsementPolicyParseException;
import org.hyperledger.fabric.sdk.exception.CryptoException;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.ProposalException;
import org.hyperledger.fabric.sdk.exception.TransactionException;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.hyperledger.fabric_ca.sdk.HFCAClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

@Configuration
public class HFConfig {

    private static final Logger logger = LoggerFactory.getLogger(HFConfig.class);

    private HFProperties hfProperties;
    private HFProperties.Organization clinicOrg;

    @Autowired
    public HFConfig(HFProperties hfProperties) {
        this.hfProperties = hfProperties;
        clinicOrg = hfProperties.getOrgs().get("clinic");
    }

    @Bean(name = "peerAdminHFClient")
    @Autowired
    public HFClient getPeerAdminHFClient(@Qualifier("peerAdminUser") User user) throws CryptoException, InvalidArgumentException {
        HFClient client = HFClient.createNewInstance();
        client.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
        client.setUserContext(user);

        return client;
    }

    @Bean(name = "sampleUserHFClient")
    @Autowired
    public HFClient getSampleUserHFClient(@Qualifier("sampleUser") User user) throws CryptoException, InvalidArgumentException {
        HFClient client = HFClient.createNewInstance();
        client.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
        client.setUserContext(user);

        return client;
    }

    @Bean(name = "hfcaAdminClient")
    public HFCAClient getHFCAAdminClient() throws MalformedURLException {
        HFCAClient hfcaAdminClient = HFCAClient.createNewInstance(clinicOrg.getCa().getUrl(), null);
        hfcaAdminClient.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());

        return hfcaAdminClient;
    }

    @Bean(name = "clinicPeers")
    public List<Peer> clinicPeers(HFClient peerAdminHFClient) {
        return clinicOrg.getPeers().stream()
                .map(peerConf -> {
                    try {
                        return peerAdminHFClient.newPeer(
                                peerConf.getName(),
                                peerConf.getGrpcUrl(),
                                constructPeerProperties(peerConf.getName(), peerConf.getTlsCertFile())
                        );
                    } catch (InvalidArgumentException e) {
                        String errMsg = String.format("Error while constructing peer: %s", peerConf);
                        logger.error(errMsg, e);
                        throw new RuntimeException(errMsg, e);
                    }
                }).collect(Collectors.toList());
    }

    @Bean(name = "orderer")
    public Orderer constructOrderer(HFClient peerAdminHFClient) throws InvalidArgumentException, IOException {
        HFProperties.Orderer orderer = clinicOrg.getOrderers().get(0);

        return peerAdminHFClient.newOrderer(
                orderer.getName(),
                orderer.getGrpcUrl(),
                constructOrdererProperties(orderer.getName(), orderer.getTlsCertFile())
        );
    }

    @Bean(name = "customEventHub")
    public EventHub constructEventHub(HFClient peerAdminHFClient) throws InvalidArgumentException, IOException {
        HFProperties.Peer peer = clinicOrg.getPeers().get(0);

        return peerAdminHFClient.newEventHub(
                peer.getName(),
                peer.getEventHub().getGrpcUrl(),
                constructPeerProperties(peer.getName(), peer.getTlsCertFile())
        );
    }

    public Properties constructPeerProperties(final String peerName, final String peerTLSCertFile) {
        URL resource = HFConfig.class.getResource(peerTLSCertFile);

        Properties properties = new Properties();
        properties.setProperty("pemFile", resource.toString());
        properties.setProperty("hostnameOverride", peerName);
        properties.setProperty("sslProvider", "openSSL");
        properties.setProperty("negotiationType", "TLS");

        return properties;
    }

    public Properties constructOrdererProperties(String ordererName, String ordererTlsCertFile) throws IOException {
        URL resource = HFConfig.class.getResource(ordererTlsCertFile);

        Properties properties = new Properties();
        properties.setProperty("pemFile", resource.toString());
        properties.setProperty("hostnameOverride", ordererName);
        properties.setProperty("sslProvider", "openSSL");
        properties.setProperty("negotiationType", "TLS");

        properties.setProperty("ordererWaitTimeMilliSecs", "20000");

        return properties;
    }

    @Bean(name = "chaincodeId")
    public ChaincodeID getChaincodeId() {
        final String CHAIN_CODE_NAME = "scheduleChaincode_go";
        final String CHAIN_CODE_PATH = "care.solve.schedule";
        final String CHAIN_CODE_VERSION = "3";

        return ChaincodeID.newBuilder().setName(CHAIN_CODE_NAME)
                .setVersion(CHAIN_CODE_VERSION)
                .setPath(CHAIN_CODE_PATH).build();
    }

    @Bean(name = "healthChannel")
    public Channel healthChannel(
            ChannelService channelService,
            @Qualifier("peerAdminUser") User peerAdminUser,
            @Qualifier("peerAdminHFClient") HFClient client,
            @Qualifier("clinicPeers") List<Peer> clinicPeers,
            @Qualifier("orderer") Orderer orderer,
            @Qualifier("customEventHub") EventHub eventHub) throws InvalidArgumentException, TransactionException, ProposalException, IOException {

        Channel channel;
        if (channelService.isChannelExists(hfProperties.getChannel().getName(), clinicPeers.get(0), client)) {
            channel = channelService.connectToChannel(hfProperties.getChannel().getName(), client, clinicPeers, orderer, eventHub);
        } else {
            channel = channelService.constructChannel(hfProperties.getChannel().getName(), client, peerAdminUser, clinicPeers, orderer, eventHub);
        }

        return channel;
    }

}
