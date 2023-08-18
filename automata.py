usage = """
This program reads a automata from .csv file and executes it 
USAGE:
    automata --help
    automata --test [filename]
    automata --show [filename] 
The input is provided by the stdin. Options:
    --help    shows this dialogue and exit
    --show    prints the final states of the automata
    --test    prints wheter the automata recognizes the input 
"""

syntax = """
EPSILON TRANSITION SYMBOL:
    The following symbols are considered epsilon transition symbol:
        (utf8): "ε" - Greek Capital Letter Epsilon (U+0395)
        (ascii): "epsilon" 
FILE FORMAT:
    The source should be a valid .csv (comma/space separed values) file.
DEFAULT ORIENTATION:
    The symbols are placed in the first row after the first column (symbols section)
    The states are placed in the first column after the first row (state section)
    The cell in the first row and column should be empty.
DECLARED STATE:
    A states name is declared on the state section.
DEAD STATE:
    The empty string represents the dead state which consumes all symbols and therefore never reaches a final state.
SYMBOL:
    A symbol is a non-empty string that doen't contain any ':' and which uses '\\' as special charater:
    	The character '\\' is encoded as "\\\\";
    	The character ':' is encoded as "\\:";
    	Any byte(u8) is encoded by its hexdecimal value prefixed by '\\x'
    	Any word(u16) is encoded by four hexdecimal digits prefixed by '\\w'
SYMBOL CELL:
    A symbol cell contains multiples symbols separed by ':'. Any redundant ':' separator in the end of symbol cell may be ignored
UNPREFIXED FORM:
    A unprefixed form is a non-empty string that doen't contain ',' and doesn't have traling withspaces. 
PREFIXED FORM:
    A state name can be in prefixed form, which contains either "+" or "*" as type prefix, followed by its unprefixed form or empty string
PROPER PREFIXED FORM:
    A proper prefixed form is a prefixed form which the sufix is its unprefixed form
UNPREFIDEXED REFENCE:
    Any declared state form has precedence of unprefixed form when referencing states. Otherwise, both prefixed and unprefixed, if exists, forms can be used to reference a state.
INITIAL STATE:
    The prefixed inital state is the first state that uses "+" as type prefix. If doesn't exists, then the first declared state is assumed to be the initial state.
FINAL STATE:
    All final states uses "*" as type prefix.
EMPTY TRANSITION:
    The epsilon transition symbol is considered as empty transition only in the first symbol cell as the first symbol
    If there is one empty transition, all subsequentes epsilon symbols are considered as literal symbols
"""
import sys
def main():
    args = sys.argv[1:]
    if(len(args) < 2):
        return print(usage[1:-1])
    operation = args[0]
    filename = args[1]
    print(operation)
    match(operation):
        case "--help" | "-h":
            print(usage)
        case "--syntax" | "syntax":
            print(syntax)
        case "--show" | "-s" | "show":
            Automata().open(filename).run().show()
        case "--test" | "-t" | "test":
            Automata().open(filename).run().show_is_final()
        case _:
            print("Invalid Operation. See --help for more details")

class InvalidSyntax(Exception):
    def __init__(self, foward):
        Exception.__init__(self, foward)
        self.foward = foward
class InvalidFilename(InvalidSyntax):
    def __init__(self):
        message ="Invalid Filename. See --help for more details"
        InvalidSyntax.__init__(self, message)
class EmptyHeader(InvalidSyntax):
    def __init__(self):
        message ="Empty Header. See --syntax for more details"
        InvalidSyntax.__init__(self, message)
class EmptySymbol(InvalidSyntax):
    def __init__(self):
        message ="Empty Symbol. See --syntax for more details"
        InvalidSyntax.__init__(self, message)
class InvalidHex(InvalidSyntax):
    def __init__(self):
        message ="Invalid Hexdecimal Digit"
        InvalidSyntax.__init__(self, message)
class UnclosedHex(InvalidSyntax):
    def __init__(self):
        message ="Unclosed Hexdecimal Substring"
        InvalidSyntax.__init__(self, message)
class InvalidScapeSequence(InvalidSyntax):
    def __init__(self):
        message ="Invalid Scape Sequence"
        InvalidSyntax.__init__(self, message)

def hexvalue(src):
    src = ord(src)
    if src < 48:
        raise InvalidHex()
    if src < 58:
        return src - 48
    if src < 65:
        raise InvalidHex()
    if src < 71:
        return src - 55
    if src < 97:
        raise InvalidHex()
    if src < 103:
        return src - 87
    raise InvalidHex()
def get_hexbyte(buffer, idx):
    if idx >= len(buffer):
        raise UnclosedHex()
    h0 = hexvalue(buffer[idx])
    h1 = hexvalue(buffer[idx + 1])
    return h1 + (h0 << 4)
def get_hexword(buffer, idx):
    b0 = get_hexbyte(buffer, idx)
    b1 = get_hexbyte(buffer, idx + 2)
    return b0 + (b1 << 8)
class bytesstack:
    def __init__(self, size):
        self.buffer = bytearray(size)
        self.ptr = 0
        self.size = size
    def append(self, value):
        if value == 0:
            self.append_byte(0)
        while value != 0:
            self.append_byte(value & 0xff)
            value = value >> 8
    def append_byte(self, value):
        if self.ptr < self.size:
            self.buffer[self.ptr] = value
            self.ptr += 1
        else:
            self.buffer.append(value)
    def unwrap(self):
        if self.ptr != 0:
            return self.buffer[:self.ptr]
        return bytearray()
class SymbolCell:
    def __init__(self, buffer):
        self.buffer = buffer
        self.pos = 0
    def __iter__(self):
        return self
    def __next__(self):
        scape = False
        base = self.pos
        size = len(self.buffer)
        if base >= size:
            return None
        buffer = bytesstack(size - base)
        index = base
        while index < size:
            value = self.buffer[index]
            if scape:
                match(value):
                    case '\\':
                        buffer.append(ord('\\'))
                    case ':':
                        buffer.append(ord(':'))
                    case 'x':
                        buffer.append(get_hexbyte(self.buffer, index + 1))
                        index += 2
                    case 'w':
                        buffer.append(get_hexword(self.buffer, index + 1))
                        index += 4
                    case _:
                        raise InvalidScapeSequence()
                scape = False
                index += 1
                continue
            match(value):
                case ':':
                    if index == base:
                        raise EmptySymbol() 
                    index += 1
                    break
                case '\\':
                    scape = True
                case _:
                    buffer.append(ord(value))
            index += 1
        self.pos = index
        return buffer.unwrap()
def first_symbol_cell(cell):
        factory = SymbolCell(cell)
        first_symbol = next(factory, None)
        empty_transition = False
        header = []
        if first_symbol == b'\xb5\x03' or first_symbol == b'epsilon':
            empty_transition = True
        else:
            header.append(bytes(first_symbol))
        insert_symbol_array(header, factory)
        return header, empty_transition
def insert_symbol_array(header, factory):
    for value in factory:
        if value == None:
            break
        header.append(bytes(value))
def get_symbol_array(cell):
    header = []
    insert_symbol_array(header, SymbolCell(cell))
    return header
class Automata:
    def __init__(self):
        self.symbols = None
        self.table = None
        self.error = None
        self.empty_transition = None
    def open(self, filename, dialect='excel', **fmtparams):
        file = None
        try:
           file = open(filename)
        except OSError as error:
            self.error = InvalidFilename()
            return self
        return self.use(file, dialect, **fmtparams)
    def use(file, dialect="excel", **fmtparams):
        with file as source:
            return self.parse(csv.reader(source, dialect, **fmtparams))
    def parse(reader):
        raw_header = next(reader, None)
        if raw_header == None or len(raw_header) == 0:
            self.error = EmptyHeader()
            return self
        header = []
        try:
            header.append(first_symbol_cell(raw_header[1]))
        except InvalidSyntax as error:
            self.error = error
            return self
        for cell in raw_header[1:]:
            header.append(get_symbol_array(cell))
    def unwrap(self):
        if self.error != None:
            raise self.error
        else:
            return self
    def clear_error(self):
        self.error = None
        return self
    def step(self):
        pass
    def run(self):
        # todo
        return self
    def print_error(self):
        print("ERROR: %s" % (self.error.foward,))
    def has_error(self):
        return self.error != None
    def show(self):
        if self.has_error():
            self.print_error()
        else:
            # todo
            pass
            
    def is_final(self):
        # todo
        pass
    def show_is_final(self):
        if self.has_error():
            self.print_error()
        return None
        #elif self.is_final()
            #print("true")
        #else:
            #print("false")
if __name__ == "__main__":
    main()
