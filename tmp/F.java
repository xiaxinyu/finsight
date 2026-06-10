import java.sql.*;
public class F {
    public static void main(String[] a) throws Exception {
        try (Connection c = DriverManager.getConnection(
                "jdbc:mysql://127.0.0.1:3306/finsight?useSSL=false&allowPublicKeyRetrieval=true", "root", "123456");
             Statement s = c.createStatement()) {
            try (ResultSet rs = s.executeQuery(
                    "SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank")) {
                while (rs.next()) {
                    System.out.println(rs.getString(1) + " | " + rs.getString(2) + " | success=" + rs.getInt(3));
                }
            }
            for (String t : new String[]{"ben_contribution", "imp_staging_entry", "fs_user", "cls_category", "fin_bank_account"}) {
                try (ResultSet rs = s.executeQuery(
                        "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=database() AND table_name='" + t + "'")) {
                    rs.next();
                    System.out.println(t + " exists: " + (rs.getInt(1) > 0));
                }
            }
        }
    }
}
