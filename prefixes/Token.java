package prefixes;
public class Token {
	public CharSequence seq;
	public Token prefix;
	public Token(CharSequence _seq, Token _prefix) {
		seq = _seq;
		prefix = _prefix;
	}
	public Token(CharSequence seq) {
		this(seq, null);
	}
	public int matchedLength(CharSequence src, int offset) {
		int srclength = src.length() - offset;
		int seqlength = seq.length();
		int length = srclength < seqlength ? srclength : seqlength;
		for(int i = 0; i < length; i++) {
			if(src.charAt(offset + i) != seq.charAt(i)){
				return i;
			}
		}
		return length;
	}
	public boolean isExact(CharSequence src, int offset) {
		int length = matchedLength(src, offset);
		return seq.length() == length;
	}
	public Token searchPrefix(int length) {
		for(Token token = prefix; token != null; token = token.prefix) {
			if(token.seq.length() <= length) {
				return token;
			}
		}
		return null;
	}
	public Token matchSubToken(CharSequence src, int offset) {
		int length = matchedLength(src, offset);
		if (seq.length() == length) {
			return this;
		}
		return searchPrefix(length);
	}
	public String toString(){
		return "FinalNode (" + seq + ")";
	}
}

