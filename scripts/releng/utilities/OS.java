package utilities;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Collectors;

public class OS {

	public static String readProperty(String name) {
		return Objects.requireNonNull(System.getProperty(name), "Not set: " + name);
	}

	public static boolean isWindowsArtifact(String name) {
		return name.contains("-win32");
	}

	public static boolean isLinuxArtifact(String name) {
		return name.contains("-linux");
	}

	public static boolean isMacOSArtifact(String name) {
		return name.contains("-macosx");
	}

	public static boolean isMacTarGZ(String f) {
		return OS.isMacOSArtifact(f.toString()) && f.endsWith(".tar.gz");
	}

	public static Map<String, String> loadProperties(Path file) throws IOException {
		Properties properties = new Properties();
		try (var stream = Files.newInputStream(file)) {
			properties.load(stream);
		}
		return properties.entrySet().stream().collect(Collectors.toUnmodifiableMap(e -> (String) e.getKey(), e -> {
			String value = e.getValue().toString().trim();
			if (value.startsWith("\"") && value.endsWith("\"")) {
				return value.substring(1, value.length() - 1);
			}
			return value;
		}));
	}

	public record FileInfo(long size, String hashSHA512) {
	}

	public static Map<Path, FileInfo> listFileTree(Path root, int searchDepth) throws IOException {
		try (var filePaths = Files.walk(root, searchDepth).filter(Files::isRegularFile)) {
			return filePaths.collect(Collectors.toMap(f -> root.relativize(f), f -> {
				try {
					String computeFileHash = computeFileHash(f, SHA512_DIGEST.get());
					return new FileInfo(Files.size(f), computeFileHash);
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			}));
		}
	}

	private static final ThreadLocal<MessageDigest> SHA512_DIGEST = ThreadLocal.withInitial(() -> {
		try {
			return MessageDigest.getInstance("SHA-512");
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	});

	public static String computeFileHash(Path file, MessageDigest digest) throws IOException {
		byte[] filesBytes = new byte[8192];
		try (var stream = Files.newInputStream(file)) {
			for (int read; (read = stream.read(filesBytes)) >= 0;) {
				digest.update(filesBytes, 0, read);
			}
			byte[] digestBytes = digest.digest();
			return HexFormat.of().formatHex(digestBytes);
		}
	}

	private static final long ONE_KILO_BYTE = 1024;
	private static final long ONE_MEGA_BYTE = ONE_KILO_BYTE * ONE_KILO_BYTE;
	private static final long TEN_MEGA_BYTE = 10 * ONE_MEGA_BYTE;
	private static final DecimalFormatSymbols ENGLISH_SYMBOLS = new DecimalFormatSymbols(Locale.US);
	private static final DecimalFormat DECIMAL_PLACES_ZERO = new DecimalFormat("0", ENGLISH_SYMBOLS);
	private static final DecimalFormat DECIMAL_PLACES_ONE = new DecimalFormat("0.0", ENGLISH_SYMBOLS);

	public static String fileSizeAsString(long size) {
		if (size < ONE_KILO_BYTE) {
			return size + " B";
		} else if (size < ONE_MEGA_BYTE) {
			return DECIMAL_PLACES_ZERO.format(1. * size / ONE_KILO_BYTE) + " kiB";
		} else if (size < TEN_MEGA_BYTE) {
			return DECIMAL_PLACES_ONE.format(1. * size / ONE_MEGA_BYTE) + " MiB";
		}
		return Math.round(1. * size / ONE_MEGA_BYTE) + " MiB";
	}

}
