import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Base64;

public class SecureDirectNotificationImpl extends UnicastRemoteObject implements SecureDirectNotification
{
    private PublicKey chavePublica;

    public SecureDirectNotificationImpl(PublicKey chavePublica) throws RemoteException
    {
        super();
        this.chavePublica = chavePublica;
        System.out.println("\n\nFUI CRIADO\n\n");
    }

    public void stock_updated(String message) throws RemoteException
    {
        System.out.println("|                                   |");
        System.out.println("+--Ocorreu uma atualização segura!--+");
    }

    public void stock_updated_signed(String message, String signature) throws RemoteException
    {

        if (verificarAssinatura(message, signature))
        {
            stock_updated(message);
        }
        else
        {
            System.out.println("Falha na verificação da assinatura. A mensagem pode ter sido alterada!");
        }
    }

    private boolean verificarAssinatura(String mensagem, String assinatura)
    {
        try
        {
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initVerify(chavePublica);
            sig.update(mensagem.getBytes());
            return sig.verify(Base64.getDecoder().decode(assinatura));
        }
        catch (Exception e)
        {
            System.err.println("Erro ao verificar a assinatura: " + e.getMessage());
            return false;
        }
    }

}
