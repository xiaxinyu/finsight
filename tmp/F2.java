import java.sql.*;
public class F2 {
    public static void main(String[] a) throws Exception {
        try (Connection c = DriverManager.getConnection(
                "jdbc:mysql://127.0.0.1:3306/finsight?useSSL=false&allowPublicKeyRetrieval=true", "root", "123456");
             Statement s = c.createStatement()) {
            try (ResultSet rs = s.executeQuery(
                    "SELECT installed_rank, version, success, script, execution_time FROM flyway_schema_history WHERE version='7'")) {
                while (rs.next()) {
                    System.out.println("rank=" + rs.getInt(1) + " success=" + rs.getInt(3) + " script=" + rs.getString(4));
                }
            }
            for (String t : new String[]{"medical", "_deprecated_medical", "endowment", "accumulation", "unemployment", "ben_contribution"}) {
                try (ResultSet rs = s.executeQuery(
                        "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=database() AND table_name='" + t + "'")) {
                    rs.next();
                    if (rs.getInt(1) > 0) {
                        try (ResultSet rs2 = s.executeQuery("SELECT COUNT(*) FROM `" + t + "`")) {
                            rs2.next();
                            System.out.println(t + " rows: " + rs2.getInt(1));
                        }
                    } else {
                        System.out.println(t + ": missing");
                    }
                }
            }
            try (ResultSet rs = s.executeQuery("SHOW COLUMNS FROM medical")) {
                System.out.println("medical columns:");
                while (rs.next()) System.out.println("  " + rs.getString(1) + " " + rs.getString(2));
            } catch (SQLException e) {
                System.out.println("medical columns: " + e.getMessage());
            }
            try (ResultSet rs = s.executeQuery("SHOW COLUMNS FROM _deprecated_medical")) {
                System.out.println("_deprecated_medical columns:");
                while (rs.next()) System.out.println("  " + rs.getString(1) + " " + rs.getString(2));
            } catch (SQLException e) {
                System.out.println("_deprecated_medical: " + e.getMessage());
            }
        }
    }
}
