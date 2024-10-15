import java.net.*;
import java.io.*;
import java.rmi.*;
import java.rmi.server.*;
import java.rmi.registry.*;
import java.util.List;
import java.util.ArrayList;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;
import java.security.Signature;


public class Servidor
{
    static final int DEFAULT_PORT_SOCKET = 12345;
    static final int DEFAULT_PORT_RMI = 1099;
    private static List<SecureDirectNotification> RMIclients = new ArrayList<>();
    private static List<Socket> Socketclients = new ArrayList<>();
    private static PrivateKey chavePrivada;
    private static PublicKey chavePublica;


    private static void notifyRMIClients(String update)
    {
        String assinatura = assinarMensagem(update);

        for (SecureDirectNotification client : RMIclients)
        {
            try
            {
                client.stock_updated_signed(update, assinatura);
            }
            catch (RemoteException e)
            {
                System.out.println("Falha ao notificar o cliente: " + e.getMessage());
            }
        }
    }

    private static void notifySocketClients(String mensagem) 
    {
        synchronized(Socketclients) 
        {
            for (Socket cliente : Socketclients) 
            {
                try 
                {
                    PrintWriter out = new PrintWriter(cliente.getOutputStream(), true);
                    out.println(mensagem);
                } 
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }
    }

    private static void gerarParDeChaves() throws NoSuchAlgorithmException
    {
        boolean chaveGerada = false;
        while (!chaveGerada)
        {
            try {
                KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
                keyGen.initialize(2048);
                KeyPair pair = keyGen.generateKeyPair();
                chavePrivada = pair.getPrivate();
                chavePublica = pair.getPublic();
                chaveGerada = true;
            }
            catch (NoSuchAlgorithmException e)
            {
                System.err.println("Falha ao gerar par de chaves, a tentar novamente...");
                try
                {
                    Thread.sleep(1000); 
                }
                catch (InterruptedException ie)
                {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }


    private static String assinarMensagem(String mensagem)
    {
        try
        {
            Signature assinatura = Signature.getInstance("SHA256withRSA");
            assinatura.initSign(chavePrivada);
            assinatura.update(mensagem.getBytes());
            byte[] assinaturaBytes = assinatura.sign();
            return Base64.getEncoder().encodeToString(assinaturaBytes);
        }
        catch (Exception e)
        {
            System.err.println("Erro ao assinar a mensagem: " + e.getMessage());
            return null;
        }
    }


    public static class StockServerImpl extends UnicastRemoteObject implements StockServer
    {
        private Inventario inventario;

        protected StockServerImpl(Inventario inv) throws RemoteException
        {
            this.inventario = inv;
        }

        @Override
        public synchronized String stock_request() throws RemoteException
        {

            try
            {
                System.out.println("Nova conexão establecida de: " + getClientHost());
            }
            catch (ServerNotActiveException e)
            {
                throw new RuntimeException(e);
            }

            String resposta = inventario.listarProdutos();
            String assinatura = Servidor.assinarMensagem(resposta);

            return resposta + "." + assinatura;
        }

        @Override
        public synchronized String stock_update(int PID, int quantidade) throws RemoteException
        {
            Produto produto = inventario.getProdutoPorPID(PID);

            try
            {
                System.out.println("Nova conexão establecida de: " + getClientHost());
            }
            catch (ServerNotActiveException e)
            {
                throw new RuntimeException(e);
            }

            if (produto == null)
            {
                return "STOCK_ERROR: Produto não encontrado.";
            }

            if (quantidade == 0 || produto.getQuantidade() + quantidade < 0)
            {
                return "STOCK_ERROR: Operação inválida.";
            }

            else
            {
                inventario.atualizarQuantidadeProduto(produto.getDesignacao(), quantidade);
                String update = "STOCK_UPDATED:" + inventario.listarProdutos();
                String assinatura = Servidor.assinarMensagem(update);
                notifyRMIClients(update);
                notifySocketClients(update);
                return update + "." + assinatura;
            }
        }

        public synchronized void subscribe(SecureDirectNotification notf) throws RemoteException
        {

            try
            {
                System.out.println("Nova conexão establecida de: " + getClientHost());
            }
            catch (ServerNotActiveException e)
            {
                throw new RuntimeException(e);
            }


            RMIclients.add(notf);
        }

        public PublicKey get_pubKey() throws RemoteException
        {
            return Servidor.chavePublica;
        }
    }


    static class ClienteHandler extends Thread
    {
        private Socket ligacao;
        private Inventario inventario;

        public ClienteHandler(Socket ligacao, Inventario inventario)
        {
            this.ligacao = ligacao;
            this.inventario = inventario;
        }

        @Override
        public void run()
        {
            PrintWriter out = null;

            try {

                BufferedReader in = new BufferedReader(new InputStreamReader(ligacao.getInputStream()));
                out = new PrintWriter(ligacao.getOutputStream(), true);
                boolean naofechar = false;

                String mensagem = in.readLine();

                if (mensagem == null)
                {
                    return;
                }

                String[] partes = mensagem.split(":");

                switch (partes[0])
                {
                    case "STOCK_REQUEST":
                        String respostaStockRequest = "STOCK_RESPONSE:" + inventario.listarProdutos();
                        String assinaturaStockRequest = Servidor.assinarMensagem(respostaStockRequest);
                        out.println(respostaStockRequest + "." + assinaturaStockRequest);
                        naofechar = false;
                        break;

                    case "STOCK_UPDATE":
                        try {
                            int PID = Integer.parseInt(partes[1]);
                            int quantidade = Integer.parseInt(partes[2]);
                            String sinal = partes[3];
                            

                            Produto produto = inventario.getProdutoPorPID(PID);

                            if (produto == null)
                            {
                                out.println("STOCK_ERROR:Produto não encontrado.");
                            }
                            else
                            {
                                if ("-".equals(sinal))
                                {
                                    quantidade = -quantidade;
                                }

                                if (quantidade == 0 || produto.getQuantidade() + quantidade < 0)
                                {
                                    out.println("STOCK_ERROR:Quantidade inválida para o PID fornecido.");
                                }
                                else
                                {
                                    inventario.atualizarQuantidadeProduto(produto.getDesignacao(), quantidade);
                                    String update = "STOCK_UPDATED:" + inventario.listarProdutos();
                                    notifyRMIClients(update);
                                    notifySocketClients(update);

                                    String assinaturaUpdate = Servidor.assinarMensagem(update);
                                    out.println(update + "." + assinaturaUpdate);
                                }
                            }
                        }
                        catch (NumberFormatException e)
                        {
                            out.println("STOCK_ERROR:PID ou quantidade fornecida não é um número válido.");
                        }
                        catch (IllegalArgumentException e)
                        {
                            out.println("STOCK_ERROR:" + e.getMessage());
                        }
                        naofechar = false;
                        break;

                    case "GET_PUBKEY":
                        out.println(Base64.getEncoder().encodeToString(chavePublica.getEncoded()));
                        naofechar = false;
                        break;


                    case "PING":
                        
                        synchronized(Socketclients) 
                        {
                            Socketclients.add(ligacao);
                        }
                        naofechar = true;
                        break;


                    default:
                        out.println("STOCK_ERROR:Mensagem inválida.");
                        naofechar = false;
                        break;
                }
                if (naofechar==false) ligacao.close();
            }
            catch (IOException e)
            {
                System.out.println("Erro no atendedor de cliente: " + e);
            }
        }
    }


    public static void main(String[] args) throws RemoteException, MalformedURLException
    {
        Inventario inventario = new Inventario();

        try
        {
            gerarParDeChaves();
        }
        catch (NoSuchAlgorithmException e)
        {
            System.err.println("Falha ao gerar par de chaves: " + e.getMessage());
            return;
        }

        new Thread(() -> {
            try
            {
                LocateRegistry.createRegistry(DEFAULT_PORT_RMI);

                StockServerImpl stockServer = new StockServerImpl(inventario);

                Naming.rebind("StockServer", stockServer);

            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }).start();

        new Thread(() -> {
            ServerSocket servidor = null;
            try {
                servidor = new ServerSocket(DEFAULT_PORT_SOCKET);


                System.out.println("\nServidor à espera de ligações.  \n");

                while (true)
                {
                    Socket ligacao = servidor.accept();

                    String clientIP = ligacao.getInetAddress().getHostAddress();

                    System.out.println("Nova conexão establecida de: " + clientIP);

                    ClienteHandler tratar = new ClienteHandler(ligacao, inventario);

                    tratar.start();
                }
            } catch (IOException e) 
            {
                System.out.println("Erro no servidor: " + e);
            }
        }).start();
    }
}
