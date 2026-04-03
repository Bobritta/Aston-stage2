import controller.CommandLineUserMenu;
import util.LiquibaseUtil;

public class Main {
    public static void main(String[] args) {
        LiquibaseUtil.runMigrations();
        CommandLineUserMenu menu = new CommandLineUserMenu();
        menu.start();
    }
}
