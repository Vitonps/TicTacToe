
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Queue;

public class Servidor {
    private final ServerSocket serverSocket;
    private final Queue<Jogador> filaDeEspera;

    public static void main(String[] args) {
        try {
            Servidor servidor = new Servidor(12345);
            servidor.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    //Construtor do servidor
    public Servidor(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        filaDeEspera = new LinkedList<>();
    }
    //Metodo para iniciar o socket do jogador e o StreamHelper
    public void start() {
        while (true) {
            try {
                Socket jogadorSocket = serverSocket.accept();
                System.out.println("Novo jogador conectado: " + jogadorSocket);

                StreamHelper streamHelper = new StreamHelper(jogadorSocket);
                ObjectInputStream entradaJogador = streamHelper.getInputStream();
                ObjectOutputStream saidaJogador = streamHelper.getOutputStream();

                handlePlayerCommunication(jogadorSocket, entradaJogador, saidaJogador);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    //Metodo para cuidar da comunicacao entre cliente e servidor
    private void handlePlayerCommunication(Socket jogadorSocket, ObjectInputStream entrada, ObjectOutputStream saida) {
        try {
            String nomeJogador = (String) entrada.readObject();
            System.out.println("Nome do jogador recebido: " + nomeJogador);
            saida.writeObject("Escolha a modalidade de jogo:\n1. Multiplayer(Digite: 1)\n2. Contra IA(Digite: 2)");
            saida.flush();
            System.out.println("Mensagem de escolha de modalidade enviada para: " + nomeJogador);
            String modo = (String) entrada.readObject();
            System.out.println("Modo de jogo escolhido por " + nomeJogador + ": " + modo);
            //Escolha do modo de jogo
            if (modo.equals("1")) {
                filaDeEspera.add(new Jogador(jogadorSocket, nomeJogador, entrada, saida));
                if (filaDeEspera.size() >= 2) {
                    Jogador jogador1 = filaDeEspera.poll();
                    Jogador jogador2 = filaDeEspera.poll();
                    iniciarJogoMultiplayer(jogador1, jogador2);
                }
            } else if (modo.equals("2")) {
                iniciarJogoContraIA(new JogadorIA(jogadorSocket, nomeJogador, entrada, saida));
            } else {
                //Se o modo de escolha for errado, desconecta o jogador do servidor
                saida.writeObject("Modo de jogo inválido. Desconectando...");
                saida.flush();
                fecharConexao(jogadorSocket, entrada, saida);
                return;
            }
        //Retorna erro se algo der errado    
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            fecharConexao(jogadorSocket, entrada, saida);
        }
    }
    //Metodo para fechar comunicacao com socket do jogador
    private void fecharConexao(Socket jogadorSocket, ObjectInputStream entrada, ObjectOutputStream saida) {
        try {
            if (entrada != null) {
                entrada.close();
            }
            if (saida != null) {
                saida.close();
            }
            if (jogadorSocket != null) {
                jogadorSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    //Metodo de criacao da thread do jogo multiplayer
    private void iniciarJogoMultiplayer(Jogador jogador1, Jogador jogador2) {
        JogoMultiplayer jogoMultiplayer = new JogoMultiplayer(jogador1, jogador2);
        new Thread(jogoMultiplayer).start();
    }
    //Classe do jogo multiplayer
    private static class JogoMultiplayer implements Runnable {
        private final Jogador jogador1;
        private final Jogador jogador2;
        //Construtor da classe multiplayer
        public JogoMultiplayer(Jogador jogador1, Jogador jogador2) {
            this.jogador1 = jogador1;
            this.jogador2 = jogador2;
        }
        //Metodo do jogo multiplayer
        @Override
        public void run() {
            try {
                ObjectOutputStream saida1 = jogador1.saida;
                ObjectOutputStream saida2 = jogador2.saida;
                ObjectInputStream entrada1 = jogador1.entrada;
                ObjectInputStream entrada2 = jogador2.entrada;
                while (true) {
                    //Escolha de jogada
                    String resultado;
                    String msgInicio = "Escolha sua jogada (pedra, papel, tesoura) ou exit para sair: ";
                    saida1.writeObject(msgInicio);
                    saida2.writeObject(msgInicio);

                    String jogada1 = (String) entrada1.readObject();
                    String jogada2 = (String) entrada2.readObject();
                    //Se um dos jogadores escolher "exit" o outro recebe uma mensagem indicando que o jogador saiu
                    if (jogada1.equalsIgnoreCase("exit") || jogada2.equalsIgnoreCase("exit")) {
                        String msg = "O outro jogador saiu do jogo ):, fechando o sistema";
                        if (jogada1.equalsIgnoreCase("exit") && !jogada2.equalsIgnoreCase("exit")) {
                            saida2.writeObject(msg);
                            saida2.flush();
                        } else if (jogada2.equalsIgnoreCase("exit") && !jogada1.equalsIgnoreCase("exit")) {
                            saida1.writeObject(msg);
                            saida1.flush();
                        }
                        break;
                    }
                    //Logica para mostrar o resultado da jogada
                    resultado = determinarResultado(jogada1.toLowerCase(), jogada2.toLowerCase());
                    saida1.writeObject(resultado);
                    saida1.flush();
                    saida2.writeObject(resultado);
                    saida2.flush();
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            } finally {
                jogador1.fecharConexao();
                jogador2.fecharConexao();
            }
        }
        //Metodo para determinar resultado da partida multiplayer
        private String determinarResultado(String jogada1, String jogada2) {
            String resumo;

            if (jogada1.equals(jogada2)) {
                resumo = "Empate!";
                jogador1.setEmpate();
                jogador2.setEmpate();
            } else if ((jogada1.equals("pedra") && jogada2.equals("tesoura")) ||
                    (jogada1.equals("papel") && jogada2.equals("pedra")) ||
                    (jogada1.equals("tesoura") && jogada2.equals("papel"))) {
                        jogador1.setVitoria();
                        jogador2.setDerrota();
                    resumo = jogador1.getNome() + " venceu!";
            } else {
                jogador2.setVitoria();
                jogador1.setDerrota();
                resumo = jogador2.getNome() + " venceu!";
            }

            return jogador1.getNome() + " escolheu " + jogada1 + "\n" + jogador2.getNome() + " escolheu " + jogada2 + "\n" + resumo +
                   "\nPlacar:\n" + jogador1.getNome() + ": " + jogador1.getResultado() + ",\n" + jogador2.getNome() + ": " + jogador2.getResultado();
        }

    }
    //Metodo para iniciar thread do jogo vs IA
    private void iniciarJogoContraIA(JogadorIA jogadorIA) {
        new Thread(jogadorIA).start();
    }
    //Classe do jogador
    private static class Jogador {
        private final Socket socket;
        private final String nome;
        private final ObjectOutputStream saida;
        private final ObjectInputStream entrada;
        private int vitorias;
        private int derrotas;
        private int empates;
        //Construtor do jogador
        public Jogador(Socket socket, String nome, ObjectInputStream entrada, ObjectOutputStream saida) {
            this.socket = socket;
            this.nome = nome;
            this.entrada = entrada;
            this.saida = saida;
            this.vitorias = 0;
            this.derrotas = 0;
            this.empates = 0;
        }
        //Retorna o nome do jogador
        public String getNome() {
            return nome;
        }
        //Retorna a entrada do jogador
        public ObjectInputStream getEntrada() {
            return entrada;
        }
        //Retorna a saida do jogador
        public ObjectOutputStream getSaida() {
            return saida;
        }
        //Contador para vitoria
        public void setVitoria() {
            vitorias++;
        }
        //Contador para derrota
        public void setDerrota() {
            derrotas++;
        }
        //Contador para empate
        public void setEmpate() {
            empates++;
        }
        //Metodo para retornar o placar do jogador
        public String getResultado() {
            return "Vitórias: " + vitorias + " Derrotas: " + derrotas + " Empates: " + empates;
        }
        //Metodo para fechar conexao do jogador
        public void fecharConexao() {
            try {
                entrada.close();
                saida.close();
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    //Classe da IA do modo jogo IA
    private static class JogadorIA extends Jogador implements Runnable {
        private int vitoriasIA;
        private int derrotasIA;
        private int empatesIA;
        //Contador vitoria IA
        public void setVitoriasIA() {
            this.vitoriasIA++;
        }
        //Contador derrota IA
        public void setDerrotasIA() {
            this.derrotasIA++;
        }
        //Contador empate IA
        public void setEmpatesIA() {
            this.empatesIA++;
        }
        //Metodo para retornar o placar da IA
        public String getResultadoIA() {
            return "IA - Vitórias: " + vitoriasIA + " Derrotas: " + derrotasIA + " Empates: " + empatesIA;
        }
        //Contrutor da IA
        public JogadorIA(Socket socket, String nome, ObjectInputStream entrada, ObjectOutputStream saida) throws IOException {
            super(socket, nome, entrada, saida);

            this.vitoriasIA = 0;
            this.derrotasIA = 0;
            this.empatesIA = 0;
        }
        //Metodo do jogo vs IA
        @Override
        public void run() {
            try {
                ObjectOutputStream saida = getSaida();
                ObjectInputStream entrada = getEntrada();
                while (true) {
                    // Enviar mensagem para escolha da jogada
                    saida.writeObject("Escolha sua jogada (pedra, papel, tesoura) ou exit para sair: ");
                    saida.flush();
        
                    String jogadaJogador = (String) entrada.readObject();
                    if (jogadaJogador.equalsIgnoreCase("exit")) {
                        saida.writeObject("Você saiu do jogo.");
                        saida.flush();
                        break;
                    }
        
                    String jogadaIA = determinarJogadaIA();
                    String resultado = determinarResultado(jogadaJogador, jogadaIA);
        
                    // Enviar resultado para o jogador
                    saida.writeObject(resultado);
                    saida.flush();
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            } finally {
                fecharConexao();
            }
        }
        //Metodo para determinar a escolha da IA
        private String determinarJogadaIA() {
            String[] jogadas = {"pedra", "papel", "tesoura"};
            int indice = (int) (Math.random() * jogadas.length);
            return jogadas[indice];
        }
        //Metodo da logica do resultado do modo IA
        private String determinarResultado(String jogadaJogador, String jogadaIA) {
            String resumo;
    
            if (jogadaJogador.equals(jogadaIA)) {
                resumo = "Empate!";
                setEmpatesIA();
                setEmpate();
            } else if ((jogadaJogador.equals("pedra") && jogadaIA.equals("tesoura")) ||
                    (jogadaJogador.equals("papel") && jogadaIA.equals("pedra")) ||
                    (jogadaJogador.equals("tesoura") && jogadaIA.equals("papel"))) {
                resumo = getNome() + " venceu!";
                setDerrotasIA();
                setVitoria();
            } else {
                resumo = "IA venceu!";
                setVitoriasIA();
                setDerrota();
            }
    
            return String.format(getNome() + " jogou: " + jogadaJogador + ", \n" + "IA jogou: " + jogadaIA +  ". \n"+
             resumo +"\n"+ getResultado() + "\n" + getResultadoIA());
        }
    }
    //Classe StreamHelper
    private static class StreamHelper {
        private final ObjectInputStream entrada;
        private final ObjectOutputStream saida;
        //Construtor StreamHelper
        public StreamHelper(Socket socket) throws IOException {
            this.entrada = new ObjectInputStream(socket.getInputStream());
            this.saida = new ObjectOutputStream(socket.getOutputStream());
        }
        //Metodo de receber input do jogador
        public ObjectInputStream getInputStream() {
            return entrada;
        }
        //Metodo de enviar um output pro jogador
        public ObjectOutputStream getOutputStream() {
            return saida;
        }
    }
}

