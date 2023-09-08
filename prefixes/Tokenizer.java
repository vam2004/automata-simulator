package prefixes;
public class Tokenizer {
	public Node root;
	public Tokenizer(Node _root) {
		root = _root;
	}
	public Tokenizer() { /* root = null;*/ }
	public boolean isEmpty(){ return root == null; }
	public Token searchLastToken(CharSequence src, int offset) {
		final int length = src.length();
		final int bitlen = length * 16;
		// if(isEmpty()) { return null; }
		Node node = root;
		Token last = null;
		// System.out.printf("@searchLastToken root=%s\n", root);
		for(;node != null && node.pos < bitlen; node = node.next(src, offset)) {
			System.out.printf("@searchLastToken node=%s\n", node);
			if(node.token != null){
				last = node.token;
			}
		}
		return node != null && node.token != null ? node.token : last;
	}
	public CharSequence matchSubString(CharSequence src, int offset) {
		Token prefix = matchSubToken(src, offset);
		if(prefix == null) { return null; }
		return prefix.seq;
	}
	public Token matchSubToken(CharSequence src, int offset) {
		Token last = searchLastToken(src, offset);
		System.out.println(last);
		if(last == null) { return null; }
		return last.matchSubToken(src,offset);
	}
}



