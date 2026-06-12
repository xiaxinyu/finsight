import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

/** One-off: clear failed Flyway rows so migrate can retry. Usage: see repair-flyway.sh */
public class FlywayRepairFailed {
    public static void main(String[] args) throws Exception {
        String url = System.getenv().getOrDefault("SPRING_DATASOURCE_URL",
                "jdbc:mysql://127.0.0.1:3306/finsight?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
        String user = System.getenv().getOrDefault("SPRING_DATASOURCE_USERNAME", "root");
        String pass = System.getenv().getOrDefault("SPRING_DATASOURCE_PASSWORD", "123456");
        String version = args.length > 0 ? args[0] : null;

        try (Connection c = DriverManager.getConnection(url, user, pass);
             Statement s = c.createStatement()) {
            String where = version == null ? "success = 0" : "success = 0 AND version = '" + version.replace("'", "") + "'";
            try (ResultSet rs = s.executeQuery(
                    "SELECT installed_rank, version, description, success FROM flyway_schema_history WHERE " + where)) {
                System.out.println("Failed migrations to remove:");
                int n = 0;
                while (rs.next()) {
                    n++;
                    System.out.printf("  rank=%s version=%s success=%s desc=%s%n",
                            rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4));
                }
                if (n == 0) {
                    System.out.println("  (none)");
                    return;
                }
            }
            int deleted = s.executeUpdate("DELETE FROM flyway_schema_history WHERE " + where);
            System.out.println("Deleted rows: " + deleted);
        }
    }
}
