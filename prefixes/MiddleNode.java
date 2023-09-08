package prefixes;
public class MiddleNode extends MatchNode {
	public int pos;
	public int distance;
	public MatchNode left;
	public MatchNode right;
	public static int getBitAt(CharSequence seq, int pos) {
		return (seq.charAt(pos >> 4) >> (pos & 0xf)) & 0x1;
	}
	public MiddleNode(int spos, int sdistance, MatchNode sleft, MatchNode sright) {
		pos = spos;
		left = sleft;
		right = sright;
		distance = sdistance;
	}
	public MatchNode towardsFinal() {
		if(!left.isMiddle()) {
			return left;
		}
		if(!right.isMiddle()) {
			return right;
		}
		MiddleNode mleft = (MiddleNode) left;
		MiddleNode mright = (MiddleNode) right;
		return mleft.distance < mright.distance ? mleft : mright;
	}
	public FinalNode closestFinal() {
		MatchNode node = this;
		while(!node.isFinal()) {
			node = ((MiddleNode) node).towardsFinal();
		}
		return (FinalNode) node;
	}
	public MatchNode next(CharSequence seq, int bitoffset, int bitlength) {
		int bpos = pos + bitoffset;
		if(bpos < bitlength) {
			return getBitAt(seq, bpos) != 0 ? right : left;
		}
		return null;
	}
	public MiddleNode foremostLeft(){
		MiddleNode node = this;
		while(!node.left.isFinal()){
			node = (MiddleNode) node.left;
		}
		return node;
	}
	public FinalNode foremostLeftFinal(){
		return (FinalNode) foremostLeft().left;
	}
	public MiddleNode foremostRight(){
		MiddleNode node = this;
		while(!node.right.isFinal()){
			node = (MiddleNode) node.right;
		}
		return node;
	}
	public FinalNode foremostRightFinal(){
		return (FinalNode) foremostRight().right;
	}
	@Override
	public boolean isFinal(){
		return false;
	}
	@Override
	public boolean isMiddle(){
		return true;
	}
	@Override
	public String toString(){
		return "(" + pos + ", '" + foremostLeftFinal().seq + "', '" + foremostRightFinal().seq + "')";
	}
}
