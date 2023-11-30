package Server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;

public class ServerThread implements Runnable {
  private Socket socket;
  private HashMap<String, PrintWriter> clientMap;
  private List<PrintWriter> clientWriters;
  private List<String> loggedInUsers;
  private BufferedReader reader;
  private PrintWriter writer;
  private MainServer mainServer;  // MainServer 인스턴스 추가

  public ServerThread(Socket socket, HashMap<String, PrintWriter> clientMap, List<PrintWriter> clientWriters, List<String> loggedInUsers, MainServer mainServer) {
    this.socket = socket;
    this.clientMap = clientMap;
    this.clientWriters = clientWriters;
    this.loggedInUsers = loggedInUsers;

    try {
      reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      writer = new PrintWriter(socket.getOutputStream(), true);
    } catch (IOException e) {
      e.printStackTrace();
    }

    this.mainServer = mainServer;
  }

  @Override
  public void run() {
    try {
      while (true) {
        String messageFromServer = reader.readLine();
        System.out.println(messageFromServer);

        boolean isRead = Boolean.parseBoolean(messageFromServer.substring(messageFromServer.lastIndexOf(" ") + 1));

        if(!isRead){
          System.out.println("메시지가 읽히지 않았습니다.");
        }

        String id = reader.readLine();
        String name = reader.readLine();

        if (isValidLogin(id, name)) {
          writer.println("로그인 성공");

          // login 성공시 connectedUsers에 사용자 정보 추가
          loggedInUsers.add(id);
          sendUsersListToClient();

          break;
        } else {
          writer.println("로그인 실패. 다시 시도하세요.");

          try {
            Thread.sleep(1000);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }
        writer.println(id + ": " + name + "(" + message + ")");
      }

      String message;
      while ((message = reader.readLine()) != null) {
        LocalDateTime currentTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formattedTime = currentTime.format(formatter);
        String messageWithTime = formattedTime + " " + message;

        // mainServer.getUsers()로 사용자 목록에 접근
        for (HashMap<String, String> user : mainServer.getUsers()) {
          // 사용자 목록에 대한 추가 로직 작성
        }

        broadcast(messageWithTime, writer);
      }
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      try {
        socket.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  private void sendMessageToClient(String message, String senderId) {
    for (PrintWriter client : clientWriters) {
      client.println(senderId + ":" + message);
      }
    }
//    writer.println(message);

  private boolean isValidLogin(String id, String password) {
    for (HashMap<String, String> user : MainServer.getUsers()) {
      if (user.get("id").equals(id) && user.get("name").equals(password)) {
        return true;
      }
    }
    return false;
  }

  private void sendUsersListToClient() {
    StringBuilder usersListStr = new StringBuilder("현재 접속된 사용자 ID: ");

    for (String user : loggedInUsers) {
      usersListStr.append(user).append(", ");
    }

    // 마지막 쉼표 제거
    if (usersListStr.length() > 1) {
      usersListStr.delete(usersListStr.length() - 2, usersListStr.length());
    }

    broadcast(usersListStr.toString(), null);

//    sendMessageToClient(usersListStr.toString());
  }

  private void broadcast(String message, PrintWriter sender) {
    for (PrintWriter client : clientWriters) {
      if (client != sender) {
        client.println(message);
      }
    }
  }
}
