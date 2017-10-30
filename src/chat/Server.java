package chat;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    private static Map<String, Connection> connectionMap=new ConcurrentHashMap<>();

    public static void sendBroadcastMessage(Message message){
        for(Connection connection:connectionMap.values()){
            try{
                connection.send(message);
            }catch(IOException e){
                ConsoleHelper.writeMessage("Сообщение не было отправлено!");
            }
        }
    }

    private static class Handler extends Thread{
        private Socket socket;
        public Handler(Socket socket){
            this.socket=socket;
        }

        private void serverMainLoop(Connection connection, String userName )throws IOException, ClassNotFoundException{
            while(true){
                Message messageIn=connection.receive();
                MessageType type=messageIn.getType();
                String text=messageIn.getData();
                if(type==MessageType.TEXT){
                    text=userName+": "+text;
                    Message messageOut=new Message(MessageType.TEXT,text);
                    sendBroadcastMessage(messageOut);
                }
                else ConsoleHelper.writeMessage("Ошибка! Сообщение не формата TEXT");
            }
        }

        private void sendListOfUsers(Connection connection, String userName) throws IOException {
            for(String name:connectionMap.keySet()){
                if(!userName.equals(name)){
                    Message message=new Message(MessageType.USER_ADDED,name);
                    connection.send(message);
                }
            }
        }

        private String serverHandshake(Connection connection) throws IOException,ClassNotFoundException{

            connection.send(new Message(MessageType.NAME_REQUEST));
            Message receive=connection.receive();
            MessageType type=receive.getType();
            String name=receive.getData();

            if(type!=MessageType.USER_NAME)return serverHandshake(connection);
            else if(name.isEmpty()) return serverHandshake(connection);
            else if(connectionMap.get(name)!=null)return serverHandshake(connection);
            else {
                connectionMap.put(name,connection);
                connection.send(new Message(MessageType.NAME_ACCEPTED));
                return name;

            }


        }

        @Override
        public void run() {
            String newUserName=null;

            ConsoleHelper.writeMessage("Установлено новое соединение с удаленным адресом: "+socket.getRemoteSocketAddress());
          try(Connection connection=new Connection(socket)){
            newUserName=serverHandshake(connection);
            Message newUserMessage=new Message(MessageType.USER_ADDED,newUserName);
            sendBroadcastMessage(newUserMessage);
            sendListOfUsers(connection,newUserName);
            serverMainLoop(connection,newUserName);
          }catch (IOException e){
              ConsoleHelper.writeMessage("Произошла ошибка при обмене данными с удаленным адресом: ");
          }
          catch (ClassNotFoundException e){
              ConsoleHelper.writeMessage("Произошла ошибка при обмене данными с удаленным адресом");
          }
          finally {
              if(newUserName!=null){
                  connectionMap.remove(newUserName);
                  Message removeUserMessage=new Message(MessageType.USER_REMOVED,newUserName);
                  sendBroadcastMessage(removeUserMessage);
              }
              ConsoleHelper.writeMessage("Cоединение с удаленным адресом закрыто");
          }


        }
    }
    public static void main(String[] args) throws IOException {
        ConsoleHelper.writeMessage("Введите порт:");
        int serverPort=ConsoleHelper.readInt();
        try(ServerSocket serverSocket=new ServerSocket(serverPort)){
            ConsoleHelper.writeMessage("Сервер запущен");

            while(true){
                Socket socket=serverSocket.accept();
                Handler handler=new Handler(socket);
                handler.start();
            }
        }
        catch (Exception e){
            ConsoleHelper.writeMessage("Возникла ошибка "+e.getClass().getSimpleName());
        }



    }
}
