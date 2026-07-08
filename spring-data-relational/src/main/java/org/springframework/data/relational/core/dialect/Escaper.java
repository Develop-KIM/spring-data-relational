/*
 * Copyright 2020-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.relational.core.dialect;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.jspecify.annotations.Nullable;

/**
 * Helper class encapsulating an escape character for LIKE queries and the actual usage of it in escaping
 * {@link String}s.
 *
 * @author Roman Chigvintsev
 * @author Mark Paluch
 * @author Alexander Tochin
 * @author Jens Schauder
 * @since 2.0
 */
public class Escaper {

	public static final Escaper ANSI_LIKE_ESCAPER = Escaper.escapeLikeWith('\\');
	public static final Escaper ANSI_LITERAL_ESCAPER = Escaper.of('\'', Set.of("'"));

	/**
	 * @deprecated since 4.2, use {@link #ANSI_LIKE_ESCAPER} instead.
	 */
	@Deprecated
	public static final Escaper DEFAULT = ANSI_LIKE_ESCAPER;

	private final char escapeCharacter;
	private final Set<String> toReplace;

	private Escaper(char escapeCharacter, Set<String> toReplace) {

		this.escapeCharacter = escapeCharacter;
		this.toReplace = new HashSet<>(toReplace);
	}

	/**
	 * Creates a new instance of this class with the given escape character escaping the {@code LIKE} special characters
	 * {@code _} and {@code %}.
	 *
	 * @param escapeCharacter escape character
	 * @return new instance of {@link Escaper}.
	 * @throws IllegalArgumentException if the escape character is one of the special characters ('_' and '%')
	 */
	public static Escaper escapeLikeWith(char escapeCharacter) {

		Set<String> toReplace = Set.of("_", "%");
		if (toReplace.contains(Character.toString(escapeCharacter))) {
			throw new IllegalArgumentException(
					String.format("'%s' cannot be used as escape character as it should be replaced", escapeCharacter));
		}

		return Escaper.of(escapeCharacter, toReplace);
	}

	/**
	 * Creates a new instance of this class with the given escape character escaping the {@code LIKE} special characters
	 * {@code _} and {@code %}.
	 *
	 * @param escapeCharacter escape character
	 * @return new instance of {@link Escaper}.
	 * @throws IllegalArgumentException if the escape character is one of the special characters ('_' and '%')
	 * @deprecated since 4.2, use {@link #escapeLikeWith(char)} instead.
	 */
	@Deprecated
	public static Escaper of(char escapeCharacter) {
		return escapeLikeWith(escapeCharacter);
	}

	/**
	 * Creates a new instance of this class with the given escape character and the characters to be escaped. In contrast
	 * to {@link #of(char)} the {@code escapeCharacter} may itself be part of {@code toReplace}; this is the standard SQL
	 * way of escaping a single quote inside a string literal by doubling it ({@code '} &rarr; {@code ''}).
	 *
	 * @param escapeCharacter escape character.
	 * @param toReplace characters/char sequences that should be escaped.
	 * @return new instance of {@link Escaper}.
	 * @since 4.1
	 */
	public static Escaper of(char escapeCharacter, Set<String> toReplace) {
		return new Escaper(escapeCharacter, toReplace);
	}

	/**
	 * Apply the {@link Escaper} to the given {@code chars}.
	 *
	 * @param chars characters/char sequences that should be escaped.
	 * @return a new {@link Escaper} instance with the given characters added to the list of characters to be escaped.
	 */
	public Escaper withRewriteFor(String... chars) {

		HashSet<String> toReplace = new HashSet<>(this.toReplace.size() + chars.length);
		toReplace.addAll(this.toReplace);
		toReplace.addAll(Arrays.asList(chars));

		return new Escaper(this.escapeCharacter, toReplace);
	}

	/**
	 * Returns the escape character.
	 *
	 * @return the escape character to use.
	 */
	public char getEscapeCharacter() {
		return escapeCharacter;
	}

	/**
	 * Escapes all special like characters ({@code _}, {@code %}) using the configured escape character. Escape character
	 * itself is also escaped.
	 *
	 * @param value value to be escaped
	 * @return escaped value
	 */
	public @Nullable String escape(@Nullable String value) {

		if (value == null) {
			return null;
		}

		String escapeCharString = String.valueOf(escapeCharacter);
		String escapedValue = value.replace(escapeCharString, escapeCharString.repeat(2));
		for (String character : toReplace) {

			// the escape character was already doubled in the step above; doubling it again would be wrong
			if (character.equals(escapeCharString)) {
				continue;
			}

			escapedValue = escapedValue.replace(character, escapeCharacter + character);
		}

		return escapedValue;
	}
}
