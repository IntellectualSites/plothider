import net.minecrell.pluginyml.bukkit.BukkitPluginDescription

plugins {
    id("java")
    id("java-library")
    id("net.minecrell.plugin-yml.bukkit") version "0.3.0"
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = sourceCompatibility
}

version = "4.0.0"

repositories {
    maven { url = uri("https://maven.enginehub.org/repo/") }
    maven { url = uri("https://mvn.intellectualsites.com/content/groups/public/") }
    maven { url = uri("https://jitpack.io") }
    maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots") }
    maven { url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") }
    maven { url = uri("http://repo.dmulloy2.net/nexus/repository/public/") }
    maven { url = uri("https://maven.enginehub.org/repo/") }
    maven { url = uri("https://repo.maven.apache.org/maven2") }
    maven { url = uri("https://papermc.io/repo/repository/maven-public/") }
    mavenLocal()
}

dependencies {
    compileOnlyApi("org.spigotmc:spigot-api:1.16.4-R0.1-SNAPSHOT")
    compileOnlyApi("com.destroystokyo.paper:paper-api:1.16.4-R0.1-SNAPSHOT")
    compileOnly("com.plotsquared:PlotSquared-Core:5.13.3")
    compileOnly("com.plotsquared:PlotSquared-Bukkit:5.13.3") { isTransitive = false }
    compileOnly("com.comphenix.protocol:ProtocolLib:4.6.0")
}

bukkit {
    name = "PlotHider"
    main = "com.boydti.plothider.Main"
    authors = listOf("Empire92", "dordsor21")
    apiVersion = "1.13"
    description = "Hide plots from other players"
    version = rootProject.version.toString()
    depend = listOf("ProtocolLib", "PlotSquared")
    website = "https://www.spigotmc.org/resources/20701/"
}