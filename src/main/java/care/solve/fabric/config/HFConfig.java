package care.solve.fabric.config;

import care.solve.fabric.entity.SampleOrganization;
import care.solve.fabric.entity.SampleStore;
import care.solve.fabric.entity.SampleUser;
import care.solve.fabric.service.ChannelService;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.EventHub;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.Orderer;
import org.hyperledger.fabric.sdk.Peer;
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
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

@Configuration
public class HFConfig {

    private static final Logger logger = LoggerFactory.getLogger(HFConfig.class);

    private HFProperties hfProperties;

    @Autowired
    public HFConfig(HFProperties hfProperties) {
        this.hfProperties = hfProperties;
    }

//    ***********************
//    * HF Clients
//    ***********************

    @Bean(name = "individualPeerAdminHFClient")
    @Autowired
    public HFClient getIndividualPeerAdminHFClient(@Qualifier("individualPeerAdmin") SampleUser individualPeerAdmin) throws CryptoException, InvalidArgumentException {
        HFClient client = HFClient.createNewInstance();
        client.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
        client.setUserContext(individualPeerAdmin);

        return client;
    }

    @Bean(name = "insurerPeerAdminHFClient")
    @Autowired
    public HFClient getInsurerPeerAdminHFClient(@Qualifier("insurerPeerAdmin") SampleUser insurerPeerAdmin) throws CryptoException, InvalidArgumentException {
        HFClient client = HFClient.createNewInstance();
        client.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
        client.setUserContext(insurerPeerAdmin);

        return client;
    }

    @Bean(name = "serviceProviderPeerAdminHFClient")
    @Autowired
    public HFClient getServiceProviderPeerAdminHFClient(@Qualifier("serviceProviderPeerAdmin") SampleUser serviceProviderPeerAdmin) throws CryptoException, InvalidArgumentException {
        HFClient client = HFClient.createNewInstance();
        client.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
        client.setUserContext(serviceProviderPeerAdmin);

        return client;
    }

//    ***********************
//    * HF CA Clients
//    ***********************

    @Bean(name = "individualHfcaAdminClient")
    public HFCAClient getIndividualHFCAClient() throws MalformedURLException {
        HFProperties.Organization organization = hfProperties.getOrgs().get("individual");

        HFCAClient hfcaAdminClient = HFCAClient.createNewInstance(organization.getCa().getUrl(), null);
        hfcaAdminClient.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());

        return hfcaAdminClient;
    }

    @Bean(name = "insurerHfcaAdminClient")
    public HFCAClient getInsurerHFCAClient() throws MalformedURLException {
        HFProperties.Organization organization = hfProperties.getOrgs().get("insurer");

        HFCAClient hfcaAdminClient = HFCAClient.createNewInstance(organization.getCa().getUrl(), null);
        hfcaAdminClient.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());

        return hfcaAdminClient;
    }

    @Bean(name = "serviceproviderHfcaAdminClient")
    public HFCAClient getServiceProviderHFCAClient() throws MalformedURLException {
        HFProperties.Organization organization = hfProperties.getOrgs().get("serviceprovider");

        HFCAClient hfcaAdminClient = HFCAClient.createNewInstance(organization.getCa().getUrl(), null);
        hfcaAdminClient.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());

        return hfcaAdminClient;
    }

//    ***********************
//    * Organizations
//    ***********************

    @Bean("individualOrganization")
    public SampleOrganization individualOrganization(
            @Qualifier("individualPeerAdminHFClient") HFClient peerAdminHFClient,
            @Qualifier("defaultStore") SampleStore sampleStore,
            @Qualifier("individualPeerAdmin") SampleUser individualPeerAdmin) throws IOException, NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException {
        HFProperties.Organization organization = hfProperties.getOrgs().get("individual");
        List<Peer> peers = organization.getPeers().stream()
                .map(peerConf -> this.confToPeer(peerAdminHFClient, peerConf))
                .collect(Collectors.toList());
        
        String userKeystoreFile = organization.getUsers().get(0).getKeystoreFile();
        String userCertFile = organization.getUsers().get(0).getCertFile();

        SampleUser user = confToUser(organization, userKeystoreFile, userCertFile, sampleStore, "user");

        return SampleOrganization.builder()
                .name(organization.getName())
                .peers(peers)
                .adminUser(individualPeerAdmin)
                .user(user)
                .build();
    }

    @Bean("insurerOrganization")
    public SampleOrganization insurerOrganization(
            @Qualifier("insurerPeerAdminHFClient") HFClient peerAdminHFClient,
            @Qualifier("defaultStore") SampleStore sampleStore,
            @Qualifier("insurerPeerAdmin") SampleUser insurerPeerAdmin) throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException, IOException {
        HFProperties.Organization organization = hfProperties.getOrgs().get("insurer");
        List<Peer> peers = organization.getPeers().stream()
                .map(peerConf -> this.confToPeer(peerAdminHFClient, peerConf))
                .collect(Collectors.toList());
        
        String userKeystoreFile = organization.getUsers().get(0).getKeystoreFile();
        String userCertFile = organization.getUsers().get(0).getCertFile();

        SampleUser user = confToUser(organization, userKeystoreFile, userCertFile, sampleStore, "user");

        return SampleOrganization.builder()
                .name(organization.getName())
                .peers(peers)
                .adminUser(insurerPeerAdmin)
                .user(user)
                .build();
    }

    @Bean("serviceProviderOrganization")
    public SampleOrganization serviceProviderOrganization(
            @Qualifier("serviceProviderPeerAdminHFClient") HFClient peerAdminHFClient,
            @Qualifier("defaultStore") SampleStore sampleStore,
            @Qualifier("serviceProviderPeerAdmin") SampleUser serviceProviderPeerAdmin) throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException, IOException {
        HFProperties.Organization organization = hfProperties.getOrgs().get("serviceprovider");
        List<Peer> peers = organization.getPeers().stream()
                .map(peerConf -> this.confToPeer(peerAdminHFClient, peerConf))
                .collect(Collectors.toList());

        String userKeystoreFile = organization.getUsers().get(0).getKeystoreFile();
        String userCertFile = organization.getUsers().get(0).getCertFile();

        SampleUser user = confToUser(organization, userKeystoreFile, userCertFile, sampleStore, "user");

        return SampleOrganization.builder()
                .name(organization.getName())
                .peers(peers)
                .adminUser(serviceProviderPeerAdmin)
                .user(user)
                .build();
    }

//    ***********************
//    * Peer Admins
//    ***********************

    @Bean("individualPeerAdmin")
    public SampleUser individualPeerAdmin(@Qualifier("defaultStore") SampleStore sampleStore) throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException, IOException {
        HFProperties.Organization organization = hfProperties.getOrgs().get("individual");
        String peerAdminKeystoreFile = organization.getAdmin().getKeystoreFile();
        String peerAdminCertFile = organization.getAdmin().getCertFile();

        return confToUser(organization, peerAdminKeystoreFile, peerAdminCertFile, sampleStore, "peerAdmin");
    }

    @Bean("insurerPeerAdmin")
    public SampleUser insurerPeerAdmin(@Qualifier("defaultStore") SampleStore sampleStore) throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException, IOException {
        HFProperties.Organization organization = hfProperties.getOrgs().get("insurer");
        String peerAdminKeystoreFile = organization.getAdmin().getKeystoreFile();
        String peerAdminCertFile = organization.getAdmin().getCertFile();

        return confToUser(organization, peerAdminKeystoreFile, peerAdminCertFile, sampleStore, "peerAdmin");
    }

    @Bean("serviceProviderPeerAdmin")
    public SampleUser serviceProviderPeerAdmin(@Qualifier("defaultStore") SampleStore sampleStore) throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException, IOException {
        HFProperties.Organization organization = hfProperties.getOrgs().get("serviceprovider");
        String peerAdminKeystoreFile = organization.getAdmin().getKeystoreFile();
        String peerAdminCertFile = organization.getAdmin().getCertFile();

        return confToUser(organization, peerAdminKeystoreFile, peerAdminCertFile, sampleStore, "peerAdmin");
    }

//    ***********************
//    * Orderers
//    ***********************

    @Bean(name = "orderer")
    public Orderer constructOrderer(@Qualifier("individualPeerAdminHFClient") HFClient peerAdminHFClient) throws InvalidArgumentException, IOException {
        HFProperties.Orderer orderer = hfProperties.getOrderers().get(0);

        return peerAdminHFClient.newOrderer(
                orderer.getName(),
                orderer.getGrpcUrl(),
                constructOrdererProperties(orderer.getName(), orderer.getTlsCertFile())
        );
    }

//    ***********************
//    * EventHubs
//    ***********************

    @Bean(name = "individualEventHub")
    public EventHub individualEventHub(@Qualifier("individualPeerAdminHFClient") HFClient peerAdminHFClient) throws InvalidArgumentException, IOException {
        HFProperties.Organization organization = hfProperties.getOrgs().get("individual");

        HFProperties.Peer peer = organization.getPeers().get(0);
        return peerAdminHFClient.newEventHub(
                peer.getName(),
                peer.getEventHub().getGrpcUrl(),
                constructPeerProperties(peer.getName(), peer.getTlsCertFile())
        );
    }

    @Bean(name = "insurerEventHub")
    public EventHub insurerEventHub(@Qualifier("insurerPeerAdminHFClient") HFClient peerAdminHFClient) throws InvalidArgumentException, IOException {
        HFProperties.Organization organization = hfProperties.getOrgs().get("insurer");

        HFProperties.Peer peer = organization.getPeers().get(0);
        return peerAdminHFClient.newEventHub(
                peer.getName(),
                peer.getEventHub().getGrpcUrl(),
                constructPeerProperties(peer.getName(), peer.getTlsCertFile())
        );
    }

    @Bean(name = "serviceProviderEventHub")
    public EventHub serviceProviderEventHub(@Qualifier("serviceProviderPeerAdminHFClient") HFClient peerAdminHFClient) throws InvalidArgumentException, IOException {
        HFProperties.Organization organization = hfProperties.getOrgs().get("serviceprovider");

        HFProperties.Peer peer = organization.getPeers().get(0);
        return peerAdminHFClient.newEventHub(
                peer.getName(),
                peer.getEventHub().getGrpcUrl(),
                constructPeerProperties(peer.getName(), peer.getTlsCertFile())
        );
    }

//    ***********************
//    * Channels
//    ***********************

    @Bean("channelMap")
    public Map<String, Channel> channelMap() {
        return new HashMap<>();
    }

    @Bean(name = "generalChannel")
    public Channel generalChannel(
            ChannelService channelService,
            @Qualifier("individualPeerAdminHFClient") HFClient client,
            @Qualifier("individualOrganization") SampleOrganization individualOrganization,
            @Qualifier("insurerOrganization") SampleOrganization insurerOrganization,
            @Qualifier("serviceProviderOrganization") SampleOrganization serviceProviderOrganization,
            @Qualifier("orderer") Orderer orderer,
            @Qualifier("individualEventHub") EventHub individualEventHub,
            @Qualifier("insurerEventHub") EventHub insurerEventHub,
            @Qualifier("serviceProviderEventHub") EventHub serviceProviderEventHub,
            @Qualifier("channelMap") Map<String, Channel> channelMap) throws InvalidArgumentException, TransactionException, ProposalException, IOException {

        HFProperties.Channel generalChannelProps = hfProperties.getGeneralChannel();
        String generalChannelName = generalChannelProps.getName();

        List<Peer> peers = new ArrayList<>();
        peers.addAll(individualOrganization.getPeers());
        peers.addAll(insurerOrganization.getPeers());
        peers.addAll(serviceProviderOrganization.getPeers());

        List<EventHub> eventHubs = new ArrayList<>();
        eventHubs.add(individualEventHub);
//        eventHubs.add(insurerEventHub);
//        eventHubs.add(serviceProviderEventHub);

        Channel channel;
        if (channelService.isChannelExists(generalChannelName, individualOrganization.getPeers().get(0), client)) {
            channel = channelService.connectToChannel(generalChannelName, client, peers, orderer, eventHubs);
        } else {
            channel = channelService.constructChannel(generalChannelName, client, individualOrganization.getAdminUser(), peers, orderer, eventHubs, generalChannelProps.getGenesisBlockFile());
        }

        channelMap.put(generalChannelName, channel);
        return channel;
    }

    @Bean(name = "privateChannel")
    public Channel privateChannel(
            ChannelService channelService,
            @Qualifier("individualPeerAdminHFClient") HFClient client,
            @Qualifier("individualOrganization") SampleOrganization individualOrganization,
            @Qualifier("serviceProviderOrganization") SampleOrganization serviceProviderOrganization,
            @Qualifier("orderer") Orderer orderer,
            @Qualifier("individualEventHub") EventHub individualEventHub,
            @Qualifier("serviceProviderEventHub") EventHub serviceProviderEventHub,
            @Qualifier("channelMap") Map<String, Channel> channelMap) throws InvalidArgumentException, TransactionException, ProposalException, IOException {

        HFProperties.Channel privateChannelProps = hfProperties.getPrivateChannel();
        String privateChannelName = privateChannelProps.getName();

        List<Peer> peers = new ArrayList<>();
        peers.addAll(individualOrganization.getPeers());
        peers.addAll(serviceProviderOrganization.getPeers());

        List<EventHub> eventHubs = new ArrayList<>();
        eventHubs.add(individualEventHub);
//        eventHubs.add(serviceProviderEventHub);

        Channel channel;
        if (channelService.isChannelExists(privateChannelName, individualOrganization.getPeers().get(0), client)) {
            channel = channelService.connectToChannel(privateChannelName, client, peers, orderer, eventHubs);
        } else {
            channel = channelService.constructChannel(privateChannelName, client, individualOrganization.getAdminUser(), peers, orderer, eventHubs, privateChannelProps.getGenesisBlockFile());
        }

        channelMap.put(privateChannelName, channel);
        return channel;
    }

//    ***********************
//    * Helpers
//    ***********************

    private SampleUser confToUser(
            HFProperties.Organization organization,
            String peerAdminKeystoreFile,
            String peerAdminCertFile,
            SampleStore sampleStore,
            String userName) throws InvalidKeySpecException, NoSuchAlgorithmException, NoSuchProviderException, IOException {

        InputStream peerAdminKeystoreFileStream = new ClassPathResource(peerAdminKeystoreFile).getInputStream();
        InputStream peerAdminCertFileStream = new ClassPathResource(peerAdminCertFile).getInputStream();

        return sampleStore.getMember(
                userName,
                organization.getName(),
                organization.getMspId(),
                peerAdminKeystoreFileStream,
                peerAdminCertFileStream
        );
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
            logger.error(errMsg, e);
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

    private Properties constructOrdererProperties(String ordererName, String ordererTlsCertFile) throws IOException {
        URL resource = HFConfig.class.getResource(ordererTlsCertFile);

        Properties properties = new Properties();
        properties.setProperty("pemFile", resource.toString());
        properties.setProperty("hostnameOverride", ordererName);
        properties.setProperty("sslProvider", "openSSL");
        properties.setProperty("negotiationType", "TLS");

        properties.setProperty("ordererWaitTimeMilliSecs", "90000");

        return properties;
    }
}
