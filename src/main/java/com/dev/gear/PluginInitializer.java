package com.dev.gear;

import com.dev.gear.util.ClassChooserUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import org.jetbrains.annotations.NotNull;

public class PluginInitializer implements StartupActivity {
    @Override
    public void runActivity(@NotNull Project project) {
        ClassChooserUtil.initialize(project);
    }
}