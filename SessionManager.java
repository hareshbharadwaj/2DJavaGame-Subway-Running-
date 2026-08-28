import java.io.*;

public class SessionManager {
    private static final String SESSION_FILE = "session.dat";

    public static void saveSession(String username) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(SESSION_FILE))) {
            writer.println(username);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String loadSession() {
        File file = new File(SESSION_FILE);
        if (!file.exists()) {
            return null;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            return reader.readLine();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void clearSession() {
        File file = new File(SESSION_FILE);
        if (file.exists()) {
            file.delete();
        }
    }
}
