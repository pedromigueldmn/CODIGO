import java.rmi.registry.*;
import java.rmi.ConnectException;
import java.util.Timer;
import java.util.TimerTask;
import java.io.*;
import java.security.Signature;
import java.security.PublicKey;
import java.util.Base64;

public class ClienteRMI
{
    private static Timer timer;
    private static StockServer servidor;
    private static int estadoAtual = 0;
    private static PublicKey chavePublicaServidor;


    public static void main(String[] args)
    {

        if (args.length != 1)
        {
            System.err.println("Uso: java ClienteRMI <IP_do_Servidor>");
            System.exit(1);
        }

        String serverIP = args[0];

        try {
                Registry registry = LocateRegistry.getRegistry(serverIP);
                servidor = (StockServer) registry.lookup("StockServer");
                obterChavePublicaServidor();
                servidor.subscribe(new SecureDirectNotificationImpl(chavePublicaServidor));

        }
        catch (ConnectException ce)
        {
            System.err.println("Erro ao conectar ao servidor RMI: O servidor RMI pode não estar em execução.");
            ce.printStackTrace();
            System.exit(1);
        }
        catch (Exception e)
        {
            System.err.println("Erro ao conectar ao servidor RMI: " + e.toString());
            e.printStackTrace();
        }

        iniciarAtualizacaoAutomatica();

        while (true)
        {
            try
            {
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
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
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


    private static void iniciarAtualizacaoAutomatica()
    {
        timer = new Timer();
        timer.schedule(new TimerTask()
        {
            @Override
            public void run()
            {
                solicitarProdutos();
            }
        }, 0, 5000);
    }

    private static void reiniciarAtualizacaoAutomatica()
    {
        timer.cancel();
        solicitarProdutos();
        iniciarAtualizacaoAutomatica();
        estadoAtual = 0;
    }

    public static void solicitarProdutos()
    {
        try
        {
            String produtos = servidor.stock_request();
            mostrarProdutos(produtos);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private static void adicionarAoStock(BufferedReader userInput)
    {
        estadoAtual = 1;
        System.out.print("Insira o PID do produto: ");
        int PID = 0;
        try
        {
            PID = Integer.parseInt(userInput.readLine());
        } catch (NumberFormatException e)
        {
            System.out.println("Erro: O PID deve ser um número inteiro.");
            estadoAtual=0;
            return;
        } catch (IOException e)
        {
            System.out.println("Erro de entrada/saída ao ler o PID do produto.");
            estadoAtual=0;
            return;
        }

        estadoAtual = 2;
        System.out.print("Insira a quantidade a adicionar: ");
        int quantidade = 0;
        try
        {
            quantidade = Integer.parseInt(userInput.readLine());
        }
        catch (NumberFormatException e)
        {
            System.out.println("Erro: A quantidade deve ser um número inteiro.");
            estadoAtual=0;
            return;
        }
        catch (IOException e)
        {
            System.out.println("Erro de entrada/saída ao ler a quantidade para adicionar.");
            estadoAtual=0;
            return;
        }

        try
        {
            String resposta = servidor.stock_update(PID, quantidade);
            handleServerResponse(resposta);
        }
        catch (Exception e)
        {
            System.out.println("Ocorreu um erro ao atualizar o stock: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void removerDoStock(BufferedReader userInput)
    {
        estadoAtual = 1;
        System.out.print("Insira o PID do produto: ");
        int PID = 0;
        try
        {
            PID = Integer.parseInt(userInput.readLine());
        }
        catch (NumberFormatException e)
        {
            System.out.println("Erro: O PID deve ser um número inteiro.");
            estadoAtual = 0;
            return;
        }
        catch (IOException e)
        {
            System.out.println("Erro de entrada/saída ao ler o PID do produto.");
            estadoAtual=0;
            return;
        }

        estadoAtual = 3;
        System.out.print("Insira a quantidade a remover: ");
        int quantidade = 0;
        try
        {
            quantidade = Integer.parseInt(userInput.readLine());
        }
        catch (NumberFormatException e)
        {
            System.out.println("Erro: A quantidade deve ser um número inteiro.");
            estadoAtual = 0;
            return;
        }
        catch (IOException e)
        {
            System.out.println("Erro de entrada/saída ao ler a quantidade para remover.");
            estadoAtual=0;
            return;
        }

        try
        {
            String resposta = servidor.stock_update(PID, -quantidade);
            handleServerResponse(resposta);
        }
        catch (Exception e)
        {
            System.out.println("Ocorreu um erro ao remover do stock: " + e.getMessage());
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
        switch (estadoAtual)
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

    private static void obterChavePublicaServidor()
    {
        try
        {
            chavePublicaServidor = servidor.get_pubKey();
        }
        catch (Exception e)
        {
            System.err.println("Erro ao obter a chave pública do servidor: " + e.getMessage());
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
}
