package solar;

import com.google.inject.Guice;
import com.google.inject.Injector;
import solar.gui.GUI;

public class Main {

    public static void main(String[] args) {
        Injector injector = Guice.createInjector();
        GUI gui = injector.getInstance(GUI.class);

        try {
            gui.run(args);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
