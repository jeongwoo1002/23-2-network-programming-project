// Client.java
package Client;

import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Client {
  private static String id;
  private static String name;

  public static void main(String[] args) {
    try {
      Socket socket = new Socket("localhost", 8000);
      System.out.println("서버에 연결됨.");

      BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);

      Scanner scanner = new Scanner(System.in);
      BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));

      boolean loggedIn = false;

      while (!loggedIn) {
        System.out.print("아이디 입력: ");
        id = consoleReader.readLine();

        System.out.print("이름 입력: ");
        name = consoleReader.readLine();

        writer.println(id);
        writer.println(name);

        try {
          String response = reader.readLine();

          if (response == null) {
            System.out.println("서버와의 연결이 끊어졌습니다.");
            break;
          }

           if (response.equals("로그인 성공")) {
            System.out.println("로그인 성공");
            loggedIn = true;
          } else {
            System.out.println("로그인 실패. 다시 시도하세요. ");
          }
        } catch (IOException e) {
          System.out.println("서버와의 통신 중 오류가 발생했습니다.");
          e.printStackTrace();
          break;
        }
      }

      new Thread(() -> {
        try {
          while (true) {
            String messageFromServer = reader.readLine();
            System.out.println(messageFromServer);
          }
        } catch (IOException e) {
          e.printStackTrace();
        }
      }).start();

      while (true) {
        System.out.println("메시지를 입력하세요: ");
        String message = scanner.nextLine();
        message = parseSpecialCodesOnClient(message);
        LocalDateTime currentTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formattedTime = currentTime.format(formatter);
        writer.println(id + ": " +  " (" + name + ")"+ message + " (" + formattedTime + ")");
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static String parseSpecialCodesOnClient(String message) {
    message = message.replaceAll("1.", "\uD83D\uDE00"); // 웃음 이모지의 유니코드
    message = message.replaceAll("2.", "\uD83D\uDE01"); // 웃는눈 이모지의 유니코드
    message = message.replaceAll("3.", "\uD83D\uDE04"); // 눈 감은 웃는 얼굴 이모지의 유니코드
    message = message.replaceAll("4.", "\uD83D\uDE03"); // 큰 미소 이모지의 유니코드
    message = message.replaceAll("5.", "\uD83D\uDE42"); // 미소 지은 얼굴 이모지의 유니코드
    message = message.replaceAll("6.", "\uD83D\uDE06"); // 기쁜 얼굴 이모지의 유니코드
    message = message.replaceAll("7.", "\uD83E\uDD23"); // 행복한 얼굴 이모지의 유니코드
    message = message.replaceAll("8.", "\uD83D\uDE09"); // 윙크하는 얼굴 이모지의 유니코드
    return message;
  }
}
