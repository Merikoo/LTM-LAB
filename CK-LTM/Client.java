/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
//package project;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {

    private static final String SERVER_IP = "127.0.0.1"; 
    private static final int SERVER_PORT = 1234;
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

        public static String decrypt(String text, int shift) {
            return encrypt(text, 26 - shift);
        }
    }

    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_IP, SERVER_PORT);
             PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             DataOutputStream dataOut = new DataOutputStream(socket.getOutputStream());
             Scanner scanner = new Scanner(System.in)) {

            System.out.println("Kết nối đến Server thành công!");
            System.out.println("Chọn chức năng:");
            System.out.println("1. Gửi văn bản (Ceasar mã hóa)");
            System.out.println("2. Gửi file");

            int choice = scanner.nextInt();
            scanner.nextLine(); 

            if (choice == 1) {
                System.out.print("Nhập văn bản: ");
                String message = scanner.nextLine();
                System.out.print("Nhập khóa mã hóa Ceasar: ");
                int key = scanner.nextInt();
                scanner.nextLine();

                String encrypted = CaesarCipher.encrypt(message, key);

                writer.println("TEXT");
                writer.println(encrypted);
                writer.println(key);

                System.out.println("Kết quả từ Server:");
                String line;
                while (!(line = reader.readLine()).equals("END")) {
                    System.out.println(line);
                }

            } else if (choice == 2) {
                System.out.print("Nhập đường dẫn file cần gửi: ");
                String filePath = scanner.nextLine();
                File file = new File(filePath);

                if (!file.exists()) {
                    System.out.println("File không tồn tại.");
                    return;
                }

                writer.println("FILE");
                writer.flush();
                dataOut.writeUTF(file.getName());
                dataOut.writeLong(file.length());

                try (FileInputStream fis = new FileInputStream(file)) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = fis.read(buffer)) != -1) {
                        dataOut.write(buffer, 0, bytesRead);
                    }
                }

                System.out.println("Đã gửi file thành công!");
                String response = reader.readLine();
                System.out.println("Phản hồi từ Server: " + response);

            } else {
                System.out.println("Lựa chọn không hợp lệ.");
            }

        } catch (IOException e) {
            System.err.println("Lỗi kết nối đến Server: " + e.getMessage());
        }
    }
}
