/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.maven;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MavenPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import com.codename1.mojo.exec.ExecJavaMojo;
import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

/**
 *
 * @author shannah
 */
@Mojo( name = "simulator2", threadSafe = true, requiresDependencyResolution = ResolutionScope.TEST)
@Execute(phase = LifecyclePhase.COMPILE)
public class SimulatorMojo2 extends AbstractMojo  {

    @Component
    private MavenProject project;
    
    @Component
    private MavenSession session;
    
    @Component
    private BuildPluginManager pluginManager;
    
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        
        executeMojo(
            plugin(
                groupId("org.codehaus.mojo"),
                artifactId("exec-maven-plugin"),
                version("3.0.0")
            ),
            goal("java"),
            configuration(
                element(name("mainClass"), "com.codename1.impl.javase.Simulator")
                
            ),
            executionEnvironment(
                project,
                session,
                pluginManager
            )
        );
    }
    
}
