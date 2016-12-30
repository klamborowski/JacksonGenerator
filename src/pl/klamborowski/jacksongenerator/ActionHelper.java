package pl.klamborowski.jacksongenerator;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;

/**
 * Created by Artur on 2016-12-30.
 */
public class ActionHelper extends AnAction implements OnDialogCloseListener {

    private DataContext dataContext;
    private Project project;

    @Override
    public void actionPerformed(AnActionEvent event) {
        project = event.getData(PlatformDataKeys.PROJECT);
        if (project == null) {
            return;
        }
        dataContext = event.getDataContext();
        GenerateDialog.show(this);
//        GenerateDialog.main(null);
    }


    @Override
    public void onDialogClose(String mainClassName, String jsonString, boolean generateOrmAnnotation) {
        JacksonGenerator jacksonGenerator = new JacksonGenerator(generateOrmAnnotation);
        jacksonGenerator.parseJson(dataContext, project, mainClassName, jsonString);
    }
}
