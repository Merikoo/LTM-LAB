import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class Server {

    private static final int SERVER_PORT = 1234;

    public static void main(String[] args) {
        System.out.println("Server đang chạy...");
        try (ServerSocket serverSocket = new ServerSocket(SERVER_PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Kết nối mới từ " + clientSocket.getInetAddress());
                new Thread(new ClientHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            System.err.println("Lỗi khi chạy server: " + e.getMessage());
        }
    }

    private static class ClientHandler implements Runnable {
        private Socket clientSocket;

        public ClientHandler(Socket socket) {
            this.clientSocket= socket;
        }

        @Override
        public void run() {
            try (
                BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);
                DataInputStream dataInputStream = new DataInputStream(clientSocket.getInputStream())
            ) {
                String requestType = reader.readLine();
                if ("TEXT".equalsIgnoreCase(requestType)) {
                    handleText(reader, writer);
                } else if ("FILE".equalsIgnoreCase(requestType)) {
                    handleFile(dataInputStream, reader, writer);
                }
            } catch (IOException e) {
                System.err.println("Lỗi xử lý client: " + e.getMessage());
            }
        }

        private void handleText(BufferedReader reader, PrintWriter writer) throws IOException {
            String encryptedText = reader.readLine();
            int key = Integer.parseInt(reader.readLine());
            String decryptedText = CeasarCipher.decrypt(encryptedText, key);

            // Đếm số lượng các chữ cái
            Map<Character, Integer> letterFrequencies = countLetterFrequencies(decryptedText);

            // Gửi kết quả cho client
            writer.println("Bản rõ: " + decryptedText);
            writer.println("Tần suất chữ cái:");
            for (Map.Entry<Character, Integer> entry : letterFrequencies.entrySet()) {
                writer.println(entry.getKey() + ": " + entry.getValue());
            }
            writer.println("END");

            // Lưu vào csdl
            DatabaseHelper.saveTextMessage(encryptedText, decryptedText, key, letterFrequencies.toString());
        }

        private void handleFile(DataInputStream dataInputStream, BufferedReader reader, PrintWriter writer) throws IOException {
            String fileName = dataInputStream.readUTF();
            long fileSize = dataInputStream.readLong();
            File file = new File("received_files/" + fileName);
            file.getParentFile().mkdirs();

            try (FileOutputStream fos = new FileOutputStream(file)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                long totalRead = 0;

                while (totalRead < fileSize && (bytesRead = dataInputStream.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                    totalRead += bytesRead;
                }
            }

            writer.println("File nhận thành công: " + fileName);

            // Lưu thông tin file vào csdl
            DatabaseHelper.saveFileInfo(fileName, file.getAbsolutePath(), fileSize);
        }

        private Map<Character, Integer> countLetterFrequencies(String text) {
            Map<Character, Integer> frequencies = new HashMap<>();
            for (char c : text.toCharArray()) {
                if (Character.isLetter(c)) {
                    c = Character.toLowerCase(c);
                    frequencies.put(c, frequencies.getOrDefault(c, 0) + 1);
                }
            }
            return frequencies;
        }
    }

    public static class CeasarCipher {
        public static String encrypt(String text, int key) {
            StringBuilder result = new StringBuilder();
            for (char character : text.toCharArray()) {
                if (Character.isLetter(character)) {
                    char base = Character.isUpperCase(character) ? 'A' : 'a';
                    result.append((char) ((character - base + key) % 26 + base));
                } else {
                    result.append(character);
                }
            }
            return result.toString();
        }

        public static String decrypt(String text, int key) {
            return encrypt(text, 26 - key);
        }
    }
}
