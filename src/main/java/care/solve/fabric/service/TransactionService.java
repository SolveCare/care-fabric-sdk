package care.solve.fabric.service;

public interface TransactionService {

    byte[] sendQueryTransaction(String channelName, String func, String[] args);
    byte[] sendInvokeTransaction(String channelName, String func, String[] args);

}
