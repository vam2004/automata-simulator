package prefixes;
public class Match {
	public MiddleNode root;
	public Match(MiddleNode sroot) {
		root = sroot;
	}
	public Match(){
		root = null;
	}
	public boolean isEmpty(){
		return root == null;
	}
	public MatchNode matchLazy(CharSequence src, int offset) {
		final int length = src.length();
		final int bitlength = length * 16;
		final int bitoffset = offset * 16;
		if(root == null) {
			return null;
		}
		MiddleNode node = root;
		while(true) {
			MatchNode next = node.next(src, bitoffset, bitlength);
			if(next == null) {
				return node;
			}
			if(next.isFinal()){
				return next;
			}
			node = (MiddleNode) next;
		}
	}
	public FinalNode matchFinal(CharSequence src, int offset) {
		final MatchNode last = matchLazy(src,offset);
		return last.isFinal() ? ((FinalNode) last) : ((MiddleNode) last).closestFinal();
	}
	public CharSequence match(CharSequence src, int offset) {
		FinalNode last = matchFinal(src, offset);
		// System.out.println(last);
		return last.matchSequence(src, offset);
	}
}



