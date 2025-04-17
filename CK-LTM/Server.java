package projec;
import java.util.List;
import java.util.ArrayList;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.*;

public class Server extends JFrame {
    private JTextArea logArea;
    private ServerSocket serverSocket;

    private static final String DB_URL = "jdbc:mysql://localhost:3306/dtb?useSSL=false&serverTimezone=UTC";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "newpassword123";
    private static final List<Socket> clients = new ArrayList<>();
    public Server() {
        setTitle("Server GUI");
        setSize(700, 450);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        initUI();
        setLocationRelativeTo(null);
        setVisible(true);
        new Thread(this::startServer).start();
    }
    private void initUI() {
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        JScrollPane scrollPane = new JScrollPane(logArea);
        add(scrollPane, BorderLayout.CENTER);
    }

    private void startServer() {
        try {
            serverSocket = new ServerSocket(5000);
            log("🚀 Server đang chạy tại cổng 5000...");

            while (true) {
                Socket client = serverSocket.accept();
                clients.add(client);
                log("📥 Kết nối mới từ " + client.getInetAddress().getHostAddress());
                new Thread(() -> handleClient(client)).start();
            }
        } catch (IOException e) {
            log("❌ Lỗi khi khởi động server: " + e.getMessage());
        }
    }

private void handleClient(Socket client) {
    try (
        InputStream rawIn = client.getInputStream();
        BufferedReader in = new BufferedReader(new InputStreamReader(rawIn));
        PrintWriter out = new PrintWriter(client.getOutputStream(), true)
    ) {
        String command = in.readLine();
        if (command == null) {
            log("⚠️ Không nhận được lệnh từ client.");
            return;
        }

        log("📨 Nhận lệnh: " + command);

        if (command.equalsIgnoreCase("TEXT")) {
            // === XỬ LÝ VĂN BẢN CEASAR ===
            String encryptedText = in.readLine();
            int key = Integer.parseInt(in.readLine());

            log("🔐 Mã hóa: " + encryptedText + " | Khóa: " + key);
            String decryptedText = CeasarCipher.decrypt(encryptedText, key);
            log("📝 Giải mã: " + decryptedText);

            int[] freq = new int[26];
            for (char c : decryptedText.toCharArray()) {
                if (Character.isLetter(c)) {
                    freq[Character.toLowerCase(c) - 'a']++;
                }
            }

            StringBuilder freqStr = new StringBuilder();
            for (int i = 0; i < 26; i++) {
                if (freq[i] > 0) {
                    freqStr.append((char) ('a' + i)).append(": ").append(freq[i]).append("\n");
                }
            }

            storeMessage(encryptedText, decryptedText, key, freqStr.toString());

            out.println("Tần suất xuất hiện của các chữ cái:");
            out.println(freqStr);
            out.println("END");
            out.flush();
            log("📤 Đã gửi tần suất và lưu CSDL.");

        } else if (command.equalsIgnoreCase("FILE")) {
            // === XỬ LÝ FILE ===
            DataInputStream dis = new DataInputStream(client.getInputStream());

            String fileName = dis.readUTF();
            long fileSize = dis.readLong();
            log("📁 Nhận file: " + fileName + " (" + fileSize + " bytes)");

            File folder = new File("server_files");
            if (!folder.exists()) folder.mkdirs();

            File receivedFile = new File(folder, fileName);

            try (FileOutputStream fos = new FileOutputStream(receivedFile)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                long totalRead = 0;

                while (totalRead < fileSize && (bytesRead = dis.read(buffer, 0, (int) Math.min(buffer.length, fileSize - totalRead))) != -1) {
                    fos.write(buffer, 0, bytesRead);
                    totalRead += bytesRead;
                }
            }

            // Kiểm tra kết quả
            if (receivedFile.exists() && receivedFile.length() == fileSize) {
                saveReceivedFile(fileName, receivedFile.getAbsolutePath(), fileSize);
                out.println("✅ File nhận thành công: " + fileName);
                out.println("📦 Kích thước: " + fileSize + " bytes");
                log("✅ File đã lưu vào: " + receivedFile.getAbsolutePath());
            } else {
                out.println("❌ Lỗi khi lưu file.");
                log("❌ File ghi không đúng hoặc thiếu.");
            }

            out.println("END");
            out.flush();

        } else {
            out.println("❌ Lệnh không hợp lệ.");
            out.println("END");
            log("❗ Lệnh không hợp lệ từ client.");
        }

    } catch (Exception e) {
        log("❌ Lỗi xử lý client: " + e.getMessage());
        e.printStackTrace();
    } finally {
        try {
            client.close();
        } catch (IOException e) {
            log("❌ Lỗi đóng kết nối client.");
        }
    }
}


    private void storeMessage(String encryptedText, String decryptedText, int key, String frequencies) {
        int nextId = getNextId("text_messages");
        String sql = "INSERT INTO text_messages (id, encrypted_text, decrypted_text, key_value, letter_frequencies) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, nextId);
            stmt.setString(2, encryptedText);
            stmt.setString(3, decryptedText);
            stmt.setInt(4, key);
            stmt.setString(5, frequencies);
            stmt.executeUpdate();
            log("✅ Tin nhắn đã lưu vào bảng text_messages.");
        } catch (SQLException e) {
            log("❌ Lỗi SQL (TEXT): " + e.getMessage());
        }
    }

   private void saveReceivedFile(String filename, String filepath, long size) {
    int nextId = getNextId("received_files");
    String sql = "INSERT INTO received_files (id, filename, filepath, size) VALUES (?, ?, ?, ?)";

    try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
         PreparedStatement stmt = conn.prepareStatement(sql)) {

        stmt.setInt(1, nextId);
        stmt.setString(2, filename);
        stmt.setString(3, filepath);
        stmt.setLong(4, size);
        stmt.executeUpdate();
        log("✅ File đã lưu vào bảng received_files.");
    } catch (SQLException e) {
        log("❌ Lỗi SQL (FILE): " + e.getMessage());
    }
}


    private int getNextId(String tableName) {
        String sql = "SELECT MAX(id) FROM " + tableName;
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) return rs.getInt(1) + 1;
        } catch (SQLException e) {
            log("❌ Lỗi truy vấn ID: " + e.getMessage());
        }
        return 1;
    }

    private void log(String message) {
        SwingUtilities.invokeLater(() -> logArea.append(message + "\n"));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Server::new);
    }

    static class CeasarCipher {
        public static String decrypt(String text, int shift) {
            StringBuilder result = new StringBuilder();
            for (char c : text.toCharArray()) {
                if (Character.isLetter(c)) {
                    char base = Character.isLowerCase(c) ? 'a' : 'A';
                    result.append((char) ((c - base - shift + 26) % 26 + base));
                } else {
                    result.append(c);
                }
            }
            return result.toString();
        }
    }
}
