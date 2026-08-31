public class TestEnv {
    public static void main(String[] args) {
        String dbUrl = System.getenv().getOrDefault("DB_URL", "fallback");
        System.out.println("DB_URL is: " + dbUrl);
    }
}
