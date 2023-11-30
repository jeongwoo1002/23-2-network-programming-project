package Server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class MainServer implements Runnable {
  private static final int PORT = 8000;
  private ServerSocket serverSocket;
  private List<PrintWriter> clientWriters;
  private HashMap<String, PrintWriter> clientMap;
  private List<String> loggedInUsers;
  private static ArrayList<HashMap<String, String>> users = new ArrayList<>();
  private static final String EXIT_COMMAND = "exit";

  public MainServer() {
    this.clientWriters = new CopyOnWriteArrayList<>();
    this.clientMap = new HashMap<>();
    this.loggedInUsers = new ArrayList<>();
    initializeUsers();
  }

  public static ArrayList<HashMap<String, String>> getUsers(){
    return users;
  }

  private void initializeUsers(){
    addUser("user1", "정우");
    addUser("user2", "민성");
    addUser("user3", "진모");
  }

  private void addUser(String id, String name){
    HashMap<String, String> user = new HashMap<>();
    user.put("id", id);
    user.put("name", name);
    users.add(user);
  }

  @Override
  public void run() {
    try {
      serverSocket = new ServerSocket(PORT);
      System.out.println("서버 시작. 클라이언트 연결 대기 중...");

      while (true) {
        Socket clientSocket = serverSocket.accept();
        System.out.println("클라이언트 연결됨.");

        PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);
        clientWriters.add(writer);

        // Create a new thread for each client
        new Thread(() -> handleClient(clientSocket, writer)).start();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    finally {
      closeServer();
    }
  }

  private void closeServer(){
    try {
      serverSocket.close();
      System.out.println("서버종료");
      System.exit(0);
    }catch (IOException e){
      e.printStackTrace();
    }
  }

  private synchronized void handleClient(Socket clientSocket, PrintWriter writer) {
    try {
      String clientAddress = clientSocket.getInetAddress().getHostAddress();
      clientMap.put(clientAddress, writer);

      BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
      String id = reader.readLine();
      String password = reader.readLine();

      // 사용자가 이미 로그인되어 있는지 확인
      if (loggedInUsers.contains(id)) {
        writer.println("이미 다른 사용자가 로그인 중입니다.");
        clientSocket.close();
        return;
      }

      loggedInUsers.add(id);
      sendUsersListToAllClients(); // 모든 클라이언트에게 새로운 로그인을 알림

      writer.println("로그인 성공");

      String message;

      while ((message = reader.readLine()) != null) {
        broadcast(message, writer);

        if (message.equals(EXIT_COMMAND)){
          closeServer();
          return;
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      try {
        clientSocket.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  private void broadcast(String message, PrintWriter sender){
    List<PrintWriter> disconnectedClients = new ArrayList<>();

    for(PrintWriter clientWriter : clientWriters){
      try {
        clientWriter.println(message);
      }catch (Exception e){
        disconnectedClients.add(clientWriter);
      }
    }

    clientWriters.removeAll(disconnectedClients);
  }

  private void sendUsersListToAllClients() {
    StringBuilder usersListStr = new StringBuilder("현재 접속된 사용자 ID: ");

    for (String user : loggedInUsers) {
      usersListStr.append(user).append(", ");
    }

    // 마지막 쉼표 제거
    if (usersListStr.length() > 1) {
      usersListStr.delete(usersListStr.length() - 2, usersListStr.length());
    }

    // 클라이언트에게 메시지로 전송
    List<PrintWriter> disconnectedClients = new ArrayList<>();
    for (PrintWriter clientWriter : clientWriters) {
      try {
        clientWriter.println(usersListStr.toString());
      } catch (Exception e) {
        // 에러 발생 시 해당 클라이언트를 리스트에 추가
        disconnectedClients.add(clientWriter);
      }
    }

    // 끊긴 클라이언트 제거
    clientWriters.removeAll(disconnectedClients);
  }
  public static void main(String[] args){
    new MainServer().run();
//    MainServer mainServer = new MainServer();
//    mainServer.initializeUsers();
//    mainServer.run();
  }
}
