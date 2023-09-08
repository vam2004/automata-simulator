package prefixes;
public class FinalNode extends MatchNode {
	public CharSequence seq;
	public FinalNode preffix;
	public FinalNode(CharSequence sseq, FinalNode spreffix) {
		seq = sseq;
		preffix = spreffix;
	}
	public FinalNode(CharSequence seq) {
		this(seq, null);
	}
	public int countMatched(CharSequence src, int offset) {
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
	public FinalNode matchPreffix(CharSequence src, int offset) {
		int length = countMatched(src, offset);
		for(FinalNode node = this; node != null; node = node.preffix) {
			if(node.seq.length() <= length) {
				return node;
			}
		}
		return null;
	}
	public CharSequence matchSequence(CharSequence src, int offset){
		FinalNode matched = matchPreffix(src, offset);
		return matched == null ? null : matched.seq;
	}
	@Override
	public boolean isFinal(){
		return true;
	}
	@Override
	public boolean isMiddle(){
		return false;
	}
	@Override
	public String toString(){
		return "FinalNode (" + seq + ")";
	}
}

