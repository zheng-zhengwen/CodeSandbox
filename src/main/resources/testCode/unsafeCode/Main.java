public class Main {
    public int maximumTripletValue(int[] nums) {
        int max = Integer.MIN_VALUE; // Initialize to smallest possible integer
        for (int i = 0; i < nums.length - 2; i++) {
            for (int j = i + 1; j < nums.length - 1; j++) {
                for (int k = j + 1; k < nums.length; k++) {
                    int value = (nums[i] - nums[j]) * nums[k];
                    max = Math.max(max, value);
                }
            }
        }
        return max < 0 ? 0 : max; // Return 0 if all triplets give negative value
    }

    public static void main(String[] args) {
        int[] nums = new int[args.length];
        for (int i = 0; i < args.length; i++) {
            nums[i] = Integer.parseInt(args[i]);
        }
        Main solution = new Main();
        int result = solution.maximumTripletValue(nums);
        System.out.println(result);
    }
}