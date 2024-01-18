package Server;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Server {
  private static final int PORT = 8000;
  private static List<PrintWriter> clientWriters = new ArrayList<>();
  private static Map<PrintWriter, String> clientNames = new HashMap<>();
  private static List<String> chatHistory = new ArrayList<>();
  private static Map<String, List<String>> clientChatHistoryMap = new HashMap<>();

  private static synchronized void broadcastMessage(String message, PrintWriter sender) {
    for (PrintWriter writer : clientWriters) {
      if (writer != sender) {
        writer.println(message);
      }
    }
  }

  private static synchronized void sendChatHistory(PrintWriter writer, List<String> chatHistory) {
    for (String chat : chatHistory) {
      writer.println(chat);
    }
  }

  public static void main(String[] args) {
    try {
      ServerSocket serverSocket = new ServerSocket(PORT);
      System.out.println("서버 시작. 클라이언트 연결 대기 중...");

      while (true) {
        Socket clientSocket = serverSocket.accept();
        System.out.println("클라이언트 연결됨.");

        BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);

        // 사용자 이름 입력 받음
        String clientName = reader.readLine();
        clientNames.put(writer, clientName);

        // 클라이언트가 접속했음을 모든 클라이언트에게 알림
        broadcastMessage(clientName + "님이 접속했습니다.", null);

        // 새로운 클라이언트에게 기존 채팅 내용 전송
        sendChatHistory(writer, chatHistory);

        clientWriters.add(writer);

        new Thread(() -> {
          try {
            String message;
            while ((message = reader.readLine()) != null) {
              if (message.equalsIgnoreCase("quit")) {
                // 클라이언트가 종료했음을 모든 클라이언트에게 알림
                broadcastMessage(clientName + "님이 종료했습니다.", writer);
                break;
              } else if (message.equalsIgnoreCase("[채팅목록]")) {
                // 클라이언트가 채팅 목록을 요청했을 때 현재 채팅 내용 전송
                sendChatHistory(writer, chatHistory);
              } else {
                // 채팅 기록에 추가
                chatHistory.add(clientName + ": " + message);
                clientChatHistoryMap.computeIfAbsent(clientName, k -> new ArrayList<>())
                    .add(clientName + ": " + message);
                broadcastMessage(clientName + ": " + message, writer);
              }
            }
          } catch (IOException e) {
            e.printStackTrace();
          } finally {
            // 클라이언트가 종료했을 때 해당 클라이언트의 PrintWriter 및 이름 제거
            clientWriters.remove(writer);
            clientNames.remove(writer);

            try {
              reader.close();
              writer.flush();
              writer.close();
              clientSocket.close();
            } catch (IOException e) {
              e.printStackTrace();
            }
          }
        }).start();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
