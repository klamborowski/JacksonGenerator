package pl.klamborowski.jacksongenerator;

/**
 * Created by Artur on 2016-12-30.
 */
public interface OnDialogCloseListener {
    void onDialogClose(String mainClassName, String jsonString, boolean generateOrmAnnotation);
}
