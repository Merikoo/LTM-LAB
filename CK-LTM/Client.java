package projec;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;

public class Client extends JFrame {
    private static final String SERVER_IP = "127.0.0.1";
    private static final int SERVER_PORT = 5000;

    private JTextArea textArea;
    private JTextField keyField;
    private JLabel responseLabel;
    private JFileChooser fileChooser;

    public Client() {
        setTitle("Chat Client");
        setSize(800, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        initUI();
        setVisible(true);
    }

    private void initUI() {
    fileChooser = new JFileChooser();
    JTabbedPane tabbedPane = new JTabbedPane();

    // Panel for Chat Client
    JPanel clientPanel = new JPanel(new BorderLayout(15, 15));
    clientPanel.setBackground(Color.LIGHT_GRAY);
    clientPanel.setBorder(BorderFactory.createLineBorder(new Color(0xADD8E6), 8)); // 🌟 Viền xanh nhạt

    textArea = new JTextArea(5, 30);
    keyField = new JTextField(5);
    JButton sendTextButton = new JButton("Gửi văn bản");
    sendTextButton.addActionListener(e -> sendEncryptedText());
    sendTextButton.setBackground(new Color(0x87CEFA)); // 🌟 Nút xanh nhạt
    sendTextButton.setForeground(Color.WHITE);

    JPanel textPanel = new JPanel(new BorderLayout());
    textPanel.setBorder(BorderFactory.createTitledBorder("Mã hóa Caesar"));
    textPanel.add(new JScrollPane(textArea), BorderLayout.CENTER);

    JPanel bottomText = new JPanel();
    bottomText.add(new JLabel("Khóa:"));
    bottomText.add(keyField);
    bottomText.add(sendTextButton);
    textPanel.add(bottomText, BorderLayout.SOUTH);

    // Panel for File Section
    JButton sendFileButton = new JButton("Chọn & Gửi File");
    sendFileButton.addActionListener(e -> sendFile());
    sendFileButton.setBackground(new Color(0x87CEFA)); // 🌟 Nút xanh nhạt
    sendFileButton.setForeground(Color.WHITE);

    JPanel filePanel = new JPanel();
    filePanel.setBorder(BorderFactory.createTitledBorder("Gửi File"));
    filePanel.add(sendFileButton);

    // Response Section
    responseLabel = new JLabel("<html><i>Chưa có phản hồi...</i></html>");
    JPanel responsePanel = new JPanel();
    responsePanel.setBorder(BorderFactory.createTitledBorder("Phản hồi"));
    responsePanel.add(responseLabel);

    clientPanel.add(textPanel, BorderLayout.NORTH);
    clientPanel.add(filePanel, BorderLayout.CENTER);
    clientPanel.add(responsePanel, BorderLayout.SOUTH);

    // Server panel bọc client panel
    JPanel serverPanel = new JPanel(new BorderLayout(15, 15));
    serverPanel.setBackground(Color.LIGHT_GRAY);
    serverPanel.setBorder(BorderFactory.createLineBorder(new Color(0xADD8E6), 8)); // 🌟 Viền xanh nhạt
    serverPanel.add(clientPanel);

    tabbedPane.addTab("Chat Client", clientPanel);
    add(tabbedPane);
}


    private void sendEncryptedText() {
        String text = textArea.getText().trim();
        int key = Integer.parseInt(keyField.getText().trim());

        if (!text.isEmpty() && key != 0) {
            try (Socket socket = new Socket(SERVER_IP, SERVER_PORT)) {
                OutputStream rawOut = socket.getOutputStream();
                PrintWriter out = new PrintWriter(rawOut, true);
                out.println("TEXT");
                out.println(text);
                out.println(key);

                InputStream rawIn = socket.getInputStream();
                BufferedReader in = new BufferedReader(new InputStreamReader(rawIn));
                String response = in.readLine();
                responseLabel.setText("<html>" + response.replace("\n", "<br>") + "</html>");
            } catch (IOException e) {
                responseLabel.setText("❌ Không thể kết nối đến server!");
            }
        } else {
            responseLabel.setText("❌ Vui lòng nhập văn bản và khóa hợp lệ!");
        }
    }

   private void sendFile() {
    int returnValue = fileChooser.showOpenDialog(this);
    if (returnValue == JFileChooser.APPROVE_OPTION) {
        File file = fileChooser.getSelectedFile();

        // Sử dụng luồng riêng để không làm đơ giao diện
        new Thread(() -> {
            try (Socket socket = new Socket(SERVER_IP, SERVER_PORT)) {
                OutputStream rawOut = socket.getOutputStream();
                DataOutputStream dataOut = new DataOutputStream(rawOut);

                // Gửi lệnh "FILE" trước tiên
                PrintWriter out = new PrintWriter(rawOut, true);
                out.println("FILE");

                // Gửi tên file và kích thước
                dataOut.writeUTF(file.getName());
                dataOut.writeLong(file.length());

                try (FileInputStream fileIn = new FileInputStream(file)) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = fileIn.read(buffer)) != -1) {
                        dataOut.write(buffer, 0, bytesRead);
                    }
                }

                // Nhận phản hồi từ server
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String response = in.readLine();
                SwingUtilities.invokeLater(() -> {
                    responseLabel.setText("<html>" + response + "</html>");
                });

            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> {
                    responseLabel.setText("❌ Lỗi khi gửi file: " + e.getMessage());
                });
            }
        }).start(); // Bắt đầu luồng mới
    }
}


    public static void main(String[] args) {
        SwingUtilities.invokeLater(Client::new);
    }
}
