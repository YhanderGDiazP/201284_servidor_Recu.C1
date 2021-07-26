package sample.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import sample.model.ClientSocket;
import sample.model.Nodo;
import sample.model.Server;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;

public class Controller implements Observer {
    ServerSocket serverSocket = null;
    private final int PORT = 3001;
    private ArrayList<Nodo> poolSocket = new ArrayList<>();

    @FXML
    private Button btnOpenServer;

    @FXML
    private Button btnSalir;

    @FXML
    private ListView<String> listClient;

    @FXML
    private Circle circleLed;

    @FXML
    void OpenServerOnMouseClicked(MouseEvent event) {
        byte[] ipBytes = {(byte)127,(byte)0,(byte)0, (byte)1 };
        InetAddress ip = null;

        try {
            ip = InetAddress.getByAddress(ipBytes);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        try {
            serverSocket = new ServerSocket(PORT,100,ip);
            listClient.getItems().add("Server abierto: " + serverSocket.getInetAddress().getHostName());
            circleLed.setFill(Color.GREEN);

            Server server = new Server(serverSocket);
            server.addObserver(this);
            new Thread(server).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
/*        finally {
            try {
                serverSocket.close();
                listClient.getItems().add("Server cerrado");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }*/

    }

    @FXML
    void SalirOnMouseClicked(MouseEvent event) {
        System.exit(1);
    }

    @Override
    public void update(Observable o, Object arg) {

        if (o instanceof Server) {
            Socket socket = (Socket)arg;
            poolSocket.add(new Nodo(socket.hashCode(),"usuario"+poolSocket.size(),socket));
            // Broadcast a todos los sockets conectados para actualizar la lista de conexiones
            broadCast();
            // Crear un hilo que reciba mensajes entrantes de ese nuevo socket creado
            ClientSocket clientSocket = new ClientSocket(socket);
            clientSocket.addObserver(this);
            new Thread(clientSocket).start();
            Platform.runLater(() -> listClient.getItems().add(socket.getInetAddress().getHostName()));
        }
        if (o instanceof ClientSocket){
            String mensaje = (String)arg;
            String[] datagrama;
            datagrama = mensaje.split(":");
            if (datagrama[0].equals("3")) {
                System.out.println("Recibio 3");
                System.out.println("Un nuevo mensaje");
                sendMessage(datagrama[1],datagrama[2],datagrama[3]);
            }
            System.out.println("Recibio 1");
            System.out.println("Nueva conexion");
            Platform.runLater(() -> listClient.getItems().add(mensaje));
        }

        //Platform.runLater(() -> listClient.getItems().add(socket.getInetAddress().getHostName()));

    }

    private void broadCast(){
        DataOutputStream bufferDeSalida = null;
        Nodo ultimaConexion = poolSocket.get(poolSocket.size()-1);
        for (Nodo nodo: poolSocket) {
            try {

                bufferDeSalida = new DataOutputStream(nodo.getSocket().getOutputStream());
                bufferDeSalida.flush();
                listaClientes();
                bufferDeSalida.writeUTF("1:Servidor:"+nodo.getName()+":"+ultimaConexion.getName()+listaClientes());

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String listaClientes(){
        String lista = "";
        for (Nodo nodo: poolSocket) {
        //System.out.println(nodo.getName());
        lista = lista+":"+nodo.getName();
        }
        //System.out.println("___________" +lista+ "____________");
        return lista;
    }



    private void sendMessage(String source, String destino, String mensaje) {
        System.out.println("Send Menssage");
        DataOutputStream bufferDeSalida = null;
        for (Nodo nodo: poolSocket) {
            if(destino.equals(nodo.getName()))
                try {
                    bufferDeSalida = new DataOutputStream(nodo.getSocket().getOutputStream());
                    bufferDeSalida.flush();
                    bufferDeSalida.writeUTF(source+": "+ mensaje);
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
    }
}



