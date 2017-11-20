package care.solve.fabric.service;

public interface TransactionService {

    byte[] sendQueryTransaction(String func, String[] args);
    byte[] sendInvokeTransaction(String func, String[] args);

}
