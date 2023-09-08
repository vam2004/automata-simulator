package prefixes;
public class Test {
	public static Tokenizer sample() {
		Token tkn_free = new Token("free");
		Token tkn_freely = new Token("freely", tkn_free);
		Token tkn_warzone = new Token("warzone");
		Token tkn_warrior = new Token("warrior");
		Node node_00 = new Node(51, null, new Node(tkn_warrior), new Node(tkn_warzone));
		Node node_01 = new Node(0, null, new Node(tkn_freely), node_00);
		return new Tokenizer(node_01);
	}
	public static void main(String[] args) {
		Tokenizer tokenizer = sample();
		System.out.println(tokenizer.root);
		String seq = args[0];
		int length = seq.length();
		System.out.println(seq);
		for(int i = 0; i < length; i++) {
			for(int j = 15; j >= 0; j--) {
				System.out.print(Node.getBitAt(seq, i, j));
			}
			System.out.printf(" %d - %d\n", 16 * i, 16 * i + 15);
		}
		CharSequence matched = tokenizer.matchSubString(seq, 0);
		if(matched == null) {
			System.out.println("[NOT FOUND]");
		} else {
			System.out.printf("[FOUND] %s", matched);
		}
	}
}
