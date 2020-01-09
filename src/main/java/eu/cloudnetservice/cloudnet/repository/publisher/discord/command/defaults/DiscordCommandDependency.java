package eu.cloudnetservice.cloudnet.repository.publisher.discord.command.defaults;

import eu.cloudnetservice.cloudnet.repository.publisher.discord.command.DiscordCommand;
import eu.cloudnetservice.cloudnet.repository.publisher.discord.command.DiscordPermissionState;
import eu.cloudnetservice.cloudnet.repository.util.StringUtils;
import eu.cloudnetservice.cloudnet.repository.version.CloudNetVersion;
import eu.cloudnetservice.cloudnet.repository.version.CloudNetVersionFile;
import eu.cloudnetservice.cloudnet.repository.version.MavenVersionInfo;
import eu.cloudnetservice.cloudnet.repository.version.VersionEnvironment;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

public class DiscordCommandDependency extends DiscordCommand {

    private String dependencyFormat;
    private String repositoryFormat;

    public DiscordCommandDependency(String dependencyFormatClassPath, String repositoryFormatClassPath, String... names) {
        super(names, new DiscordPermissionState[]{DiscordPermissionState.EVERYONE});

        this.dependencyFormat = StringUtils.readStringFromClassPath(this.getClass().getClassLoader(), dependencyFormatClassPath);
        this.repositoryFormat = StringUtils.readStringFromClassPath(this.getClass().getClassLoader(), repositoryFormatClassPath);
    }


    @Override
    public void execute(Member sender, MessageChannel channel, Message message, String[] args) {
        CloudNetVersion version = super.getServer().getCurrentLatestVersion();

        if (version == null) {
            channel.sendMessage("There is currently no version available!").queue();
            return;
        }


        Collection<MavenVersionInfo> versionInfos = Arrays.stream(version.getFiles())
                .map(CloudNetVersionFile::getVersionInfo)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (versionInfos.isEmpty()) {
            channel.sendMessage("No dependency for that version found!").queue();
            return;
        }

        if (args.length == 1) {
            VersionEnvironment environment;
            try {
                environment = VersionEnvironment.valueOf(args[0].toUpperCase());
            } catch (Exception ignored) {
                channel.sendMessage("That environment doesn't exist!").queue();
                return;
            }

            Collection<String> supportedDependencies = version.getVersionFileMappings().getSupportedDependencies(environment);

            MavenVersionInfo firstVersionInfo = versionInfos.iterator().next();

            EmbedBuilder builder = new EmbedBuilder();

            builder.setTitle("**Dependencies that can be used on " + environment + " of Version " + version.getName() + "**");

            builder.setDescription("Repository:\n" +
                    this.repositoryFormat.replace("%url%", firstVersionInfo.getRepositoryUrl()));

            for (MavenVersionInfo versionInfo : versionInfos) {
                String artifact = versionInfo.getArtifactId();

                if (!supportedDependencies.contains(artifact)) {
                    continue;
                }

                builder.addField(artifact,
                        this.dependencyFormat
                                .replace("%groupId%", versionInfo.getGroupId())
                                .replace("%artifactId%", artifact)
                                .replace("%version%", version.getName()),
                        true
                );
            }

            channel.sendMessage(builder.build()).queue();

            return;
        }

        EmbedBuilder builder = new EmbedBuilder();

        builder.setTitle("**Dependencies for CloudNet " + version.getName() + "**");

        builder.setDescription("Use " + super.getCommandMap().getCommandPrefix() + super.getNames()[0] + " <" + Arrays.toString(VersionEnvironment.values()) + "> " +
                "to get all dependencies that can be used for a specific environment"
        );

        for (MavenVersionInfo versionInfo : versionInfos) {
            builder.addField("", versionInfo.getArtifactId(), true);
        }

        channel.sendMessage(builder.build()).queue();

        /*EmbedBuilder builder = new EmbedBuilder();

        builder.setTitle("**Maven Dependencies for Version " + version.getName() + "**");
        builder.setDescription(
                "Basic dependency:\n" +
                        this.dependencyFormat
                                .replace("%groupId%", generalGroup != null ? generalGroup : "cloudnet-group")
                                .replace("%artifactId%", "cloudnet-artifact")
                                .replace("%version%", version.getName())
                        + "\nRepository:\n" +
                        this.repositoryFormat.replace("%url%", firstVersionInfo.getRepositoryUrl())
        );

        builder.addField("**You have to replace:**",
                generalGroup != null ?
                        "cloudnet-artifact" :
                        "cloudnet-group and cloudnet-artifact",
                false
        );

        for (MavenVersionInfo versionInfo : versionInfos) {
            String artifact = versionInfo.getArtifactId();
            String artifactTarget = super.getServer().getConfiguration().getVersionFileMappings().getVersionTarget(artifact);
            if (artifactTarget != null) {
                artifact += " | " + artifactTarget;
            }
            if (generalGroup != null) {
                builder.addField("", artifact, true);
            } else {
                builder.addField(versionInfo.getGroupId(), artifact, true);
            }
        }

        channel.sendMessage(builder.build()).queue();*/
    }
}
