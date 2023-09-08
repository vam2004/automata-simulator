package prefixes;
public class Test {
	public static void main(String[] args) {
		FinalNode prefixed = new FinalNode("freely", new FinalNode("free"));
		MiddleNode unprefixed = new MiddleNode(51, 0, new FinalNode("warrior"), new FinalNode("warzone"));
		Match pattern = new Match(new MiddleNode(0, 0, prefixed, unprefixed));
		String seq = args[0];
		int length = seq.length();
		System.out.println(seq);
		for(int i = 0; i < length; i++) {
			int k = 16 * i;
			for(int j = 15; j >= 0; j--) {
				System.out.print(MiddleNode.getBitAt(seq, k + j));
			}
			System.out.printf(" %d - %d\n", k, k + 15);
		}
		CharSequence matched = pattern.match(seq, 0);
		if(matched == null) {
			System.out.println("[NOT FOUND]");
		} else {
			System.out.printf("[FOUND] %s", matched);
		}
	}
}
