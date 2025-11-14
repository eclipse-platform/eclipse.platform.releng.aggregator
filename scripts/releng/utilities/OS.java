package utilities;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
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

	public static Map<Path, Long> listFileTree(Path root, Optional<Path> filesListFile, Path directory, int depth)
			throws IOException {
		if (filesListFile.isPresent()) {
			return Files.lines(filesListFile.get()).map(line -> line.startsWith("./") ? line.substring(2) : line)
					.map(line -> {
						String[] elements = line.split(" ");
						if (elements.length != 2) {
							System.out.println(
									"Line does not contain exactly two space-separated elements. Ignoring:" + line);
							return null;
						}
						return Map.entry(Path.of(elements[0]), Long.parseLong(elements[1]));
					}).filter(Objects::nonNull).filter(e -> {
						Path path = e.getKey();
						return (directory == null || path.startsWith(directory)) && path.getNameCount() <= depth;
					}).collect(Collectors.toUnmodifiableMap(Entry::getKey, Entry::getValue));
		} else {
			int searchDepth = depth - (directory != null ? directory.getNameCount() : 0);
			Path start = directory != null ? root.resolve(directory) : root;
			try (var filePaths = Files.walk(start, searchDepth).filter(Files::isRegularFile)) {
				return filePaths.collect(Collectors.toMap(f -> root.relativize(f), f -> {
					try {
						return Files.size(f);
					} catch (IOException e) {
						throw new UncheckedIOException(e);
					}
				}));
			}
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
