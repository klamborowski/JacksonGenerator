package pl.klamborowski.jacksongenerator;

import com.intellij.openapi.ui.InputValidator;

/**
 * Created by artur on 2015-02-02.
 */
public class JavaClassNameInputValidator implements InputValidator {
    @Override
    public boolean checkInput(String s) {
        return JavaClassNameChecker.isJavaClassName(s) && s.length() > 0;
    }

    @Override
    public boolean canClose(String s) {
        return checkInput(s);
    }
}
