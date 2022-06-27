all:
	javac -d . jcomp/*.java

run:
	java jcomp.Compiler $(F)
