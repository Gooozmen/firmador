package ar.com.rgp;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class SocketServer
{
    public static Socket clientSocket = null;
    public static ServerSocket serverSocket = null;

    public static InputStreamReader inputStreamReader = null;
    public static OutputStreamWriter outputStreamWriter = null;
    public static BufferedWriter bufferedWriter = null;
    public static BufferedReader bufferedReader = null;


    public void StartServer() throws IOException {

        serverSocket = new ServerSocket(9678);

        while(true)
        {
            try
            {
                clientSocket = serverSocket.accept();

                inputStreamReader = new InputStreamReader(clientSocket.getInputStream());
                outputStreamWriter = new OutputStreamWriter(clientSocket.getOutputStream());

                bufferedReader = new BufferedReader(inputStreamReader);
                bufferedWriter = new BufferedWriter(outputStreamWriter);


                while(true)
                {
                    String clientMessage = bufferedReader.readLine();

                    System.out.println("Client Message: "+clientMessage);

                    bufferedWriter.write("Message Received.");
                    bufferedWriter.newLine();
                    bufferedWriter.flush();

                    if(clientMessage == null)
                        break;

                    if(clientMessage.equalsIgnoreCase("CLOSE"))
                        break;
                }

                clientSocket.close();
                inputStreamReader.close();
                outputStreamWriter.close();
                bufferedReader.close();
                bufferedWriter.close();
            }
            catch(IOException e)
            {
                e.printStackTrace();
            }
        }
    }


}
