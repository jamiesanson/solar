package solar.gui.modeladapter;

import javafx.scene.control.ListCell;
import solar.gui.GUIRepresentable;

/**
 * Created by No3x on 01.02.2017.
 */
public class ListViewModelAdapter<T extends GUIRepresentable> extends ListCell<T> {

    @Override
    protected void updateItem(T item, boolean empty) {
        super.updateItem(item, empty);
        if (item != null) {
            setText(item.getTitle());
        } else {
            setText(null);
        }
    }

}
