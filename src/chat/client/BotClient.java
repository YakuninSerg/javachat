package chat.client;



import chat.ConsoleHelper;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class BotClient extends Client{
    @Override
    protected SocketThread getSocketThread() {
        return new BotSocketThread();
    }

    @Override
    protected boolean shouldSendTextFromConsole() {
        return false;
    }

    @Override
    protected String getUserName() {

        int number=(int)(Math.random()*100);
        return "date_bot_"+number;
    }

    public class BotSocketThread extends SocketThread{
        @Override
        protected void clientMainLoop() throws IOException, ClassNotFoundException {
            sendTextMessage("Привет чатику. Я бот. Понимаю команды: дата, день, месяц, год, время, час, минуты, секунды.");
            super.clientMainLoop();
        }

        @Override
        protected void processIncomingMessage(String message) {
            ConsoleHelper.writeMessage(message);
            if(message.contains(":")){
                String []str=message.split(": ",2);
                String name=str[0];
                String text=str[1];
                String pattern=null;
                if("дата".equals(text))pattern="d.MM.YYYY";
                if("день".equals(text))pattern="d";
                if("месяц".equals(text))pattern="MMMM";
                if("год".equals(text))pattern="YYYY";
                if("время".equals(text))pattern="H:mm:ss";
                if("час".equals(text))pattern="H";
                if("минуты".equals(text))pattern="m";
                if("секунды".equals(text))pattern="s";

                if(pattern!=null){
                    SimpleDateFormat dateFormat=new SimpleDateFormat(pattern);
                    Calendar calendar=Calendar.getInstance();
                    Date currentDate=calendar.getTime();
                    String date=dateFormat.format(currentDate);
                    String reply="Информация для "+name+": "+date;
                    sendTextMessage(reply);

                }


            }
            
        }
    }

    public static void main(String[] args) {
        BotClient botClient=new BotClient();
        botClient.run();
    }
}
