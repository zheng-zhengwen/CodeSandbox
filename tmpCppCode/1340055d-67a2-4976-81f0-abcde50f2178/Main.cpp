#include <iostream>
#include <cstdlib> // 用于 atoi
#include <vector>
#include <algorithm>

using namespace std;

int trap(vector<int>& height) {
    if (height.empty()) return 0;
    
    int left = 0, right = height.size() - 1;
    int left_max = height[left], right_max = height[right];
    int result = 0;
    
    while (left < right) {
        if (left_max < right_max) {
            left++;
            if (height[left] < left_max) {
                result += left_max - height[left];
            } else {
                left_max = height[left];
            }
        } else {
            right--;
            if (height[right] < right_max) {
                result += right_max - height[right];
            } else {
                right_max = height[right];
            }
        }
    }
    
    return result;
}

int main(int argc, char* argv[]) {
    vector<int> height;
    for (int i = 1; i < argc; i++) {
        height.push_back(atoi(argv[i]));
    }
    cout << trap(height) << endl;
    return 0;
}