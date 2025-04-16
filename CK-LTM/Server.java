import java.io.*;
import java.net.*;

public class Server {
    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(1234)) {
            System.out.println("Server đang chạy...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Kết nối mới từ Client!");

                new Thread(() -> handleClient(clientSocket)).start();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleClient(Socket clientSocket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                InputStream socketIn = clientSocket.getInputStream()) {

            String command = in.readLine();

            if (command.equals("TEXT")) {
                String encryptedText = in.readLine();
                int key = Integer.parseInt(in.readLine());

                String decryptedText = CeasarCipher.decrypt(encryptedText, key);
                System.out.println("Văn bản giải mã: " + decryptedText);

                // Đếm số lượng xuất hiện của các chữ cái
                int[] frequency = new int[26];
                for (char c : decryptedText.toCharArray()) {
                    if (Character.isLetter(c)) {
                        frequency[Character.toLowerCase(c) - 'a']++;
                    }
                }
                // +
                StringBuilder freqResult = new StringBuilder();
                // +
                // Gửi kết quả về Client
                out.println("Tần suất xuất hiện của các chữ cái:");
                for (int i = 0; i < 26; i++) {
                    if (frequency[i] > 0) {
                        freqResult.append((char) (i + 'a'))
                                .append(": ").append(frequency[i]).append(", ");
                        out.println((char) (i + 'a') + ": " + frequency[i]);
                    }
                }
                DatabaseHelper.saveTextMessage(encryptedText, decryptedText, key, freqResult.toString());

            } else if (command.equals("FILE")) {
                String fileName = in.readLine();
                long fileSize = Long.parseLong(in.readLine());

                File receivedFile = new File("server_files/" + fileName);
                receivedFile.getParentFile().mkdirs();

                try (BufferedOutputStream fileOut = new BufferedOutputStream(new FileOutputStream(receivedFile))) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    long totalBytesRead = 0;

                    while (totalBytesRead < fileSize && (bytesRead = socketIn.read(buffer)) != -1) {
                        fileOut.write(buffer, 0, bytesRead);
                        totalBytesRead += bytesRead;
                    }

                    System.out.println("Đã nhận file: " + fileName);
                }
                // Lưu thông tin file vào CSDL
                DatabaseHelper.saveFileInfo(fileName, receivedFile.getAbsolutePath(), fileSize);

                // Gửi phản hồi lại cho client
                out.println("File đã được nhận và lưu.");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}