import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  `java-library`
  jacoco
  `maven-publish`
  signing

  kotlin("jvm") version "1.5.31"
  id("org.jetbrains.dokka") version "1.4.30"

  id("org.cadixdev.licenser") version "0.6.1"
  id("org.jmailen.kotlinter") version "3.6.0"
}


group = "co.touchlab.fork.swiftpoet"
version = System.getenv("RELEASE_VERSION") ?: "1.5.0-SNAPSHOT"
description = "A Kotlin/Java API for generating .swift source files."

val isSnapshot = "$version".endsWith("SNAPSHOT")


//
// DEPENDENCIES
//

// Versions

val junitJupiterVersion = "5.6.2"
val hamcrestVersion = "1.3"

repositories {
  mavenCentral()
  jcenter()
}

dependencies {

  //
  // LANGUAGES
  //

  // kotlin
  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
  implementation("org.jetbrains.kotlin:kotlin-reflect")

  //
  // TESTING
  //

  // junit
  testImplementation("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
  testImplementation("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")
  testImplementation("org.hamcrest:hamcrest-all:$hamcrestVersion")

}


//
// COMPILE
//

java {
  sourceCompatibility = JavaVersion.VERSION_1_8
  targetCompatibility = JavaVersion.VERSION_1_8

  withSourcesJar()
  withJavadocJar()
}

tasks {
  withType<KotlinCompile> {
    kotlinOptions {
      jvmTarget = "1.8"
    }
  }
}


//
// TEST
//

jacoco {
  toolVersion = "0.8.7"
}

tasks {
  test {
    useJUnitPlatform()

    finalizedBy(jacocoTestReport)
    jacoco {}
  }

  jacocoTestReport {
    dependsOn(test)
  }
}


//
// DOCS
//

tasks {
  dokkaHtml {
    outputDirectory.set(file("$buildDir/javadoc/${project.version}"))
  }

  javadoc {
    dependsOn(dokkaHtml)
  }
}


//
// CHECKS
//

kotlinter {
  indentSize = 2
}

license {
  header.set(resources.text.fromFile("HEADER.txt"))
  include("**/*.kt")
}


//
// PUBLISHING
//

publishing {

  publications {

    create<MavenPublication>("mavenJava") {
      from(components["java"])

      pom {

        name.set("Swift Poet")
        description.set("SwiftPoet is a Kotlin and Java API for generating .swift source files.")
        url.set("https://github.com/outfoxx/swiftpoet")

        organization {
          name.set("Outfox, Inc.")
          url.set("https://outfoxx.io")
        }

        issueManagement {
          system.set("GitHub")
          url.set("https://github.com/outfoxx/swiftpoet/issues")
        }

        licenses {
          license {
            name.set("Apache License 2.0")
            url.set("https://raw.githubusercontent.com/outfoxx/swiftpoet/master/LICENSE.txt")
            distribution.set("repo")
          }
        }

        scm {
          url.set("https://github.com/outfoxx/swiftpoet")
          connection.set("scm:https://github.com/outfoxx/swiftpoet.git")
          developerConnection.set("scm:git@github.com:outfoxx/swiftpoet.git")
        }

        developers {
          developer {
            id.set("kdubb")
            name.set("Kevin Wooten")
            email.set("kevin@outfoxx.io")
          }
        }

      }
    }

  }

  repositories {

    maven {
      val snapshotUrl = "https://oss.sonatype.org/content/repositories/snapshots/"
      val releaseUrl = "https://oss.sonatype.org/service/local/staging/deploy/maven2/"
      url = uri(if (isSnapshot) snapshotUrl else releaseUrl)

      credentials {
        username = project.findProperty("ossrhUsername")?.toString()
        password = project.findProperty("ossrhPassword")?.toString()
      }
    }

      maven {
          name = "aws"
          val snapshotUrl = "s3://touchlab-repo/snapshot"
          val releaseUrl = "s3://touchlab-repo/release"
          url = uri(if (isSnapshot) snapshotUrl else releaseUrl)
          credentials(AwsCredentials::class) {
              accessKey = System.getenv("AWS_TOUCHLAB_DEPLOY_ACCESS")
              secretKey = System.getenv("AWS_TOUCHLAB_DEPLOY_SECRET")
          }
      }

  }

}


signing {
  gradle.taskGraph.whenReady {
    isRequired = hasTask("publishMavenJavaPublicationToMavenRepository")
  }
  sign(publishing.publications["mavenJava"])
}
