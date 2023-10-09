plugins {
    `java-library`
    `maven-publish`
    signing
}

java {
    withJavadocJar()
    withSourcesJar()
}

group = "xyz.rtfpessoa"
version = if (findProperty("version") == "unspecified") "0.1.0-SNAPSHOT" else version

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testImplementation("org.assertj:assertj-core:3.24.2")
    implementation("org.jetbrains:annotations:24.0.1")
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = "smart-version"
            from(components["java"])
            versionMapping {
                usage("java-api") {
                    fromResolutionOf("runtimeClasspath")
                }
                usage("java-runtime") {
                    fromResolutionResult()
                }
            }
            pom {
                name = "smart-version"
                description = "Version comparison made easy (e.g. SemVer, Maven)"
                url = "https://github.com/rtfpessoa/smart-version"
                properties = mapOf()
                licenses {
                    license {
                        name = "The Apache License, Version 2.0"
                        url = "http://www.apache.org/licenses/LICENSE-2.0.txt"
                    }
                }
                developers {
                    developer {
                        id = "rtfpessoa"
                        name = "Rodrigo Fernandes"
                        email = "rtfrodrigo@gmail.com"
                    }
                }
                scm {
                    connection = "scm:git:git://github.com/rtfpessoa/smart-version.git"
                    developerConnection = "scm:git:ssh://github.com/rtfpessoa/smart-version.git"
                    url = "https://github.com/rtfpessoa/smart-version"
                }
            }
        }
    }
    repositories {
//        maven {
//            name = "OSSRH"
//            credentials {
//                username = System.getenv("MAVEN_USERNAME")
//                password = System.getenv("MAVEN_PASSWORD")
//            }
//            val releasesRepoUrl = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
//            val snapshotsRepoUrl = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
//            url = if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl
//        }
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/rtfpessoa/smart-version")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

//signing {
//    sign(publishing.publications["maven"])
//}

tasks.javadoc {
    if (JavaVersion.current().isJava9Compatible) {
        (options as StandardJavadocDocletOptions).addBooleanOption("html5", true)
    }
}
