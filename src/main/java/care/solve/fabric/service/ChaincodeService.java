package care.solve.fabric.service;

import care.solve.fabric.config.ChaincodeProperties;
import care.solve.fabric.entity.ChaincodeMeta;
import com.google.common.collect.ImmutableSet;
import org.hyperledger.fabric.protos.peer.Query;
import org.hyperledger.fabric.sdk.ChaincodeEndorsementPolicy;
import org.hyperledger.fabric.sdk.ChaincodeID;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.InstallProposalRequest;
import org.hyperledger.fabric.sdk.InstantiateProposalRequest;
import org.hyperledger.fabric.sdk.Orderer;
import org.hyperledger.fabric.sdk.Peer;
import org.hyperledger.fabric.sdk.ProposalResponse;
import org.hyperledger.fabric.sdk.UpgradeProposalRequest;
import org.hyperledger.fabric.sdk.exception.ChaincodeEndorsementPolicyParseException;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.ProposalException;
import org.rauschig.jarchivelib.Archiver;
import org.rauschig.jarchivelib.ArchiverFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;


@Service
public class ChaincodeService {

    private ChaincodeProperties chaincodeProperties;

    @Autowired
    public ChaincodeService(ChaincodeProperties chaincodeProperties) {
        this.chaincodeProperties = chaincodeProperties;
    }

    public void installChaincode(HFClient client, ChaincodeMeta chaincodeMeta, Collection<Peer> peers, File tarGzFile) throws InvalidArgumentException, ProposalException, IOException {
        extractChaincode(tarGzFile);
        ChaincodeID chaincodeID = ChaincodeID.newBuilder()
                .setName(chaincodeMeta.getName())
                .setPath(chaincodeMeta.getPath())
                .setVersion(chaincodeMeta.getVersion())
                .build();
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

    public void instantiateChaincode(HFClient client, ChaincodeMeta chaincodeMeta, Channel channel, Orderer orderer) throws InvalidArgumentException, IOException, ChaincodeEndorsementPolicyParseException, ProposalException, ExecutionException, InterruptedException {
        InstantiateProposalRequest instantiateProposalRequest = client.newInstantiationProposalRequest();
        ChaincodeID chaincodeID = ChaincodeID.newBuilder()
                .setName(chaincodeMeta.getName())
                .setPath(chaincodeMeta.getPath())
                .setVersion(chaincodeMeta.getVersion())
                .build();
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

        channel.sendTransaction(proposalResponses, ImmutableSet.of(orderer)).get();
    }

    public void upgradeChaincode(HFClient client, ChaincodeMeta chaincodeMeta, Channel channel, Orderer orderer) throws InvalidArgumentException, ProposalException, IOException, ExecutionException, InterruptedException {
        UpgradeProposalRequest upgradeProposalRequest = client.newUpgradeProposalRequest();
        upgradeProposalRequest.setProposalWaitTime(20000L);
        ChaincodeID chaincodeID = ChaincodeID.newBuilder()
                .setName(chaincodeMeta.getName())
                .setPath(chaincodeMeta.getPath())
                .setVersion(chaincodeMeta.getVersion())
                .build();
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
                System.out.println(String.format("Successful upgrade proposal response Txid: %s from peer %s", response.getTransactionID(), response.getPeer().getName()));
            } else {
                System.out.println(String.format("FAILED upgrade proposal response Txid: %s from peer %s", response.getTransactionID(), response.getPeer().getName()));
            }
        }

        channel.sendTransaction(proposalResponses, ImmutableSet.of(orderer)).get();
    }

    public ChaincodeID queryLatestChaincodeId(Channel channel) { //todo filter by name
        try {
            List<Query.ChaincodeInfo> chaincodeInfos = channel.queryInstantiatedChaincodes(channel.getPeers().iterator().next());
            List<Query.ChaincodeInfo> sortedChaincodeInfos = chaincodeInfos.stream()
                    .sorted(Comparator.comparing(Query.ChaincodeInfo::getVersion).reversed())
                    .collect(Collectors.toList());
            Query.ChaincodeInfo latestChaincodeInfo = sortedChaincodeInfos.get(0);

            return ChaincodeID.newBuilder()
                    .setName(latestChaincodeInfo.getName())
                    .setPath(latestChaincodeInfo.getPath())
                    .setVersion(latestChaincodeInfo.getVersion())
                    .build();
        } catch (InvalidArgumentException | ProposalException e) {
            throw new RuntimeException(e);
        }
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


