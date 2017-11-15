package care.solve.fabric.service;

import care.solve.fabric.config.ChaincodeProperties;
import care.solve.fabric.config.ChaincodeUpdateEvent;
import com.google.common.collect.ImmutableSet;
import org.hyperledger.fabric.sdk.*;
import org.hyperledger.fabric.sdk.exception.ChaincodeEndorsementPolicyParseException;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.ProposalException;
import org.rauschig.jarchivelib.Archiver;
import org.rauschig.jarchivelib.ArchiverFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;


@Service
public class ChaincodeService {

    private ChaincodeProperties chaincodeProperties;

    private ApplicationEventPublisher applicationEventPublisher;

    private ChaincodeIDFactory chaincodeIDFactory;

    @Autowired
    public ChaincodeService(ChaincodeProperties chaincodeProperties,
                            ApplicationEventPublisher applicationEventPublisher,
                            ChaincodeIDFactory chaincodeIDFactory) {
        this.chaincodeProperties = chaincodeProperties;
        this.applicationEventPublisher = applicationEventPublisher;
        this.chaincodeIDFactory = chaincodeIDFactory;
    }

    public void installChaincode(HFClient client, Collection<Peer> peers, File tarGzFile) throws InvalidArgumentException, ProposalException, IOException {
        extractChaincode(tarGzFile);
        ChaincodeID chaincodeID = chaincodeIDFactory.getNextChaincode();
        InstallProposalRequest installProposalRequest = client.newInstallProposalRequest();
        installProposalRequest.setChaincodeID(chaincodeID);

        installProposalRequest.setChaincodeSourceLocation(new File(chaincodeProperties.getSourceDir()));
        installProposalRequest.setChaincodeVersion(chaincodeID.getVersion());

        ChaincodeEndorsementPolicy endorsementPolicy = getChaincodeEndorsementPolicy();

        if (endorsementPolicy != null) {
            installProposalRequest.setChaincodeEndorsementPolicy(endorsementPolicy);
        }

        Collection<ProposalResponse> proposalResponses = client.sendInstallProposal(installProposalRequest, peers);

        for (ProposalResponse response : proposalResponses) {
            if (response.getStatus() == ProposalResponse.Status.SUCCESS) {
                System.out.println(String.format("Successful install proposal response Txid: %s from peer %s", response.getTransactionID(), response.getPeer().getName()));
            } else {
                System.out.println(String.format("FAILED to install proposal response Txid: %s from peer %s", response.getTransactionID(), response.getPeer().getName()));
            }
        }
    }

    public void instantiateChaincode(HFClient client, Channel channel, Orderer orderer) throws InvalidArgumentException, IOException, ChaincodeEndorsementPolicyParseException, ProposalException {
        InstantiateProposalRequest instantiateProposalRequest = client.newInstantiationProposalRequest();
        ChaincodeID chaincodeID = chaincodeIDFactory.getNextChaincode();
        instantiateProposalRequest.setProposalWaitTime(20000L);
        instantiateProposalRequest.setChaincodeID(chaincodeID);
        instantiateProposalRequest.setFcn("init");
        instantiateProposalRequest.setArgs(new String[]{"someArg", "0"});

        Map<String, byte[]> tm = new HashMap<>();
        tm.put("HyperLedgerFabric", "InstantiateProposalRequest:JavaSDK".getBytes(UTF_8));
        tm.put("method", "InstantiateProposalRequest".getBytes(UTF_8));
        instantiateProposalRequest.setTransientMap(tm);

        Collection<ProposalResponse> proposalResponses = channel.sendInstantiationProposal(instantiateProposalRequest, channel.getPeers());

        for (ProposalResponse response : proposalResponses) {
            if (response.isVerified() && response.getStatus() == ProposalResponse.Status.SUCCESS) {
                System.out.println(String.format("Successful instantiate proposal response Txid: %s from peer %s", response.getTransactionID(), response.getPeer().getName()));
            } else {
                System.out.println(String.format("FAILED instantiate proposal response Txid: %s from peer %s", response.getTransactionID(), response.getPeer().getName()));
            }
        }

        channel.sendTransaction(proposalResponses, ImmutableSet.of(orderer));
        applicationEventPublisher.publishEvent(new ChaincodeUpdateEvent(this));
    }

    public void upgradeChaincode(HFClient client, Channel channel, Orderer orderer) throws InvalidArgumentException, ProposalException, IOException {
        UpgradeProposalRequest upgradeProposalRequest = client.newUpgradeProposalRequest();
        upgradeProposalRequest.setProposalWaitTime(20000L);
        ChaincodeID chaincodeID = chaincodeIDFactory.getNextChaincode();
        upgradeProposalRequest.setChaincodeID(chaincodeID);
        upgradeProposalRequest.setFcn("init");
        upgradeProposalRequest.setArgs(new String[]{"someArg", "0"});

        Map<String, byte[]> tm = new HashMap<>();
        tm.put("HyperLedgerFabric", "InstantiateProposalRequest:JavaSDK".getBytes(UTF_8));
        tm.put("method", "InstantiateProposalRequest".getBytes(UTF_8));
        upgradeProposalRequest.setTransientMap(tm);
        Collection<ProposalResponse> proposalResponses = channel.sendUpgradeProposal(upgradeProposalRequest, channel.getPeers());

        for (ProposalResponse response : proposalResponses) {
            if (response.isVerified() && response.getStatus() == ProposalResponse.Status.SUCCESS) {
                System.out.println(String.format("Successful instantiate proposal response Txid: %s from peer %s", response.getTransactionID(), response.getPeer().getName()));
            } else {
                System.out.println(String.format("FAILED instantiate proposal response Txid: %s from peer %s", response.getTransactionID(), response.getPeer().getName()));
            }
        }

        channel.sendTransaction(proposalResponses, ImmutableSet.of(orderer));
        applicationEventPublisher.publishEvent(new ChaincodeUpdateEvent(this));
    }

    private void extractChaincode(File tarGzFile) {
        File destination = new File(chaincodeProperties.getBaseDir() + "/src");
        Archiver archiver = ArchiverFactory.createArchiver("tar", "gz");

        try {
            archiver.extract(tarGzFile, destination);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private ChaincodeEndorsementPolicy getChaincodeEndorsementPolicy () {
        File config = new File(chaincodeProperties.getEndorsementPolicyFile());

        if (!config.exists()) {
            System.out.println("Endorsement policy config not set.");

            return null;
        }

        ChaincodeEndorsementPolicy chaincodeEndorsementPolicy = new ChaincodeEndorsementPolicy();

        try {
            chaincodeEndorsementPolicy.fromYamlFile(config);
        } catch (IOException | ChaincodeEndorsementPolicyParseException e) {
            e.printStackTrace();
            System.out.println("Cannot obtain endorsement policies from file.");

            return null;
        }

        return chaincodeEndorsementPolicy;
    }
}


