public class Program {
    public static void main(String[] args) {
        for (int i = 0; i < 20; ++i) {
            for (int j = 0; j < 5; ++j) {
                for (int k = 0; k < 5; ++k) {
                    System.out.println();
                }

                for (int k = 0; k < 5; ++k) {
                    System.out.println();
                }
            }

            do {
                System.out.println();
                ++i;
            } while(i % 5 != 0);
        }
    }
}