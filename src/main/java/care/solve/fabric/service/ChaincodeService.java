package care.solve.fabric.service;

import care.solve.fabric.config.ChaincodeProperties;
import com.google.common.collect.ImmutableSet;
import org.hyperledger.fabric.sdk.BlockEvent;
import org.hyperledger.fabric.sdk.ChaincodeEndorsementPolicy;
import org.hyperledger.fabric.sdk.ChaincodeID;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.InstallProposalRequest;
import org.hyperledger.fabric.sdk.InstantiateProposalRequest;
import org.hyperledger.fabric.sdk.Orderer;
import org.hyperledger.fabric.sdk.Peer;
import org.hyperledger.fabric.sdk.ProposalResponse;
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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static java.nio.charset.StandardCharsets.UTF_8;


@Service
public class ChaincodeService {

    private ChaincodeProperties chaincodeProperties;

    @Autowired
    public ChaincodeService(ChaincodeProperties chaincodeProperties) {
        this.chaincodeProperties = chaincodeProperties;
    }

    public CompletableFuture<BlockEvent.TransactionEvent> instantiateChaincode(HFClient client, ChaincodeID chaincodeId, Channel channel, Orderer orderer, Collection<Peer> peers) throws InvalidArgumentException, IOException, ChaincodeEndorsementPolicyParseException, ProposalException {
        InstantiateProposalRequest instantiateProposalRequest = client.newInstantiationProposalRequest();
        instantiateProposalRequest.setProposalWaitTime(20000L);
        instantiateProposalRequest.setChaincodeID(chaincodeId);
        instantiateProposalRequest.setFcn("init");
        instantiateProposalRequest.setArgs(new String[]{"someArg", "0"});

        Map<String, byte[]> tm = new HashMap<>();
        tm.put("HyperLedgerFabric", "InstantiateProposalRequest:JavaSDK".getBytes(UTF_8));
        tm.put("method", "InstantiateProposalRequest".getBytes(UTF_8));
        instantiateProposalRequest.setTransientMap(tm);

        Collection<ProposalResponse> proposalResponses = channel.sendInstantiationProposal(instantiateProposalRequest, peers);

        for (ProposalResponse response : proposalResponses) {
            if (response.isVerified() && response.getStatus() == ProposalResponse.Status.SUCCESS) {
                System.out.println(String.format("Successful instantiate proposal response Txid: %s from peer %s", response.getTransactionID(), response.getPeer().getName()));
            } else {
                System.out.println(String.format("FAILED instantiate proposal response Txid: %s from peer %s", response.getTransactionID(), response.getPeer().getName()));
            }
        }

        return channel.sendTransaction(proposalResponses, ImmutableSet.of(orderer));
    }

    public void installChaincode(HFClient client, ChaincodeID chaincodeId, Collection<Peer> peers, File tarGzFile) throws InvalidArgumentException, ProposalException, IOException {
        extractChaincode(tarGzFile);
        InstallProposalRequest installProposalRequest = client.newInstallProposalRequest();
        installProposalRequest.setChaincodeID(chaincodeId);

        installProposalRequest.setChaincodeSourceLocation(new File(chaincodeProperties.getSourceDir()));
        installProposalRequest.setChaincodeVersion(chaincodeId.getVersion());

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

    private void extractChaincode(File tarGzFile) {
        File destination = new File(chaincodeProperties.getBaseDir());
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


