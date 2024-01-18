package Client;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Client {
  private static final String SERVER_IP = "localhost";
  private static final int SERVER_PORT = 8000;
  private static String clientName = "";
  private static List<String> clientChatHistory = new ArrayList<>();
  private static PrintWriter writer;
  private static BufferedReader reader;
  private static Scanner scanner = new Scanner(System.in);
  private static List<String> receivedChatList = new ArrayList<>();
  private static Thread chatHistoryThread;
  private static boolean chatHistoryReceived = false;

  public static void main(String[] args) {
    try {
      Socket socket = new Socket(SERVER_IP, SERVER_PORT);
      System.out.println("서버에 연결됨.");

      reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      writer = new PrintWriter(socket.getOutputStream(), true);

      System.out.println("사용자 이름을 입력하세요: ");
      clientName = scanner.nextLine();
      writer.println(clientName);

      chatHistoryThread = new Thread(() -> {
        while (true) {
          showChatHistory();
        }
      });
      chatHistoryThread.start();

      new Thread(() -> {
        try {
          while (true) {
            String messageFromServer = reader.readLine();
            System.out.println(messageFromServer);
            receivedChatList.add(messageFromServer);

            if (messageFromServer.contains("종료했습니다.")) {
              System.exit(0);
            }
          }
        } catch (IOException e) {
          e.printStackTrace();
        }
      }).start();

      new Thread(() -> {
        while (true) {
          System.out.println("메시지를 입력하세요: ");
          String message = scanner.nextLine();

          if (message.equalsIgnoreCase("quit")) {
            writer.println("quit");
            System.exit(0);
          } else if (message.equalsIgnoreCase("[채팅목록]")) {
            showChatHistory();
          } else {
            sendMessage(message);
          }
        }
      }).start();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static void showChatHistory() {
    try {
      if (clientChatHistory.isEmpty() && !chatHistoryReceived) {
        writer.println("[채팅목록]");
        String chat;
        while (!(chat = reader.readLine()).equals("[채팅목록 끝]")) {
          clientChatHistory.add(chat);
        }
        chatHistoryReceived = true;
      }

      System.out.println("[채팅목록]");
      for (int i = 0; i < clientChatHistory.size(); i++) {
        System.out.println(i + 1 + ". " + clientChatHistory.get(i));
      }

      System.out.println("답장할 채팅을 선택하세요 (번호 입력, 예: 1): ");
      String selectedChatIndexStr = scanner.nextLine();

      int selectedChatIndex = Integer.parseInt(selectedChatIndexStr);

      if (selectedChatIndex > 0 && selectedChatIndex <= clientChatHistory.size()) {
        String selectedChat = clientChatHistory.get(selectedChatIndex - 1);
        System.out.println("선택한 채팅:" + selectedChat);

        System.out.println("답장을 입력하세요: ");
        String replyMessage = scanner.nextLine();

        String originalChatIndex = getChatIndexByTime(selectedChat);
        writer.println("[답장]" + originalChatIndex + " " + replyMessage);
      } else {
        System.out.println("올바른 번호를 입력하세요.");
      }
    } catch (NumberFormatException | IOException e) {
      e.printStackTrace();
    }
  }

  private static void sendMessage(String message) {
    message = parseSpecialCodesOnClient(message);
    LocalDateTime currentTime = LocalDateTime.now();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    String formattedTime = currentTime.format(formatter);
    writer.println(clientName + ": " + message + " (" + formattedTime + ")");
  }

  private static String parseSpecialCodesOnClient(String message) {
    message = message.replaceAll("1.", "\uD83D\uDE00");
    message = message.replaceAll("2.", "\uD83D\uDE01");
    message = message.replaceAll("3.", "\uD83D\uDE04");
    message = message.replaceAll("4.", "\uD83D\uDE03");
    message = message.replaceAll("5.", "\uD83D\uDE42");
    message = message.replaceAll("6.", "\uD83D\uDE06");
    message = message.replaceAll("7.", "\uD83E\uDD23");
    message = message.replaceAll("8.", "\uD83D\uDE09");
    return message;
  }

  private static String getChatIndexByTime(String selectedChat) {
    String[] parts = selectedChat.split("\\(");
    if (parts.length > 1) {
      String timePart = parts[1];
      timePart = timePart.substring(0, timePart.length() - 1).trim(); // Remove the trailing ')' and trim spaces

      // Instead of parsing the time string to an integer, directly return the time string
      return timePart;
    }
    return "";
  }
}
