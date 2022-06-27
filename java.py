from stateless.utils import *
from plumbum import local

class JavaValidate(Validate):
    def __init__(self, exe):
        self.exe = exe
        self.lex_tokens = [
                b"do",
                b"while",
                b"if",
                b"else",
                b"function",
                b"final",
                b"int",
                b"print"
                ]

    def validate(self, input_str):
        print(repr(input_str))
        tokens = None
        try:
            with open('tmp.mjava', 'wb+') as f:
                f.write(input_str)
                f.flush()
            javac = local[self.exe]
            ret,so,se = javac['-cp', 'out/production/java', 'jcomp.Compiler', 'tmp.mjava'].run(retcode=None)
            if ret == 0: return Status.Complete, None
            print(se);
            if 'got null' in  se:
                return Status.Incomplete, None
            return Status.Incorrect, None
        except AssertionError as e:
            print(e)
            return Status.Incorrect, None

validator = JavaValidate('/usr/bin/java')

