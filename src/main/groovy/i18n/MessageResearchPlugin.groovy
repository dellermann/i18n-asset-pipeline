package i18n
import org.gradle.api.Project
import org.gradle.api.Plugin


class MessageResearchPlugin implements Plugin<Project>{
    void apply(Project target) {
        target.task('getRuntimeClasspath') {
            doLast {
                StringBuilder builder = new StringBuilder()
                target.configurations.runtime.files.each { f->
                    builder << f.toString()+','
                }
                new File('classpath_runtime.txt').withWriter('UTF-8') { w ->
                    w.write(builder.toString())
                }
                println('Runtime classpath executed : '+builder.toString())
                System.setProperty("com.i18n-asset-pipeline.pluginRuntimePath", builder.toString())
            }
        }
        target.afterEvaluate {
            def assetTask = target.getTasksByName('assetCompile',true)
            assetTask.each {
                it.dependsOn << 'getRuntimeClasspath'
            }
            def bootrunTask = target.getTasksByName('bootRun',true)
            bootrunTask.each {
                it.dependsOn << 'getRuntimeClasspath'
            }
        }
    }
}
