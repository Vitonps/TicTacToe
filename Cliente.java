import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;

public class Cliente {
    private final Socket socket;
    private final ObjectOutputStream saida;
    private final ObjectInputStream entrada;
    private final Scanner scanner;
    //Construtor cliente
    public Cliente(String serverAddress, int serverPort) throws IOException {
        this.socket = new Socket(serverAddress, serverPort);
        this.saida = new ObjectOutputStream(socket.getOutputStream());
        this.entrada = new ObjectInputStream(socket.getInputStream());
        this.scanner = new Scanner(System.in);
    }
    //Metodo para enviar o nome pro servidor
    public void enviarNome(String nome) throws IOException {
        saida.writeObject(nome);
        saida.flush();
    }
    //Metodo para a escolha do modo no servidor
    public void enviarEscolhaModo(String modo) throws IOException {
        saida.writeObject(modo);
        saida.flush();
    }
    //Metodo para enviar mensagem ao servidor
    public void enviarJogada(String jogada) throws IOException {
        saida.writeObject(jogada);
        saida.flush();
    }
    //Metodo para receber mensagem do servidor
    public Object receberMensagem() throws IOException, ClassNotFoundException {
        return entrada.readObject();
    }
    //Metodo para fechar conexao com o servidor
    public void fecharConexao() {
        try {
            entrada.close();
            saida.close();
            socket.close();
            scanner.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        try {
            //Endereco do servidor
            String serverAddress = "localhost";

            //Porta do servidor
            int serverPort = 12345;

            // Cria o objeto cliente
            Cliente cliente = new Cliente(serverAddress, serverPort);

            //Nome do jogador
            System.out.print("Digite seu nome: ");
            String nome = cliente.scanner.nextLine();
            cliente.enviarNome(nome);

            System.out.println("Conectado no servidor!");

            //Recebe a mensagem do servidor para escolher o modo de jogo
            String resposta = (String) cliente.receberMensagem();
            System.out.println(resposta);
            
            //Campo de escolha para o modo de jogo
            String modo = cliente.scanner.nextLine();
            cliente.enviarEscolhaModo(modo);

            //Mensagem pro jogador para nao parecer que travou o sistema
            if(modo.equals("1")){
                System.out.println("Aguardando segundo jogador..");
            }

            while (true) {
                //Recebe a escolha de pedra, papel ou tesoura
                resposta = (String) cliente.receberMensagem();
                System.out.println(resposta);
                
                if(resposta.equals( "Modo de jogo inválido. Desconectando...")){
                    cliente.fecharConexao();
                    break;
                }

                //Campo de escolha do pedra, papel e tesoura
                String enviar = cliente.scanner.nextLine();
                //Checa se escreveu correto a escolha
                if (!isValidMove(enviar)) {
                    while(!isValidMove(enviar)){
                        System.out.println("Jogada inválida! Use apenas 'pedra', 'papel' ou 'tesoura'.");
                        enviar = cliente.scanner.nextLine();
                    }
                }

                //Envia a escolha pro servidor    
                cliente.enviarJogada(enviar);

                //Recebe o resultado da partida
                resposta = (String) cliente.receberMensagem();
                System.out.println(resposta);
                
                //Fecha o jogo se digitar "exit"
                if (enviar.equals("exit")) {
                    cliente.fecharConexao();
                    break;
                }
            
            }
            //Retorna erro se algo de errado acontecer
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        
    }
    //Metodo para checar se a escolha foi valida
    private static boolean isValidMove(String jogada) {
        return jogada.equalsIgnoreCase("pedra") ||
                jogada.equalsIgnoreCase("papel") ||
                jogada.equalsIgnoreCase("tesoura") ||
                jogada.equalsIgnoreCase("exit");
    }
    
}
