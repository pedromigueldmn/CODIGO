import java.rmi.Remote;
import java.rmi.RemoteException;
import java.security.PublicKey;

public interface StockServer extends Remote
{
    String stock_request() throws RemoteException;
    String stock_update(int PID, int qtd) throws RemoteException;
    void subscribe(SecureDirectNotification client) throws RemoteException;
    PublicKey get_pubKey() throws RemoteException;
}
