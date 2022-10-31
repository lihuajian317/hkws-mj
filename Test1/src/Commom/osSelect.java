package Commom;

public class osSelect {

    public static boolean isLinux() {
        if (System.getProperty("os.name").toLowerCase().contains("linux") ||
            System.getProperty("os.name").toLowerCase().contains("mac")) {
            return true;
        }
        return false;
    }

    public static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("windows");
    }

    /*public static void main(String[] args) {
        System.out.println(System.getProperty("os.name"));
    }*/
}
