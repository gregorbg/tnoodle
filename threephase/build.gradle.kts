import configurations.Languages.configureJava
import configurations.Publications.configureMavenPublication
import configurations.Publications.addGitHubPackagesTarget

description = "A copy of Chen Shuang's 4x4 scrambler."

plugins {
    `java-library`
    `maven-publish`
}

configureJava()
configureMavenPublication("scrambler-threephase")

addGitHubPackagesTarget()

dependencies {
    implementation(project(":min2phase"))
}
