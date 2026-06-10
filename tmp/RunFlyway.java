import java.sql.*;
public class RunFlyway {
    public static void main(String[] a) throws Exception {
        try (Connection c = DriverManager.getConnection(
                "jdbc:mysql://127.0.0.1:3306/finsight?useSSL=false&allowPublicKeyRetrieval=true", "root", "123456");
             Statement s = c.createStatement()) {
            int deleted = s.executeUpdate("DELETE FROM flyway_schema_history WHERE version = '7' AND success = 0");
            System.out.println("deleted failed V7 rows: " + deleted);
        }
    }
}
