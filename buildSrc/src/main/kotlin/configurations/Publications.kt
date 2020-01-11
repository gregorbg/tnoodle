package configurations

import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.get

object Publications {
    fun Project.configureMavenPublication(targetArtifactId: String? = null) {
        configure<PublishingExtension> {
            publications {
                create<MavenPublication>("tnoodle") {
                    targetArtifactId?.let {
                        artifactId = it
                    }

                    from(components["java"])
                }
            }
        }
    }

    fun Project.addGitHubPackagesTarget() {
        configure<PublishingExtension> {
            repositories {
                maven {
                    name = "GitHubPackages"
                    url = uri("https://maven.pkg.github.com/thewca/tnoodle")
                    credentials {
                        username = project.findProperty("gpr.user") as? String
                        password = project.findProperty("gpr.key") as? String
                    }
                }
            }
        }
    }
}
