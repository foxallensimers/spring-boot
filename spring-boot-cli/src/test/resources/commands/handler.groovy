package org.test.command

import joptsimple.OptionSet

@Grab("org.eclipse.jgit:org.eclipse.jgit:2.3.1.201302201838-r")
import org.eclipse.jgit.api.Git

class TestCommand extends OptionHandler {

	void options() {
		option "foo", "Foo set"
	}

	void run(OptionSet options) {
		// Demonstrate use of Grape.grab to load dependencies before running
		println "Clean : " + Git.open(".." as File).status().call().isClean()
		org.springframework.boot.cli.command.ScriptCommandTests.executed = true
		println "Hello ${options.nonOptionArguments()}: ${options.has('foo')}"
	}

}
