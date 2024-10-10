package com.dev.gear.generator;

import com.dev.gear.util.ClassChooserUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import groovyjarjarantlr4.v4.runtime.misc.NotNull;
import groovyjarjarantlr4.v4.runtime.misc.Nullable;
import kotlin.Unit;
import kotlin.coroutines.Continuation;

public class PluginInitializer implements ProjectActivity {
    @Nullable
    @Override
    public Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        ClassChooserUtil.initialize(project);
        return Unit.INSTANCE;
    }
}

        