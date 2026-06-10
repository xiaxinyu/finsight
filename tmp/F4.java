import java.sql.*;
public class F4 {
    public static void main(String[] a) throws Exception {
        try (Connection c = DriverManager.getConnection(
                "jdbc:mysql://127.0.0.1:3306/finsight?useSSL=false&allowPublicKeyRetrieval=true", "root", "123456");
             Statement s = c.createStatement()) {
            try (ResultSet rs = s.executeQuery("SELECT ID, LENGTH(ID) len FROM medical ORDER BY len DESC LIMIT 5")) {
                while (rs.next()) System.out.println(rs.getString(1) + " len=" + rs.getInt(2));
            }
            try (ResultSet rs = s.executeQuery("SELECT MAX(LENGTH(ID)) FROM medical")) {
                rs.next();
                System.out.println("max id len: " + rs.getInt(1));
            }
        }
    }
}
