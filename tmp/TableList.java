import java.sql.*;
public class TableList {
  public static void main(String[] a) throws Exception {
    String[] check = {"transfer_pair","transaction_link","budget","budget_line","bill","financial_goal","financial_account","account_balance_snapshot","medical","endowment","accumulation","unemployment","consume_category","consume_rule"};
    try (Connection c = DriverManager.getConnection("jdbc:mysql://127.0.0.1:3306/finsight?useSSL=false&allowPublicKeyRetrieval=true","root","123456");
         Statement s = c.createStatement()) {
      for (String t : check) {
        try (ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM `" + t + "`")) {
          rs.next();
          System.out.println(t + " rows=" + rs.getLong(1));
        } catch (SQLException e) {
          System.out.println(t + " MISSING");
        }
      }
    }
  }
}
