import java.util.ArrayList;

public class Row {
    private final String[] columns;

    Row(String[] columns) {
        this.columns = columns;
    }

    public int get_num_columns() {
        return this.columns.length;
    }
}
