import net.minecrell.pluginyml.bukkit.BukkitPluginDescription

plugins {
  `java`
  `java-library`
  id("io.papermc.paperweight.userdev") version "1.4.1"
  id("net.minecrell.plugin-yml.bukkit") version "0.5.2" // Generates plugin.yml
  id("com.github.johnrengelman.shadow") version "7.0.0"
}

var pluginName = "rhulcompsoc-whitelist"
group = "com.github.rhulcompsoc"
version = "1.0.0-SNAPSHOT"
description = "Compsoc Whitelist Plugin"
var pluginMain = "com.github.hulcompsoc.whitelist.PluginMain"
var pluginApiVersion = "1.18"

java {
  // Configure the java toolchain. This allows gradle to auto-provision JDK 17 on systems that only have JDK 8 installed for example.
  toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

repositories {
  maven("https://repo.dmulloy2.net/repository/public/")
  mavenCentral()
  /* For ProtocolLib */
}
dependencies {
  compileOnly("com.comphenix.protocol", "ProtocolLib", "4.8.0")
  implementation("io.github.cdimascio:dotenv-java:2.3.1")
  shadow("io.github.cdimascio:dotenv-java:2.3.1")

  implementation("com.google.guava:guava:31.1-jre")
  shadow("com.google.guava:guava:31.1-jre")

  implementation("org.postgresql:postgresql:42.5.1")
  shadow("org.postgresql:postgresql:42.5.1")

  implementation("org.apache.logging.log4j:log4j-core:2.19.0")
  shadow("org.apache.logging.log4j:log4j-core:2.19.0")

  implementation("org.apache.logging.log4j:log4j-api:2.19.0")
  shadow("org.apache.logging.log4j:log4j-api:2.19.0")

  implementation("org.slf4j:slf4j-log4j12:2.0.5")
  shadow("org.slf4j:slf4j-log4j12:2.0.5")

  implementation("org.apache.commons:commons-dbcp2:2.9.0")
  shadow("org.apache.commons:commons-dbcp2:2.9.0")

  paperDevBundle("1.19.3-R0.1-SNAPSHOT")
}

tasks {
  // Configure reobfJar to run when invoking the build task
  assemble {
    dependsOn(reobfJar)
  }

  compileJava {
    options.encoding = Charsets.UTF_8.name() // We want UTF-8 for everything

    // Set the release flag. This configures what version bytecode the compiler will emit, as well as what JDK APIs are usable.
    // See https://openjdk.java.net/jeps/247 for more information.
    options.release.set(17)
  }
  javadoc {
    options.encoding = Charsets.UTF_8.name() // We want UTF-8 for everything
  }
  processResources {
    filteringCharset = Charsets.UTF_8.name() // We want UTF-8 for everything
  }

  reobfJar {
    // This is an example of how you might change the output location for reobfJar. It's recommended not to do this
    // for a variety of reasons, however it's asked frequently enough that an example of how to do it is included here.
    outputJar.set(layout.buildDirectory.file("libs/${pluginName}-${project.version}.jar"))
  }
}

tasks.jar {
  archiveBaseName.set("${pluginName}");
}

// Configure plugin.yml generation
bukkit {
  name = "${pluginName}"
  load = BukkitPluginDescription.PluginLoadOrder.POSTWORLD /* Needs to be post world, because we add NPCs to the world on load */
  main = "${pluginMain}"
  apiVersion = "${pluginApiVersion}"
  authors = listOf("Danny Piper (djpiper28)")
}
