package com.github.winplay02.gitcraft.pipeline;

import com.github.winplay02.gitcraft.GitCraft;
import com.github.winplay02.gitcraft.MinecraftVersionGraph;
import com.github.winplay02.gitcraft.mappings.MappingFlavour;
import com.github.winplay02.gitcraft.types.OrderedVersion;
import com.github.winplay02.gitcraft.util.GitCraftPaths;
import com.github.winplay02.gitcraft.util.RepoWrapper;

import java.nio.file.Files;
import java.nio.file.Path;

public class PatchOptiFineStep extends Step {
	@Override
	public String getName() {
		return "Patch client.jar With OptiFine";
	}

	@Override
	public StepResult run(PipelineCache pipelineCache, OrderedVersion mcVersion, MappingFlavour mappingFlavour, MinecraftVersionGraph versionGraph, RepoWrapper repo) throws Exception {
		if (!mcVersion.hasClientCode() || GitCraft.config.optiFineVersion == null) {
			return StepResult.NOT_RUN;
		}

		Path clientJarPath = mcVersion.clientJar().resolve(GitCraftPaths.MC_VERSION_STORE.resolve(mcVersion.launcherFriendlyVersionName()));
		Path optifineJarPath = GitCraft.config.optiFineVersion.resolve(GitCraftPaths.MC_VERSION_STORE.resolve(mcVersion.launcherFriendlyVersionName()));
		if (!Files.exists(clientJarPath)) {
			return StepResult.FAILED;
		}
		if (!Files.exists(optifineJarPath)) {
			return StepResult.FAILED;
		}

		Path clientPatchedJar = GitCraftPaths.MC_VERSION_STORE.resolve(mcVersion.launcherFriendlyVersionName()).resolve("client-OptiFine_" + GitCraft.config.optiFineVersion.version() + ".jar");
		if (Files.exists(clientPatchedJar)) {
			mcVersion.setPatchedClientJar(clientPatchedJar);
			return StepResult.UP_TO_DATE;
		}

		// java -cp OptiFine.jar optifine.Patcher client.jar client-patched.jar
		ProcessBuilder pb = new ProcessBuilder("java", "-cp", optifineJarPath.toString(), "optifine.Patcher", clientJarPath.toString(), optifineJarPath.toString(), clientPatchedJar.toString());
		pb.inheritIO();
		Process p = pb.start();
		int exitCode = p.waitFor();
		if (exitCode != 0) {
			return StepResult.FAILED;
		}

		mcVersion.setPatchedClientJar(clientPatchedJar);
		return StepResult.SUCCESS;
	}
}
