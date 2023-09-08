package prefixes;
public class Node {
	public int pos;
	public Token token;
	public Node left;
	public Node right;
	public Node(int _pos, Token _token, Node _left, Node _right) {
		pos = _pos;
		token = _token;
		left = _left;
		right = _right;
	}
	public Node(Token token) {
		this(token.seq == null ? Integer.MAX_VALUE : token.seq.length() * 16, token, null, null);
	}
	public int byteOffset() {
		return pos >> 4;
	}
	public int bitOffset() {
		return pos & 0xf;
	}
	public static int getBitAt(CharSequence src, int offset, int bitpos) {
		return (src.charAt(offset) >> bitpos) & 0x1;
	}
	public static boolean isSetAt(CharSequence src, int offset, int bitpos){
		return getBitAt(src, offset, bitpos) != 0;
	}
	public Node next(CharSequence seq, int offset) {
		return isSetAt(seq, offset + byteOffset(), bitOffset()) ? right : left;
	}
	public Node firstTokenNodeLeft() {
		Node node = this;
		while(node.token == null) {
			node = node.left;
		}
		return node;
	}
	public Node firstTokenNodeRight(){
		Node node = this;
		while(node.token == null) {
			node = node.right;
		}
		return node;
	}
	public Token firstTokenLeft(){
		return firstTokenNodeLeft().token;
	}
	public Token firstTokenRight(){
		return firstTokenNodeRight().token;
	}
	@Override
	public String toString(){
		return String.format("Node (%d, '%s', '%s')", pos, firstTokenLeft().seq, firstTokenRight().seq);
	}
}
