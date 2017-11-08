package care.solve.fabric.service;

import care.solve.fabric.config.HFProperties;
import org.apache.commons.io.IOUtils;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.ChannelConfiguration;
import org.hyperledger.fabric.sdk.EventHub;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.Orderer;
import org.hyperledger.fabric.sdk.Peer;
import org.hyperledger.fabric.sdk.User;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.ProposalException;
import org.hyperledger.fabric.sdk.exception.TransactionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URL;
import java.util.List;

@Service
public class ChannelService {

    private HFProperties hfProperties;

    @Autowired
    public ChannelService(HFProperties hfProperties) {
        this.hfProperties = hfProperties;
    }

    public boolean isChannelExists(String channelName, Peer peer, HFClient client) throws ProposalException, InvalidArgumentException {
        return client.queryChannels(peer).contains(channelName);
    }

    public Channel connectToChannel(
            String channelName,
            HFClient client,
            List<Peer> peers,
            Orderer orderer,
            EventHub eventHub) throws InvalidArgumentException, TransactionException, ProposalException {
        Channel newChannel = client.newChannel(channelName);

        newChannel.addOrderer(orderer);
        peers.forEach(peer -> {
            try {
                newChannel.addPeer(peer);
            } catch (InvalidArgumentException e) {
                e.printStackTrace();
            }
        });
        newChannel.addEventHub(eventHub);

        newChannel.initialize();

        return newChannel;
    }

    public Channel constructChannel(String channelName, HFClient client, User user, List<Peer> peers, Orderer orderer, EventHub eventHub) throws IOException, InvalidArgumentException, TransactionException, ProposalException {
        URL resource = ChannelService.class.getResource(hfProperties.getChannel().getGenesisBlockFile());
        byte[] bytes = IOUtils.toByteArray(resource);
        ChannelConfiguration channelConfiguration = new ChannelConfiguration(bytes);

        Channel newChannel = client.newChannel(
                channelName,
                orderer,
                channelConfiguration,
                client.getChannelConfigurationSignature(channelConfiguration, user)
        );
        newChannel.addEventHub(eventHub);
        peers.forEach(peer -> {
            try {
                newChannel.joinPeer(peer);
            } catch (ProposalException e) {
                e.printStackTrace();
            }
        });
        newChannel.initialize();

        return newChannel;
    }
}
