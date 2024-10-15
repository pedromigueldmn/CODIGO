import java.io.Serializable;

public class Produto implements Serializable
{
    private int PID;
    private String designacao;
    private int quantidade;


    public Produto(String designacao, int quantidade, int PID)
    {
        this.designacao = designacao;
        this.quantidade = quantidade;
        this.PID = PID;
    }

    public String getDesignacao()
    {
        return designacao;
    }

    @Override
    public String toString()
    {
        return PID + " --- " + designacao + " --- " + quantidade;
    }

    public int getQuantidade()
    {
        return quantidade;
    }

    public void setQuantidade(int quantidade)
    {
        this.quantidade = quantidade;
    }

    public int getPID()
    {
        return PID;
    }
}
