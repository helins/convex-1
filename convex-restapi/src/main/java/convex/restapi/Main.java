package convex.restapi;

import com.hellokaton.blade.Blade;

public class Main {

	public static void main(String[] args) {
		Blade.of().listen(9001).start();
	}
}