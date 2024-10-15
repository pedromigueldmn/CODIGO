import java.util.ArrayList;
import java.io.*;


public class Inventario
{
    private ArrayList <Produto> produtos;
    private static int contadorPID = 0;
    static final String NOME_FICHEIRO = "inventario.dat";

    public Inventario()
    {
        produtos = new ArrayList<>();

        carregarTudo();

        if (produtos.isEmpty())
        {
            adicionarProduto("Computador", 10);
            adicionarProduto("Telemovel", 15);
            adicionarProduto("Tablet", 8);
            adicionarProduto("Monitor", 6);
            adicionarProduto("Teclado", 20);
            adicionarProduto("Rato", 25);
            adicionarProduto("Headphones", 30);
            adicionarProduto("Oculos VR", 50);
        }

    }

    public synchronized ArrayList<Produto> getInventario()
    {
        return produtos;
    }

    public synchronized void adicionarProduto (String nome, int quantidade)
    {
        Produto produto = new Produto(nome, quantidade, contadorPID++);
        produtos.add(produto);
        gravarTudo();
    }


    public synchronized Produto getProdutoPorNome(String nome)
    {
        for(Produto produto : produtos)
        {
            if(produto.getDesignacao().equals(nome))
            {
                return produto;
            }
        }
        return null;
    }

    public synchronized Produto getProdutoPorPID(int PID)
    {
        for (Produto produto : produtos)
        {
            if (produto.getPID() == PID)
            {
                return produto;
            }
        }
        return null;
    }

    public synchronized void atualizarQuantidadeProduto(String nome, int quantidade)
    {
        Produto produto = getProdutoPorNome(nome);

        if (produto == null)
        {
            produto = new Produto(nome, quantidade, contadorPID++);
            produtos.add(produto);
        }
        else
        {
            produto.setQuantidade(produto.getQuantidade() + quantidade);
        }
        gravarTudo();
    }

    public synchronized String listarProdutos()
    {
        StringBuilder resultado = new StringBuilder();

        for (Produto produto : produtos)
        {
            resultado.append(produto.toString()).append("|");
        }
        return resultado.toString();
    }



    @SuppressWarnings("unchecked")          
    public void gravarTudo()
    {
        try (FileOutputStream fos = new FileOutputStream(NOME_FICHEIRO);
             ObjectOutputStream oos = new ObjectOutputStream(fos))
        {

            oos.writeObject(produtos);

        } catch (IOException e)
        {
            System.out.println("Erro ao salvar inventário: " + e);
        }
    }

    @SuppressWarnings("unchecked")        
    public void carregarTudo()
    {
        File ficheiro = new File(NOME_FICHEIRO);

        if (ficheiro.exists() && !ficheiro.isDirectory())
        {
            try (FileInputStream fis = new FileInputStream(NOME_FICHEIRO);
                 ObjectInputStream ois = new ObjectInputStream(fis)) 
                {
                    produtos = (ArrayList<Produto>) ois.readObject();
                } 
                catch (IOException | ClassNotFoundException e) 
                {
                    System.out.println("Erro ao carregar inventário: " + e);
                }
        }
    }
}
