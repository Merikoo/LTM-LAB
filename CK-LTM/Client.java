//package projec;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.*;
import java.net.*;

public class Client extends JFrame {
    private static final String SERVER_IP = "127.0.0.1";
    private static final int SERVER_PORT = 1234;

    private JTextArea textArea;
    private JTextField keyField;
    private JButton sendTextButton;
    private JButton sendFileButton;
    private JLabel responseLabel;
    private JFileChooser fileChooser;

    public Client() {
        setTitle("Client Caesar & File Sender");
        setSize(600, 500);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        initUI();
        setVisible(true);
    }

    private void initUI() {
        JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        mainPanel.setBackground(new Color(240, 248, 255));
        JPanel textPanel = new JPanel(new BorderLayout(10, 10));
        textPanel.setBorder(BorderFactory.createTitledBorder("Gửi văn bản mã hóa Caesar"));
        textPanel.setBackground(Color.WHITE);

        textArea = new JTextArea(5, 30);
        textArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        textArea.setLineWrap(true);
        JScrollPane scrollPane = new JScrollPane(textArea);

        keyField = new JTextField(5);
        keyField.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        sendTextButton = new JButton("Gửi văn bản");
        sendTextButton.setBackground(new Color(0x007BFF));
        sendTextButton.setForeground(Color.WHITE);

        JPanel keyPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        keyPanel.setBackground(Color.WHITE);
        keyPanel.add(new JLabel("Khóa Caesar:"));
        keyPanel.add(keyField);
        keyPanel.add(sendTextButton);

        textPanel.add(scrollPane, BorderLayout.CENTER);
        textPanel.add(keyPanel, BorderLayout.SOUTH);
        JPanel filePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filePanel.setBorder(BorderFactory.createTitledBorder("Gửi File"));
        filePanel.setBackground(new Color(250, 250, 250));

        sendFileButton = new JButton("Chọn & Gửi File");
        sendFileButton.setBackground(new Color(0x28A745));
        sendFileButton.setForeground(Color.WHITE);
        filePanel.add(sendFileButton);
        JPanel responsePanel = new JPanel();
        responsePanel.setBorder(BorderFactory.createTitledBorder("Phản hồi từ Server"));
        responsePanel.setBackground(Color.WHITE);

        responseLabel = new JLabel("<html><i>Chưa có phản hồi...</i></html>");
        responseLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        responsePanel.add(responseLabel);

        // Add panels to main layout
        mainPanel.add(textPanel, BorderLayout.NORTH);
        mainPanel.add(filePanel, BorderLayout.CENTER);
        mainPanel.add(responsePanel, BorderLayout.SOUTH);
        add(mainPanel);

        fileChooser = new JFileChooser();
        sendTextButton.addActionListener(e -> sendEncryptedText());
        sendFileButton.addActionListener(e -> sendFile());
    }

    private void sendEncryptedText() {
        try (
            Socket socket = new Socket(SERVER_IP, SERVER_PORT);
            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))
        ) {
            System.out.println("Đã kết nối tới Server (TEXT)");

            String message = textArea.getText().trim();
            int key = Integer.parseInt(keyField.getText().trim());
            String encrypted = CaesarCipher.encrypt(message, key);
            writer.println("TEXT");
            writer.println(encrypted);
            writer.println(key);
            StringBuilder response = new StringBuilder("<html>");
            String line;
            while (!(line = reader.readLine()).equals("END")) {
                response.append(" ").append(line).append("<br>");
            }
            response.append("</html>");
            responseLabel.setText(response.toString());

        } catch (IOException | NumberFormatException ex) {
            responseLabel.setText("<html><span style='color:red'>Lỗi: " + ex.getMessage() + "</span></html>");
        }
    }

    private void sendFile() {
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try (
                Socket socket = new Socket(SERVER_IP, SERVER_PORT);
                DataOutputStream dataOut = new DataOutputStream(socket.getOutputStream());
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
                FileInputStream fis = new FileInputStream(file)
            ) {
                System.out.println("Đã kết nối tới Server (FILE)");
                writer.println("FILE");
                dataOut.writeUTF(file.getName());
                dataOut.writeLong(file.length());
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    dataOut.write(buffer, 0, bytesRead);
                }
                String response = reader.readLine();
                responseLabel.setText("<html><span style='color:green'> " + response + "</span></html>");
            } catch (IOException ex) {
                responseLabel.setText("<html><span style='color:red'>Lỗi gửi file: " + ex.getMessage() + "</span></html>");
            }
        }
    }

    public static class CaesarCipher {
        public static String encrypt(String text, int shift) {
            StringBuilder result = new StringBuilder();
            for (char c : text.toCharArray()) {
                if (Character.isLetter(c)) {
                    char base = Character.isUpperCase(c) ? 'A' : 'a';
                    result.append((char) ((c - base + shift) % 26 + base));
                } else {
                    result.append(c);
                }
            }
            return result.toString();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Client::new);
    }
}
