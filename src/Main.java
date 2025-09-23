import com.car.rental.AdminUI;
import com.car.rental.DatabaseManager;

public class Main {
    public static void main(String[] args) {
        DatabaseManager db = new DatabaseManager();
        new AdminUI();
    }
}

