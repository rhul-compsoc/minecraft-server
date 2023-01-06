import net.minecrell.pluginyml.bukkit.BukkitPluginDescription

plugins {
  `java`
  `java-library`
  id("io.papermc.paperweight.userdev") version "1.3.7"
  id("net.minecrell.plugin-yml.bukkit") version "0.5.2" // Generates plugin.yml
}

var pluginName = "example"
group = "com.github.rhulcompsoc"
version = "1.0.0-SNAPSHOT"
description = "Compsoc Whitelist Plugin"
var pluginMain = "com.github.rhulcompsoc.whitelist.PluginMain"
var pluginApiVersion = "1.18"

java {
  // Configure the java toolchain. This allows gradle to auto-provision JDK 17 on systems that only have JDK 8 installed for example.
  toolchain.languageVersion.set(JavaLanguageVersion.of(18))
}

repositories {
  maven("https://repo.dmulloy2.net/repository/public/")
  mavenCentral()
  /* For ProtocolLib */
}
dependencies {
  compileOnly("com.comphenix.protocol", "ProtocolLib", "4.8.0")
  paperDevBundle("1.18.2-R0.1-SNAPSHOT")
  // paperweightDevBundle("com.example.paperfork", "1.19-R0.1-SNAPSHOT")

  // You will need to manually specify the full dependency if using the groovy gradle dsl
  // (paperDevBundle and paperweightDevBundle functions do not work in groovy)
  // paperweightDevelopmentBundle("io.papermc.paper:dev-bundle:1.19-R0.1-SNAPSHOT")
  implementation("io.github.cdimascio:dotenv-java:2.3.1")
  implementation("com.google.guava:guava:31.1-jre")
  implementation("org.postgresql:postgresql:42.5.1")
  implementation("org.apache.logging.log4j:log4j-core:2.19.0")
  implementation("org.apache.logging.log4j:log4j-api:2.19.0")
  implementation("org.slf4j:slf4j-log4j12:2.0.5")
  implementation("org.apache.commons:commons-dbcp2:2.9.0")
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
