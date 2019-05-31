/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.test.extension;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.junit.jupiter.api.extension.Extension;

import org.springframework.boot.ansi.AnsiOutput;
import org.springframework.boot.ansi.AnsiOutput.Enabled;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Provides access to {@link System#out System.out} and {@link System#err System.err}
 * output that has been capture by the {@link OutputExtension}. Can be used to apply
 * assertions either using AssertJ or standard JUnit assertions. For example:
 * <pre class="code">
 * assertThat(output).contains("started"); // Checks all output
 * assertThat(output.getErr()).contains("failed"); // Only checks System.err
 * assertThat(output.getOut()).contains("ok"); // Only checks System.put
 * </pre>
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 * @since 2.2.0
 * @see OutputExtension
 */
public class CapturedOutput implements CharSequence, Extension {

	private final Deque<SystemCapture> systemCaptures = new ArrayDeque<>();

	private AnsiOutputState ansiOutputState;

	protected CapturedOutput() {
	}

	/**
	 * Push a new system capture session onto the stack.
	 */
	protected final void push() {
		if (this.systemCaptures.isEmpty()) {
			this.ansiOutputState = AnsiOutputState.saveAndDisable();
		}
		this.systemCaptures.addLast(new SystemCapture());
	}

	/**
	 * Pop the last system capture session from the stack.
	 */
	protected final void pop() {
		this.systemCaptures.removeLast().release();
		if (this.systemCaptures.isEmpty() && this.ansiOutputState != null) {
			this.ansiOutputState.restore();
			this.ansiOutputState = null;
		}
	}

	@Override
	public int length() {
		return toString().length();
	}

	@Override
	public char charAt(int index) {
		return toString().charAt(index);
	}

	@Override
	public CharSequence subSequence(int start, int end) {
		return toString().subSequence(start, end);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj instanceof CapturedOutput || obj instanceof CharSequence) {
			return getAll().equals(obj.toString());
		}
		return false;
	}

	@Override
	public int hashCode() {
		return toString().hashCode();
	}

	@Override
	public String toString() {
		return getAll();
	}

	/**
	 * Return all content (both {@link System#out System.out} and {@link System#err
	 * System.err}) in the order that it was was captured.
	 * @return all captured output
	 */
	public String getAll() {
		return get((type) -> true);
	}

	/**
	 * Return {@link System#out System.out} content in the order that it was was captured.
	 * @return {@link System#out System.out} captured output
	 */
	public String getOut() {
		return get(Type.OUT::equals);
	}

	/**
	 * Return {@link System#err System.err} content in the order that it was was captured.
	 * @return {@link System#err System.err} captured output
	 */
	public String getErr() {
		return get(Type.ERR::equals);
	}

	private String get(Predicate<Type> filter) {
		Assert.state(!this.systemCaptures.isEmpty(),
				"No system captures found. Check that you have used @RegisterExtension "
						+ "or @ExtendWith and the fields are not private");
		StringBuilder builder = new StringBuilder();
		for (SystemCapture systemCapture : this.systemCaptures) {
			systemCapture.append(builder, filter);
		}
		return builder.toString();
	}

	/**
	 * A capture session that captures {@link System#out System.out} and {@link System#out
	 * System.err}.
	 */
	private static class SystemCapture {

		private final PrintStreamCapture out;

		private final PrintStreamCapture err;

		private final List<CapturedString> capturedStrings = Collections
				.synchronizedList(new ArrayList<>());

		SystemCapture() {
			this.out = new PrintStreamCapture(System.out, this::captureOut);
			this.err = new PrintStreamCapture(System.err, this::captureErr);
			System.setOut(this.out);
			System.setErr(this.err);
		}

		public void release() {
			System.setOut(this.out.getParent());
			System.setErr(this.err.getParent());
		}

		private void captureOut(String string) {
			this.capturedStrings.add(new CapturedString(Type.OUT, string));
		}

		private void captureErr(String string) {
			this.capturedStrings.add(new CapturedString(Type.ERR, string));
		}

		public void append(StringBuilder builder, Predicate<Type> filter) {
			for (CapturedString stringCapture : this.capturedStrings) {
				if (filter.test(stringCapture.getType())) {
					builder.append(stringCapture);
				}
			}
		}

	}

	/**
	 * A {@link PrintStream} implementation that captures written strings.
	 */
	private static class PrintStreamCapture extends PrintStream {

		private final PrintStream parent;

		PrintStreamCapture(PrintStream parent, Consumer<String> copy) {
			super(new OutputStreamCapture(getSystemStream(parent), copy));
			this.parent = parent;
		}

		public PrintStream getParent() {
			return this.parent;
		}

		private static PrintStream getSystemStream(PrintStream printStream) {
			while (printStream instanceof PrintStreamCapture) {
				return ((PrintStreamCapture) printStream).getParent();
			}
			return printStream;
		}

	}

	/**
	 * An {@link OutputStream} implementation that captures written strings.
	 */
	private static class OutputStreamCapture extends OutputStream {

		private final PrintStream systemStream;

		private final Consumer<String> copy;

		OutputStreamCapture(PrintStream systemStream, Consumer<String> copy) {
			this.systemStream = systemStream;
			this.copy = copy;
		}

		@Override
		public void write(int b) throws IOException {
			write(new byte[] { (byte) (b & 0xFF) });
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			this.copy.accept(new String(b, off, len));
			this.systemStream.write(b, off, len);
		}

		@Override
		public void flush() throws IOException {
			this.systemStream.flush();
		}

	}

	/**
	 * A captured string that forms part of the full output.
	 */
	private static class CapturedString {

		private final Type type;

		private final String string;

		CapturedString(Type type, String string) {
			this.type = type;
			this.string = string;
		}

		public Type getType() {
			return this.type;
		}

		@Override
		public String toString() {
			return this.string;
		}

	}

	/**
	 * Types of content that can be captured.
	 */
	private enum Type {

		OUT, ERR

	}

	/**
	 * Save disable and restore AnsiOutput without it needing to be on the classpath.
	 */
	private static class AnsiOutputState {

		private Enabled saved;

		AnsiOutputState() {
			this.saved = AnsiOutput.getEnabled();
			AnsiOutput.setEnabled(Enabled.NEVER);
		}

		public void restore() {
			AnsiOutput.setEnabled(this.saved);
		}

		public static AnsiOutputState saveAndDisable() {
			if (!ClassUtils.isPresent("org.springframework.boot.ansi.AnsiOutput",
					CapturedOutput.class.getClassLoader())) {
				return null;
			}
			return new AnsiOutputState();
		}

	}

}
