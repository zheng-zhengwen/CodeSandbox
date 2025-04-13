#include <iostream>
#include <string>
#include <algorithm>

int main(int argc, char* argv[]) {
    std::string a = argv[1];
    std::string b = argv[2];
    std::string result = addBinary(a, b);
    std::cout << result << std::endl;
    return 0;
}