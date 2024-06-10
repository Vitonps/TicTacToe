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

    public Cliente(String serverAddress, int serverPort) throws IOException {
        this.socket = new Socket(serverAddress, serverPort);
        this.saida = new ObjectOutputStream(socket.getOutputStream());
        this.entrada = new ObjectInputStream(socket.getInputStream());
        this.scanner = new Scanner(System.in);
    }

    public void enviarNome(String nome) throws IOException {
        saida.writeObject(nome);
        saida.flush();
    }

    public void enviarEscolhaModo(String modo) throws IOException {
        saida.writeObject(modo);
        saida.flush();
    }

    public void enviarJogada(String jogada) throws IOException {
        saida.writeObject(jogada);
        saida.flush();
    }

    public Object receberMensagem() throws IOException, ClassNotFoundException {
        return entrada.readObject();
    }

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
        Scanner scanner = new Scanner(System.in);
        
        try {
            System.out.print("Digite o endereço do servidor: ");
            String serverAddress = scanner.nextLine();
            
            System.out.print("Digite a porta do servidor: ");
            int serverPort = scanner.nextInt();
            scanner.nextLine(); // Limpa o buffer do scanner

            Cliente cliente = new Cliente(serverAddress, serverPort);

            System.out.print("Digite seu nome: ");
            String nome = cliente.scanner.nextLine();
            cliente.enviarNome(nome);

            System.out.println("Conectado no servidor!");

            String resposta = (String) cliente.receberMensagem();
            System.out.println(resposta);

            String modo = cliente.scanner.nextLine();
            cliente.enviarEscolhaModo(modo);

            if(modo.equals("1")){
                System.out.println("Aguardando segundo jogador..");
            }

            while (true) {
                resposta = (String) cliente.receberMensagem();
                System.out.println(resposta);

                if(resposta.equals("Modo de jogo inválido. Desconectando...")){
                    cliente.fecharConexao();
                    break;
                }

                String enviar = cliente.scanner.nextLine();
                if (!isValidMove(enviar)) {
                    while(!isValidMove(enviar)){
                        System.out.println("Jogada inválida! Use apenas 'pedra', 'papel' ou 'tesoura'.");
                        enviar = cliente.scanner.nextLine();
                    }
                }

                cliente.enviarJogada(enviar);

                resposta = (String) cliente.receberMensagem();
                System.out.println(resposta);

                if (enviar.equals("exit")) {
                    cliente.fecharConexao();
                    break;
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            scanner.close();
        }
    }

    private static boolean isValidMove(String jogada) {
        return jogada.equalsIgnoreCase("pedra") ||
                jogada.equalsIgnoreCase("papel") ||
                jogada.equalsIgnoreCase("tesoura") ||
                jogada.equalsIgnoreCase("exit");
    }
}
