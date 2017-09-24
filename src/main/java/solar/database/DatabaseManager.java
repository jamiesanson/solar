package solar.database;

import io.reactivex.*;
import org.dizitart.no2.Cursor;
import org.dizitart.no2.Document;
import org.dizitart.no2.Nitrite;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.dizitart.no2.objects.filters.ObjectFilters.*;

public class DatabaseManager {

    private static Nitrite database = Nitrite.builder()
            .filePath(new File("solar.db"))
            .openOrCreate();

    private static String TARGET_KEY = "targets";
    private static String TARGETS_ID = "target_id";
    private static String OPERATION_COLLECTION = "operation";

    // For adding temperature targets to DB
    public Completable updateTargets(List<Double> targets) {
        return Completable.fromAction(() -> {
            Document doc = Document.createDocument(TARGET_KEY, targets);
            doc.put("_id", TARGETS_ID);

            database.getCollection(OPERATION_COLLECTION).insert(doc);
        });
    }

    // For getting targets from the DB
    public Maybe<ArrayList<Double>> getTargets() {
        return Maybe.create(e -> {
            Cursor cursor = database.getCollection(OPERATION_COLLECTION).find(
                    elemMatch(TARGET_KEY, gt("$", 0))
            );

            for (Document document : cursor) {
                Object field = document.get(TARGET_KEY);
                if (field != null) {
                    //noinspection unchecked
                    e.onSuccess((ArrayList<Double>) field);
                    break;
                }
            }

            e.onComplete();
        });
    }
}
