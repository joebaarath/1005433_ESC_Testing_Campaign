import java.util.ArrayList;

public class MismatchesInfo {
    public ArrayList<String> columns;
    public int rand_index;
    public String[] row1;
    public String[] row2;

    MismatchesInfo(
        ArrayList<String> columns, int rand_index,
        String[] row1, String[] row2
    ) {
        this.columns = columns;
        this.rand_index = rand_index;
        this.row1 = row1;
        this.row2 = row2;
    }
}
