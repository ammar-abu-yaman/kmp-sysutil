plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.vanniktech.mavenPublish)
}

group = "com.ammarymn.kmp.sysinfo"
version = "0.0.1"

kotlin {
    jvm()
    linuxX64()
    mingwX64()

    sourceSets {
        commonMain.dependencies {
            //put your multiplatform dependencies here
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

mavenPublishing {
    publishToMavenCentral()

    signAllPublications()

    coordinates(group.toString(), "library", version.toString())

    pom {
        name = "KMP SysInfo"
        description = "A low-level system information library for Kotlin Multiplatform."
        inceptionYear = "2026"
        url = "https://ammarymn.com"
        licenses {
            license {
                name = "MIT"
                url = "https://opensource.org/licenses/MIT"
                distribution = "repo"
            }
        }
        developers {
            developer {
                id = "ammarymn"
                name = "Ammar Abu Yaman"
                email = "ammar.abu.yaman@gmail.com"
                url = "https://ammarymn.com"
            }
        }
        scm {
            url = "https://github.com/ammar-abu-yaman/kmp-sysinfo"
            connection = "scm:git:git://github.com/ammar-abu-yaman/kmp-sysinfo.git"
            developerConnection = "scm:git:ssh://git@github.com/ammar-abu-yaman/kmp-sysinfo.git"
        }
    }
}
