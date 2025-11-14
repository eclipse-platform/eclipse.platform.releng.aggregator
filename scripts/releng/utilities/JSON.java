/*******************************************************************************
 *  Copyright (c) 2025, 2025 Hannes Wellmann and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     Hannes Wellmann - initial API and implementation
 *******************************************************************************/
package utilities;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * A simple (incomplete) replication of the JSON spec, tied to our needs and
 * just for writing. To be replaced with the JDK's JSON API, once available.
 */
public class JSON {

	public static void write(JSON.Value value, Path file) throws IOException {
		try (var st = Files.newOutputStream(file)) {
			value.write(new PrintStream(st), 0);
		}
	}

	public interface Value {
		void write(PrintStream stream, int depth);
	}

	public record Object(Map<java.lang.String, JSON.Value> members) implements JSON.Value {
		public static JSON.Object create() {
			return new JSON.Object(new LinkedHashMap<>());
		}

		public void add(java.lang.String key, JSON.Value value) {
			JSON.Value existingValue = members.put(key, value);
			if (existingValue != null) {
				throw new IllegalArgumentException("Key <" + key + "> already present: " + existingValue);
			}
		}

		@Override
		public void write(PrintStream stream, int depth) {
			stream.print("{");
			printNewline(stream, depth + 1);
			for (Iterator<Entry<java.lang.String, JSON.Value>> iterator = members.entrySet().iterator(); iterator
					.hasNext();) {
				Entry<java.lang.String, JSON.Value> entry = iterator.next();
				java.lang.String key = entry.getKey();
				JSON.Value value = entry.getValue();
				stream.print('"');
				stream.print(key);
				stream.print("\": ");
				value.write(stream, depth + 1);
				if (iterator.hasNext()) {
					stream.print(",");
					printNewline(stream, depth + 1);
				}
			}
			printNewline(stream, depth);
			stream.print("}");
		}

		@Override
		public final java.lang.String toString() {
			return JSON.toString(this);
		}
	}

	public record Array(List<JSON.Value> elements) implements JSON.Value {

		public Array { // ensure elements are modifiable
			elements = new ArrayList<>(elements);
		}

		public static JSON.Array create(JSON.Value... elements) {
			return new JSON.Array(List.of(elements));
		}

		public static <T extends JSON.Value> Collector<T, ?, JSON.Array> toJSONArray() {
			return Collectors.collectingAndThen(Collectors.toList(), l -> new JSON.Array(new ArrayList<>(l)));
		}

		public void add(JSON.Value value) {
			elements.add(value);
		}

		@Override
		public void write(PrintStream stream, int depth) {
			if (elements.isEmpty()) {
				stream.print("[]");
				return;
			}
			stream.print("[");
			printNewline(stream, depth + 1);
			for (Iterator<JSON.Value> iterator = elements.iterator(); iterator.hasNext();) {
				JSON.Value element = iterator.next();
				element.write(stream, depth + 1);
				if (iterator.hasNext()) {
					stream.print(",");
					printNewline(stream, depth + 1);
				}
			}
			printNewline(stream, depth);
			stream.print("]");
		}

		@Override
		public final java.lang.String toString() {
			return JSON.toString(this);
		}
	}

	public record String(java.lang.String characters) implements JSON.Value {
		public static JSON.String create(java.lang.String characters) {
			return new JSON.String(characters);
		}

		@Override
		public void write(PrintStream stream, int depth) {
			stream.print('"');
			stream.print(characters);
			stream.print('"');
		}

		@Override
		public final java.lang.String toString() {
			return JSON.toString(this);
		}
	}

	public record Integer(int integer) implements JSON.Value {
		public static JSON.Integer create(int integer) {
			return new JSON.Integer(integer);
		}

		@Override
		public void write(PrintStream stream, int depth) {
			stream.print(integer);
		}

		@Override
		public final java.lang.String toString() {
			return JSON.toString(this);
		}
	}

	private static java.lang.String toString(JSON.Value value) {
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		value.write(new PrintStream(stream), 0);
		return stream.toString();
	}

	private static void printNewline(PrintStream stream, int depth) {
		stream.println();
		stream.print("\t".repeat(depth));
	}
}
