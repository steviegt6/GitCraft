package com.github.winplay02.gitcraft.types;

import com.github.winplay02.gitcraft.GitCraft;
import com.github.winplay02.gitcraft.pipeline.Step;
import com.github.winplay02.gitcraft.util.MiscHelper;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * An OptiFine version.
 * @param version The downloadable version string
 */
public record OptiFineVersion(String version) {
	/**
	 * Get the URL to download this version.
	 * @return The URL to download this version
	 */
	public String getUrl() {
		// http://optifine.net/adloadx?f=OptiFine_1.21.4_HD_U_J3.jar
		return "https://optifine.net/adloadx?f=OptiFine_" + version + ".jar";
	}

	public Path resolve(Path containingPath) {
		return containingPath.resolve("OptiFine_" + version + ".jar");
	}

	public Step.StepResult fetchArtifact(Path containingPath) {
		String url = getUrl();
		Path targetFile = resolve(containingPath);

		// TODO: Can we checksum this?
		if (Files.exists(targetFile)) {
			return Step.StepResult.UP_TO_DATE;
		}

		if (targetFile.getParent() != null) {
			try {
				Files.createDirectories(targetFile.getParent());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		do {
			try {
				MiscHelper.println("Fetching OptiFine JAR %s from: %s", version, url);
				URLConnection url_connection = new URL(url).openConnection(Proxy.NO_PROXY);
				url_connection.setUseCaches(false);
				try (OutputStream file_output = Files.newOutputStream(targetFile, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)) {
					try (InputStream open_stream = new BufferedInputStream(url_connection.getInputStream())) {
						open_stream.transferTo(file_output);
					}
					file_output.flush();
				}
			} catch (Exception e1) {
				MiscHelper.println("\u001B[31mFailed to fetch URL (retrying in %sms): %s (%s)\u001B[0m", GitCraft.config.failedFetchRetryInterval, url, e1);
				MiscHelper.sleep(GitCraft.config.failedFetchRetryInterval);
			}
		} while (Files.exists(targetFile));
		return Step.StepResult.SUCCESS;
	}
}
