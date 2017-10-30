package chat.client;



import chat.Connection;
import chat.ConsoleHelper;
import chat.Message;
import chat.MessageType;

import java.io.IOException;
import java.net.Socket;

public class Client {
    protected Connection connection;
    private volatile boolean clientConnected=false;

    protected String getServerAddress(){
        System.out.print("Введите адрес сервера: ");
        return ConsoleHelper.readString();
    }
    protected int getServerPort(){
        System.out.print("Введите порт: ");
        return ConsoleHelper.readInt();
    }
    protected String getUserName(){
        System.out.print("Введите имя пользователя: ");
        return ConsoleHelper.readString();
    }
    protected boolean shouldSendTextFromConsole(){
        return true;
    }
    protected SocketThread getSocketThread(){
        return new SocketThread();
    }
    protected void sendTextMessage(String text){

        try {
            Message message=new Message(MessageType.TEXT,text);
            connection.send(message);
        } catch (IOException e) {
           clientConnected=false;
        }
    }
    public void run(){
        SocketThread socketThread=getSocketThread();
        socketThread.setDaemon(true);
        socketThread.start();
        synchronized (this){
            try {
                this.wait();
                if(clientConnected)ConsoleHelper.writeMessage("Соединение установлено. Для выхода наберите команду ‘exit’.");
                else ConsoleHelper.writeMessage("Произошла ошибка во время работы клиента.");
                while(clientConnected){
                    String console=ConsoleHelper.readString();
                    if("exit".equals(console))break;
                    if(shouldSendTextFromConsole())sendTextMessage(console);
                }

            } catch (Exception e) {
                ConsoleHelper.writeMessage("Произошла ошибка во время ожидания потока socketThread");

            }
        }
    }

    public class SocketThread extends Thread{

        protected void processIncomingMessage(String message){
            ConsoleHelper.writeMessage(message);
        }
        protected void informAboutAddingNewUser(String userName){
            ConsoleHelper.writeMessage(userName+" присоединился к чату.");
        }
        protected void informAboutDeletingNewUser(String userName){
            ConsoleHelper.writeMessage(userName+" покинул чат.");
        }
        protected void notifyConnectionStatusChanged(boolean clientConnected){
            synchronized (Client.this){
            Client.this.clientConnected=clientConnected;
            Client.this.notify();
            }
        }
        protected void clientHandshake() throws IOException, ClassNotFoundException{
            while(true){
                Message inMessage=connection.receive();
                MessageType type=inMessage.getType();
                if (type==MessageType.NAME_REQUEST) {
                    String userName = getUserName();
                    Message outMessage = new Message(MessageType.USER_NAME, userName);
                    connection.send(outMessage);
                    }
                else if (type==MessageType.NAME_ACCEPTED) {
                    notifyConnectionStatusChanged(true);
                    return;
                    }
                else throw new IOException("Unexpected MessageType");

            }
        }
        protected void clientMainLoop() throws IOException, ClassNotFoundException{

            while (true){
                Message messageIn=connection.receive();
                MessageType type=messageIn.getType();
                String data=messageIn.getData();
                if(type==MessageType.TEXT) processIncomingMessage(data);
                else if(type==MessageType.USER_ADDED) informAboutAddingNewUser(data);
                else if (type==MessageType.USER_REMOVED) informAboutDeletingNewUser(data);
                else throw new IOException("Unexpected MessageType");

            }
        }

        @Override
        public void run() {
            try {
                String serverAddress=getServerAddress();
                int serverPort=getServerPort();
                Socket socket=new Socket(serverAddress,serverPort);
                connection=new Connection(socket);
                clientHandshake();
                clientMainLoop();
            } catch (IOException e) {
                notifyConnectionStatusChanged(false);
            } catch (ClassNotFoundException e) {
                notifyConnectionStatusChanged(false);
            }

        }
    }

    public static void main(String[] args) {
        Client client=new Client();
        client.run();
    }
}
