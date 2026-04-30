#include <iostream>
#include <vector>
using namespace std;

int main(){
    vector<int> num = {1,1,2,3,3,4,5,5,6,7};
    int* tmp = &num[0];
    int* pos = &num[0];
    int* next = &num[1]; 
    for(int i = 0; i < num.size(); i++){
        cout << num[i] << " ";
    }
    while(next!= &num[num.size()]){
        if(*pos == *next){
            next++;
        }
        if(*pos != *next){
            int swap = *pos;
            *pos = *next;
            *next = swap;
            tmp++;
            next = pos + 1;
        }
    }
    for(int *i = &num[0]; i <= tmp; i++){
        cout << *i << " ";
    }
    return 0;
}