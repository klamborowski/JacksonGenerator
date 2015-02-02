package pl.klamborowski.jacksongenerator;

import com.intellij.ide.highlighter.JavaClassFileType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataConstants;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.InputValidator;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.text.StringTokenizer;
import org.apache.commons.lang.WordUtils;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by artur on 2015-02-02.
 */
public class JacksonGenerator extends AnAction {
    public void actionPerformed(AnActionEvent event) {

        showDialogs(event);
    }

    private void showDialogs(AnActionEvent event) {

        Project project = event.getData(PlatformDataKeys.PROJECT);
        if (project == null) {
            return;
        }
        String mainJacksonClassName = Messages.showInputDialog(project, "Type main class name", "Jackson Generator - Class Name",
                Messages.getInformationIcon(), "", new InputValidator() {
                    @Override
                    public boolean checkInput(String s) {
                        return JavaClassNameChecker.isJavaClassName(s) && s.length() > 0;
                    }

                    @Override
                    public boolean canClose(String s) {
                        return checkInput(s);
                    }
                });

        if (mainJacksonClassName == null || mainJacksonClassName.length() == 0) {
            return;
        }

        String jsonString = Messages.showInputDialog(project, "Paste your Json string here", "Jackson Generator - Json String",
                Messages.getInformationIcon(), "", new InputValidator() {
                    @Override
                    public boolean checkInput(String s) {
                        return s.length() > 0;
                    }

                    @Override
                    public boolean canClose(String s) {
                        return checkInput(s);
                    }
                });


        if (jsonString == null || jsonString.length() == 0) {
            return;
        }

        parseJson(event, project, mainJacksonClassName, jsonString);
    }


    private void parseJson(AnActionEvent event, Project project, String mainJacksonClassName, String jsonString) {
        VirtualFile actionParnetDirectory = (VirtualFile) event.getDataContext().getData(DataConstants.VIRTUAL_FILE);
        if (actionParnetDirectory != null) {
            if (!project.isInitialized()) {
                return;
            }
            final PsiDirectory directory = PsiManager.getInstance(project).findDirectory(actionParnetDirectory);
            PsiPackage pkg = findTargetPackage(directory);

            try {
                generateFiles(project, jsonString, mainJacksonClassName, pkg.getQualifiedName(), directory);
            } catch (JSONException e) {
                e.printStackTrace();
                Messages.showErrorDialog(project, e.getMessage(), "Error!");
            }

        }

    }

    private void generateFiles(Project project, String jsonString, String jacksonClassName, String packageName, final PsiDirectory directory) {
        StringBuilder body = new StringBuilder("package ")
                .append(packageName)
                .append(";\n")
                .append("\n");
        JSONObject rootJO = new JSONObject(jsonString);


        StringBuilder imports = new StringBuilder();
        imports.append("import com.fasterxml.jackson.annotation.JsonProperty;\n\n");

        StringBuilder fields = new StringBuilder();
        for (String key : rootJO.keySet()) {
            Class type = rootJO.get(key).getClass();
            if (JSONObject.class.equals(type)) {
                generateJsonObjectFieldAndCreateNewClass(project, packageName, directory, rootJO, fields, key);
            } else {
                if (JSONArray.class.equals(type)) {


                    generateJsonArrayFieldAndCreateNewItemClassIfNeeded(project, packageName, directory, rootJO, imports, fields, key);

                } else {
                    generateField(fields, key, type);
                }
            }
        }


        body
                .append(imports.toString())
                .append("\n")
                .append("\n")
                .append("public class ").append(jacksonClassName)
                .append("{\n")
                .append(fields.toString())
                .append("}");


        final PsiFile file = PsiFileFactory.getInstance(project).createFileFromText(
                jacksonClassName.endsWith(".java") ? jacksonClassName : jacksonClassName + ".java",
                JavaClassFileType.INSTANCE, body.toString());


        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            @Override
            public void run() {
                directory.add(file);

            }
        });
    }

    private void generateJsonArrayFieldAndCreateNewItemClassIfNeeded(Project project, String packageName, PsiDirectory directory, JSONObject rootJO, StringBuilder imports, StringBuilder fields, String key) {
        imports.append("import import java.util.List;\n");


        JSONArray array = rootJO.getJSONArray(key);
        Object arrayItem = null;
        if (array.length() > 0) {
            arrayItem = array.get(0);
        }

        StringBuilder fieldTypeString = new StringBuilder("List");

        if (arrayItem != null) {
            if (JSONObject.class.equals(arrayItem.getClass())) {
                String internalClassName = createClassName(key) + "Item";
                generateFiles(project, arrayItem.toString(), internalClassName, packageName, directory);
            } else {
                String[] classArray = arrayItem.getClass().getName().split("\\.");
                fieldTypeString.append("<")
                        .append(classArray[classArray.length - 1])
                        .append(">");
            }
        }


        fields
                .append("\t@JsonProperty(\"")
                .append(key)
                .append("\")")
                .append("\n")

                .append("\tprivate ")
                .append(fieldTypeString.toString())
                .append(" ")
                .append(createFieldName(key))
                .append(";")
                .append("\n");
    }

    private void generateField(StringBuilder fields, String key, Class type) {
        String[] classArray = type.getName().split("\\.");
        fields
                .append("\t@JsonProperty(\"")
                .append(key)
                .append("\")")
                .append("\n")

                .append("\tprivate ")
                .append(classArray[classArray.length - 1])
                .append(" ")
                .append(createFieldName(key))
                .append(";")
                .append("\n");
    }

    private void generateJsonObjectFieldAndCreateNewClass(Project project, String packageName, PsiDirectory directory, JSONObject rootJO, StringBuilder fields, String key) {
        String internalClassName = createClassName(key);
        fields
                .append("\t@JsonProperty(\"")
                .append(key)
                .append("\")")
                .append("\n")

                .append("\tprivate ")
                .append(internalClassName)
                .append(" ")
                .append(WordUtils.uncapitalize(internalClassName))
                .append(";")
                .append("\n");
        generateFiles(project, rootJO.get(key).toString(), internalClassName, packageName, directory);
    }


    private String createClassName(String key) {
        String[] splittedKey = key.split("_");
        StringBuilder name = new StringBuilder();
        for (String str : splittedKey) {
            name.append(WordUtils.capitalize(str));
        }

        return name.toString();
    }


    private String createFieldName(String key) {
        return WordUtils.uncapitalize(createClassName(key));
    }

    @Nullable
    public static PsiPackage findTargetPackage(PsiDirectory directory) {
        PsiPackage aPackage = null;

        if (directory != null) {
            aPackage = JavaDirectoryService.getInstance().getPackage(directory);
        }
        if (aPackage == null) return null;

        return aPackage;
    }

    public static PsiDirectory createDirectory(PsiDirectory parent, String name)
            throws IncorrectOperationException {
        PsiDirectory result = null;
        for (PsiDirectory dir : parent.getSubdirectories()) {
            if (dir.getName().equalsIgnoreCase(name)) {
                result = dir;
                break;
            }
        }
        if (null == result) {
            result = parent.createSubdirectory(name);
        }
        return result;
    } // createDirectory()

    public static PsiDirectory createPackage(PsiDirectory sourceDir, String qualifiedPackage)
            throws IncorrectOperationException {
        PsiDirectory parent = sourceDir;
        StringTokenizer token = new StringTokenizer(qualifiedPackage, ".");
        while (token.hasMoreTokens()) {
            String dirName = token.nextToken();
            parent = createDirectory(parent, dirName);
        }
        return parent;
    } // createPackage()
}
