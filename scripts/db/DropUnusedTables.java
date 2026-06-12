import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** Drop unused tables/views. Usage: ./scripts/db/drop-unused-tables.sh */
public class DropUnusedTables {
    private static final String[] DROPS = {
            "DROP TABLE IF EXISTS `_deprecated_medical`",
            "DROP TABLE IF EXISTS `_deprecated_endowment`",
            "DROP TABLE IF EXISTS `_deprecated_accumulation`",
            "DROP TABLE IF EXISTS `_deprecated_unemployment`",
            "DROP TABLE IF EXISTS `_deprecated_bank_card`",
            "DROP TABLE IF EXISTS `medical`",
            "DROP TABLE IF EXISTS `endowment`",
            "DROP TABLE IF EXISTS `accumulation`",
            "DROP TABLE IF EXISTS `unemployment`",
            "DROP VIEW IF EXISTS `_archive_consume_rule_tag`",
            "DROP TABLE IF EXISTS `_archive_consume_rule_tag`",
            "DROP VIEW IF EXISTS `_archive_consume_rule`",
            "DROP TABLE IF EXISTS `_archive_consume_rule`",
            "DROP VIEW IF EXISTS `_archive_consume_category`",
            "DROP TABLE IF EXISTS `_archive_consume_category`",
            "DROP VIEW IF EXISTS `card`",
            "DROP TABLE IF EXISTS `_archive_card_legacy`",
            "DROP TABLE IF EXISTS `budget_line`",
            "DROP TABLE IF EXISTS `budget`",
            "DROP TABLE IF EXISTS `bill`",
            "DROP TABLE IF EXISTS `financial_goal`",
            "DROP TABLE IF EXISTS `account_balance_snapshot`",
            "DROP TABLE IF EXISTS `_archive_financial_account`",
            "DROP TABLE IF EXISTS `financial_account`",
            "DROP TABLE IF EXISTS `transfer_pair`",
            "DROP TABLE IF EXISTS `transaction_link`",
            "DROP TABLE IF EXISTS `django_admin_log`",
            "DROP TABLE IF EXISTS `django_session`",
            "DROP TABLE IF EXISTS `auth_user_user_permissions`",
            "DROP TABLE IF EXISTS `auth_user_groups`",
            "DROP TABLE IF EXISTS `auth_group_permissions`",
            "DROP TABLE IF EXISTS `auth_user`",
            "DROP TABLE IF EXISTS `auth_group`",
            "DROP TABLE IF EXISTS `auth_permission`",
            "DROP TABLE IF EXISTS `django_content_type`",
            "DROP TABLE IF EXISTS `django_migrations`",
            "DROP TABLE IF EXISTS `deposit_record`",
            "DROP TABLE IF EXISTS `deposit`",
            "DROP TABLE IF EXISTS `CREDIT`",
            "DROP TABLE IF EXISTS `salary`",
            "DROP TABLE IF EXISTS `consume_rule_tag`",
            "DROP TABLE IF EXISTS `consume_rule`",
            "DROP TABLE IF EXISTS `consume_category`",
    };

    public static void main(String[] args) throws Exception {
        String url = System.getenv().getOrDefault("SPRING_DATASOURCE_URL",
                "jdbc:mysql://127.0.0.1:3306/finsight?useUnicode=true&characterEncoding=UTF-8&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true");
        String user = System.getenv().getOrDefault("SPRING_DATASOURCE_USERNAME", "root");
        String pass = System.getenv().getOrDefault("SPRING_DATASOURCE_PASSWORD", "123456");

        try (Connection c = DriverManager.getConnection(url, user, pass);
             Statement s = c.createStatement()) {
            System.out.println("Dropping unused tables/views...");
            for (String sql : DROPS) {
                try {
                    s.execute(sql);
                    System.out.println("  OK " + sql);
                } catch (Exception e) {
                    System.out.println("  SKIP " + sql + " -> " + e.getMessage());
                }
            }
            System.out.println("\nRemaining tables:");
            List<String> remaining = new ArrayList<>();
            try (ResultSet rs = s.executeQuery(
                    "SELECT table_name FROM information_schema.tables "
                            + "WHERE table_schema = DATABASE() ORDER BY table_name")) {
                while (rs.next()) {
                    remaining.add(rs.getString(1));
                }
            }
            remaining.forEach(t -> System.out.println("  - " + t));
            System.out.println("\nTotal: " + remaining.size());
        }
    }
}
