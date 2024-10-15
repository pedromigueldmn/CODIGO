import java.net.*;
import java.io.*;
import java.util.Timer;
import java.util.TimerTask;
import java.security.Signature;
import java.security.PublicKey;
import java.util.Base64;
import java.security.KeyFactory;
import java.security.spec.X509EncodedKeySpec;


public class ClienteSocket
{
    static final int DEFAULT_PORT = 12345;
    private static final int NOTIFICATION_PORT = 12346;
    static final String DEFAULT_HOST = "127.0.0.1";
    private static Timer timer;
    private static String serverIP;
    private static int serverPort;
    private static int estadoAtual = 0;
    private static PublicKey chavePublicaServidor;



    public static void main(String[] args)
    {
        if (args.length != 1)
        {
            System.err.println("Uso: java Cliente <IP_do_Servidor>");
            System.exit(1);
        }

        serverIP = args[0];
        serverPort = DEFAULT_PORT;

        obterChavePublicaServidor();
        iniciarAtualizacaoAutomatica();
        iniciarOuvinteDeNotificacoes();

        while (true)
        {
            try {
                BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));
                String choice = userInput.readLine();

                switch (choice)
                {
                    case "1":
                        adicionarAoStock(userInput);
                        break;
                    case "2":
                        removerDoStock(userInput);
                        break;
                    case "3":
                        reiniciarAtualizacaoAutomatica();
                        break;
                    case "4":
                        timer.cancel();
                        System.exit(0);
                        break;
                    default:
                        System.out.println("Opção inválida.");
                        break;
                }
            } catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    private static void iniciarAtualizacaoAutomatica()
    {
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                solicitarProdutos();
            }
        }, 0, 5000);
    }

    private static void reiniciarAtualizacaoAutomatica()
    {
        timer.cancel(); 
        solicitarProdutos();
        iniciarAtualizacaoAutomatica();
        estadoAtual=0;
    }

    private static void obterChavePublicaServidor()
    {
        try (Socket ligacao = new Socket(serverIP, serverPort);
             BufferedReader in = new BufferedReader(new InputStreamReader(ligacao.getInputStream()));
             PrintWriter out = new PrintWriter(ligacao.getOutputStream(), true)) {

            out.println("GET_PUBKEY");
            String chavePublicaEncoded = in.readLine();
            byte[] chavePublicaBytes = Base64.getDecoder().decode(chavePublicaEncoded);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(chavePublicaBytes);
            chavePublicaServidor = keyFactory.generatePublic(publicKeySpec);
        } 
        catch (Exception e)
        {
            System.err.println("Erro ao obter a chave pública do servidor: " + e.getMessage());
        }
    }


    private static boolean verificarAssinatura(String mensagem, String assinatura)
    {
        try
        {
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initVerify(chavePublicaServidor);
            sig.update(mensagem.getBytes());
            return sig.verify(Base64.getDecoder().decode(assinatura));
        }
        catch (Exception e)
        {
            System.err.println("Erro ao verificar a assinatura: " + e.getMessage());
            return false;
        }
    }


    private static void solicitarProdutos()
    {
        try (Socket ligacao = new Socket(serverIP, serverPort);
             BufferedReader in = new BufferedReader(new InputStreamReader(ligacao.getInputStream()));
             PrintWriter out = new PrintWriter(ligacao.getOutputStream(), true))
        {

            out.println("STOCK_REQUEST");
            String produtos = in.readLine();
            mostrarProdutos(produtos.split(":", 2)[1]);


        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private static void adicionarAoStock(BufferedReader userInput)
    {
        try (Socket ligacao = new Socket(serverIP, serverPort);
             BufferedReader in = new BufferedReader(new InputStreamReader(ligacao.getInputStream()));
             PrintWriter out = new PrintWriter(ligacao.getOutputStream(), true))
        {
            estadoAtual = 1;

            System.out.print("Insira o PID do produto: ");
            String pidInput = userInput.readLine();
            int PID;
            try {
                PID = Integer.parseInt(pidInput);
            } catch (NumberFormatException e)
            {
                System.out.println("Erro: O PID deve ser um número inteiro.");
                estadoAtual=0;
                return;
            }

            estadoAtual = 2;

            System.out.print("Insira a quantidade a adicionar: ");
            int quantidade;
            try {
                quantidade = Integer.parseInt(userInput.readLine());
            }
            catch (NumberFormatException e)
            {
                System.out.println("Erro: A quantidade deve ser um número inteiro.");
                estadoAtual=0;
                return;
            }

            out.println("STOCK_UPDATE:" + PID + ":" + quantidade + ":+");

            String resposta = in.readLine();

            handleServerResponse(resposta);

        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private static void removerDoStock(BufferedReader userInput)
    {
        try (Socket ligacao = new Socket(serverIP, serverPort);
             BufferedReader in = new BufferedReader(new InputStreamReader(ligacao.getInputStream()));
             PrintWriter out = new PrintWriter(ligacao.getOutputStream(), true))
        {
            estadoAtual = 1;

            System.out.print("Insira o PID do produto: ");
            String pidInput = userInput.readLine();
            int PID;
            try
            {
                PID = Integer.parseInt(pidInput);
            } catch (NumberFormatException e)
            {
                System.out.println("Erro: O PID deve ser um número inteiro.");
                estadoAtual = 0;
                return;
            }

            estadoAtual = 3;
            System.out.print("Insira a quantidade a remover: ");
            int quantidade;
            try
            {
                quantidade = Integer.parseInt(userInput.readLine());
            } catch (NumberFormatException e)
            {
                System.out.println("Erro: A quantidade deve ser um número inteiro.");
                estadoAtual = 0;
                return;
            }

            out.println("STOCK_UPDATE:" + PID + ":" + quantidade + ":-");

            String resposta = in.readLine();

            handleServerResponse(resposta);

        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }


    private static void mostrarProdutos(String produtosComAssinatura)
    {
        String[] partes = produtosComAssinatura.split("\\.", 2);
        String produtos = partes[0];

        System.out.println("\n\n#####################################\n\n");
        System.out.println("+-----------------------------------+");
        System.out.println("|            Inventário             |");
        System.out.println("+-----------------------------------+");
        System.out.println("|  PID --- Produto --- Quantidade   |\n");
        System.out.println("+-----------------------------------+");

        for (String produto : produtos.split("\\|"))
        {
            System.out.println("  " + produto);
            System.out.println("+-----------------------------------+");
        }

        System.out.println("\n+-----------------------------------+");
        mostrarMenuAtual();
    }

    private static void mostrarMenuAtual()
    {
        switch(estadoAtual)
        {
            case 0:
                System.out.println("|        Escolha uma opção:         |");
                System.out.println("+-----------------------------------+");
                System.out.println("| 1. Adicionar ao stock             |");
                System.out.println("| 2. Remover do stock               |");
                System.out.println("| 3. Listar Produtos                |");
                System.out.println("| 4. Sair                           |");
                break;
            case 1:
                System.out.print("Insira o PID do produto: ");
                break;
            case 2:
                System.out.print("Insira a quantidade a adicionar: ");
                break;
            case 3:
                System.out.print("Insira a quantidade a remover: ");
                break;
        }
    }


    private static void handleServerResponse(String resposta) throws IOException
    {
        if (resposta.contains("."))
        {
            String[] partes = resposta.split("\\.", 2);
            String mensagem = partes[0];
            String assinatura = partes[1];

            if (verificarAssinatura(mensagem, assinatura))
            {
                if (mensagem.startsWith("STOCK_RESPONSE"))
                {
                    mostrarProdutos(mensagem.split(":", 2)[1]);
                }
                else if (mensagem.startsWith("STOCK_ERROR"))
                {
                    System.out.println(mensagem.split(":", 2)[1]);
                    estadoAtual = 0;
                }
                else if (mensagem.startsWith("STOCK_UPDATED"))
                {
                    System.out.println("Stock atualizado:");
                    estadoAtual = 0;
                    mostrarProdutos(mensagem.split(":", 2)[1]);
                }
                else
                {
                    System.out.println(mensagem);
                }
            }
            else
            {
                System.out.println("Assinatura inválida!");
            }
        }
        else
        {
            System.out.println("Resposta não assinada ou formato inválido.");
        }
    }

    private static void iniciarOuvinteDeNotificacoes() 
    {
        new Thread(() -> {
            try (Socket socketNotificacao = new Socket(serverIP, DEFAULT_PORT);
                 PrintWriter out = new PrintWriter(socketNotificacao.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socketNotificacao.getInputStream()))) 
                 {

                // Envia um PING para se registrar no servidor
                out.println("PING:");

                // Loop para ouvir notificações
                while (true) 
                {
                    String notificacao = in.readLine();

                    if (notificacao != null) 
                    {
                        //System.out.println("Notificação recebida: " + notificacao);
                        System.out.println("+------Ocorreu uma atualização!-----+");
                    }
                }

            } catch (Exception e) 
            {
                e.printStackTrace();
            }
        }).start();
    }
}
