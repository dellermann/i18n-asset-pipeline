package i18n

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project

class MessageResearchPlugin implements Plugin<Project>{
    void apply(Project target) {
        target.task('getRuntimeClasspath') {
            doLast {
                StringBuilder builder = new StringBuilder()
                target.configurations.runtime.files.each { f->
                    builder << f.toString()+','
                }
                System.setProperty("com.i18n-asset-pipeline.pluginRuntimePath", builder.toString())
            }
        }
        target.afterEvaluate {
            def assetTask = target.getTasksByName('assetCompile',true)
            if (!assetTask) {
                throw new GradleException('The task assetCompile is not yet defined. ' +
                        'Please check that this plugin is applied after the other plugin that defines assetCompile' +
                        ' or that other plugins does not destroy the task')
            }
            assetTask.each {
                it.dependsOn << 'getRuntimeClasspath'
            }
        }
    }
}
