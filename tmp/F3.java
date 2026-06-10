import java.sql.*;
public class F3 {
    public static void main(String[] a) throws Exception {
        try (Connection c = DriverManager.getConnection(
                "jdbc:mysql://127.0.0.1:3306/finsight?useSSL=false&allowPublicKeyRetrieval=true", "root", "123456");
             Statement s = c.createStatement()) {
            String sql = "INSERT INTO ben_contribution (id, benefit_type, unit_no, unit_name, period_label, pay_base, personal_pay, unit_pay, total_pay, personal_reserved, memo, created_by, created_at, updated_by, updated_at, version) "
                    + "SELECT ID, 'MEDICAL', UNIT_NO, UNIT_NAME, TIME, PAY_BASE, PERSONAL_PAY, UNIT_PAY, TOTAL_PAY, PERSONAL_RESERVED, DEMOAREA, CREATEUSER, CREATETIME, UPDATEUSER, UPDATETIME, COALESCE(VERSION,0) "
                    + "FROM medical b WHERE NOT EXISTS (SELECT 1 FROM ben_contribution x WHERE x.id = b.ID) LIMIT 1";
            try {
                s.executeUpdate(sql);
                System.out.println("without backticks: OK");
            } catch (SQLException e) {
                System.out.println("without backticks: " + e.getMessage());
            }
            s.executeUpdate("DELETE FROM ben_contribution WHERE benefit_type='MEDICAL'");
            String sql2 = "INSERT INTO ben_contribution (id, benefit_type, unit_no, unit_name, period_label, pay_base, personal_pay, unit_pay, total_pay, personal_reserved, memo, created_by, created_at, updated_by, updated_at, version) "
                    + "SELECT ID, 'MEDICAL', UNIT_NO, UNIT_NAME, `TIME`, PAY_BASE, PERSONAL_PAY, UNIT_PAY, TOTAL_PAY, PERSONAL_RESERVED, DEMOAREA, CREATEUSER, CREATETIME, UPDATEUSER, UPDATETIME, COALESCE(VERSION,0) "
                    + "FROM medical b WHERE NOT EXISTS (SELECT 1 FROM ben_contribution x WHERE x.id = b.ID) LIMIT 1";
            try {
                s.executeUpdate(sql2);
                System.out.println("with backticks: OK");
            } catch (SQLException e) {
                System.out.println("with backticks: " + e.getMessage());
            }
        }
    }
}
