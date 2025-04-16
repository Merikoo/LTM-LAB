public class CeasarCipher {

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