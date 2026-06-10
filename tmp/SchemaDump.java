import java.sql.*;

public class SchemaDump {
    public static void main(String[] args) throws Exception {
        try (Connection c = DriverManager.getConnection(
                "jdbc:mysql://127.0.0.1:3306/finsight?useSSL=false&allowPublicKeyRetrieval=true", "root", "123456");
             Statement s = c.createStatement()) {
            try (ResultSet rs = s.executeQuery("SHOW TABLES")) {
                java.util.List<String> tables = new java.util.ArrayList<>();
                while (rs.next()) tables.add(rs.getString(1));
                for (String table : tables) {
                    System.out.println("=== " + table + " ===");
                    try (ResultSet cols = s.executeQuery("SHOW COLUMNS FROM `" + table + "`")) {
                        while (cols.next()) {
                            System.out.println("  " + cols.getString(1) + " " + cols.getString(2));
                        }
                    }
                }
            }
        }
    }
}
