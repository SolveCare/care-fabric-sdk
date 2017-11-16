package care.solve.fabric.service;

import com.google.protobuf.ByteString;
import org.hyperledger.fabric.sdk.BlockEvent;

public interface TransactionService {

    ByteString sendQueryTransaction(String func, String[] args);
    BlockEvent.TransactionEvent sendInvokeTransaction(String func, String[] args);

}
