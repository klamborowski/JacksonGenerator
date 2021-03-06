package pl.klamborowski.jacksongenerator;

import com.intellij.codeInsight.actions.RearrangeCodeProcessor;
import com.intellij.codeInsight.actions.ReformatCodeProcessor;
import com.intellij.ide.highlighter.JavaClassFileType;
import com.intellij.openapi.actionSystem.DataConstants;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
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

import java.text.DateFormat;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by artur on 2015-02-02.
 */
public class JacksonGenerator {


    private boolean addDatabaseFields = true;

    public JacksonGenerator(boolean addDatabaseFields) {
        this.addDatabaseFields = addDatabaseFields;
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

    public void parseJson(DataContext dataContext, Project project, String mainJacksonClassName, String jsonString) {
        VirtualFile actionParnetDirectory = (VirtualFile) dataContext.getData(DataConstants.VIRTUAL_FILE);
        if (actionParnetDirectory != null) {
            if (!project.isInitialized()) {
                return;
            }
            if (!actionParnetDirectory.isDirectory()) {
                actionParnetDirectory = actionParnetDirectory.getParent();
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

    private void generateFiles(final Project project, String jsonString, String jacksonClassName, String packageName, final PsiDirectory directory) {
        Set<String> columNames = new HashSet<String>();

        StringBuilder body = new StringBuilder();

        if (packageName.length() > 0) {
            body.append("package ")
                    .append(packageName)
                    .append(";\n")
                    .append("\n");
        }

        body.append("/**\n" +
                " * Created by JacksonGenerator on ")
                .append(DateFormat.getDateInstance(DateFormat.SHORT).format(Calendar.getInstance().getTime()))
                .append(".\n" +
                        " */\n" +
                        "\n");
        JSONObject rootJO = new JSONObject(jsonString);


        Set<String> imports = new HashSet<String>();

        if (addDatabaseFields) {
            imports.add("import com.j256.ormlite.field.DatabaseField;");
        }
        imports.add("import com.fasterxml.jackson.annotation.JsonProperty;\n\n");

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
            columNames.add(key);
        }


        for (String anImport : imports) {
            body.append(anImport);
        }
        body
                .append("\n")
                .append("\n")
                .append("public class ").append(jacksonClassName)
                .append("{\n")
                .append(fields.toString());
              if(addDatabaseFields){
                  body
                          .append("\n")
                          .append(generateColumnsInterface(columNames))
                          .append("\n");
              }
                  body
                .append("}");

        final PsiFile file = PsiFileFactory.getInstance(project).createFileFromText(
                jacksonClassName.endsWith(".java") ? jacksonClassName : jacksonClassName + ".java",
                JavaClassFileType.INSTANCE, body.toString());

        WriteCommandAction.runWriteCommandAction(project, () -> {
            int replaceIfExist = -1;
            if (directory.findFile(file.getName()) != null) {
                //1 - NO, 0 - YES
                replaceIfExist = Messages.showYesNoDialog("File " + file.getName() + " already exists! \nDo you want to delete it and replace by the generated file?", "File " + file.getName() + " already exists!", Messages.getWarningIcon());
            }
            PsiElement resultElement = null;
            switch (replaceIfExist) {
                case -1:
                    resultElement = directory.add(file);
                    break;
                case 0:
                    directory.findFile(file.getName()).delete();
                    resultElement = directory.add(file);
                    break;
                default:
                    break;
            }

            if (resultElement != null && resultElement instanceof PsiJavaFile) {

                RearrangeCodeProcessor rearrangeCodeProcessor = new RearrangeCodeProcessor(project, new PsiFile[]{(PsiJavaFile) resultElement},
                        RearrangeCodeProcessor.COMMAND_NAME, null);
                rearrangeCodeProcessor.run();
                ReformatCodeProcessor reformatCodeProcessor = new ReformatCodeProcessor(project, (PsiJavaFile) resultElement, null, false);
                reformatCodeProcessor.run();
            }


        });
    }

    private void generateJsonArrayFieldAndCreateNewItemClassIfNeeded(Project project, String packageName, PsiDirectory directory,
                                                                     JSONObject rootJO, Set<String> imports, StringBuilder fields, String key) {
        imports.add("import java.util.List;\n");


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
                fieldTypeString.append("<")
                        .append(internalClassName)
                        .append(">");
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
                .append("\n");

        if (addDatabaseFields) {
            fields.append("\t@DatabaseField(columnName = Column.")
                    .append(key.toUpperCase())
                    .append(")")
                    .append("\n");
        }


        fields
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
                .append("\n");

        if (addDatabaseFields) {
            fields.append("\t@DatabaseField(columnName = Column.")
                    .append(key.toUpperCase())
                    .append(")")
                    .append("\n");
        }

        fields.append("\tprivate ")
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
                .append("\n");

        if (addDatabaseFields) {
            fields.append("\t@DatabaseField(columnName = Column.")
                    .append(key.toUpperCase())
                    .append(")")
                    .append("\n");
        }

        fields.append("\tprivate ")
                .append(internalClassName)
                .append(" ")
                .append(WordUtils.uncapitalize(internalClassName))
                .append(";")
                .append("\n");
        generateFiles(project, rootJO.get(key).toString(), internalClassName, packageName, directory);
    }

    private String createClassName(String key) {
        String[] splittedKey = key.replaceAll("\\W", "_").split("_");
        StringBuilder name = new StringBuilder();
        for (String str : splittedKey) {
            if (str.length() > 0) {
                name.append(WordUtils.capitalize(str));
            }
        }
        return name.toString();
    }

    private String createFieldName(String key) {
        return WordUtils.uncapitalize(createClassName(key));
    }

    private String generateColumnsInterface(Set<String> columNames) {
        StringBuilder interfaceBuilder = new StringBuilder();
        interfaceBuilder
                .append("\tpublic interface Column {")
                .append("\n");
        for (String columName : columNames) {
            interfaceBuilder
                    .append("\t\tString ")
                    .append(columName.toUpperCase())
                    .append(" = \"")
                    .append(columName.toUpperCase())
                    .append("\";")
                    .append("\n");
        }
        interfaceBuilder
                .append("\n\t}");
        return interfaceBuilder.toString();
    }
}
