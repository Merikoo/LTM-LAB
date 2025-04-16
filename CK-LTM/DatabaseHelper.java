import java.sql.*;

public class DatabaseHelper {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/chat_data";
    private static final String USER = "root";
    private static final String PASSWORD = "your_password_here"; // sửa lại cho đúng

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver"); // MySQL 8+
        } catch (ClassNotFoundException e) {
            System.err.println("Không tìm thấy JDBC Driver");
        }
    }

    public static void saveTextMessage(String encrypted, String decrypted, int key, String frequencies) {
        String sql = "INSERT INTO text_messages (encrypted_text, decrypted_text, key_value, letter_frequencies) VALUES (?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, encrypted);
            pstmt.setString(2, decrypted);
            pstmt.setInt(3, key);
            pstmt.setString(4, frequencies);
            pstmt.executeUpdate();

            System.out.println("Đã lưu văn bản vào CSDL.");

        } catch (SQLException e) {
            System.err.println("Lỗi lưu vào CSDL: " + e.getMessage());
        }
    }

    public static void saveFileInfo(String fileName, String filePath, long size) {
        String sql = "INSERT INTO received_files (filename, filepath, size) VALUES (?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, fileName);
            pstmt.setString(2, filePath);
            pstmt.setLong(3, size);
            pstmt.executeUpdate();

            System.out.println("Đã lưu file vào CSDL.");

        } catch (SQLException e) {
            System.err.println("Lỗi lưu file vào CSDL: " + e.getMessage());
        }
    }
}
