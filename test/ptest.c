
// Check Struct and nested Struct
//struct s1 {
//    int a;
//    int b;
//    float c;
//    char d;
//    int arr[10];
//    char str[6];
//    struct s2 ss;
//};
//
//int a, b, c;
//float d;
//char e;
//int f[10];
//struct s2 g;

// Check Function call, done
//int f1(int a){
//    return a;
//}
// Check Binary op, done
//int f2(int a){
//    int res = a + 1;
//    //res = a + 1
//    res = res * 2;
//    //res = 2*a +2
//    res = res - 2;
//    //res = 2*a
//    res = res / 2;
//    //res = a
//    return res;
//}
// Check if statement, done
//int f3(int a){
//    int res;
//    if(a>1){
//        res = 1;
//    }else{
//        res = 0;
//    }
//    return res;
//}
// Check while statement, done
//int f4(int a){
//    int b = 0;
//    while (a>0){
//        a = a - 1;
//        b = b + 1;
//    }
//    return b;
//}
// Check for statement, done
//int f5(int a){
//    int i,b;
//    b = 0;
//    for(i = 0; i < a; i = i + 1;) {
//        b = b + 1;
//    }
//    return b;
//}
// Check Array, done
//struct s{
//    int a;
//};
//struct s arr[5][2];
//int f6(int a){
//    arr[1][1].a = a;
//
//    return arr[1][1].a;
//}
// Check Struct, done
//struct s1{
//    int a;
//    int b;
//};
//struct s2{
//    int a;
//    struct s1 b;
//};
//
//int f7(int a){
//    int b;
//    struct s1 ss;
//    struct s2 sss;
//
//    sss.b.a = a;
//    a = sss.b.a;
//    b = sss.b.a;
//
//    return b;
//}

// Check recursive, done
//int f8(int a){
//    int res;
//    if(a == 0){
//        res = 0;
//    }else{
//        res = f8(a - 1) + a;
//    }
//    return res;
//}

// Check NEG, done
//int f9(int a){
//    int b = -1;
//    a = -b;
//    return a;
//}

// Check NOT, done
//int f10(int a){
//    int b = !a;
//    return b;
//}
struct s1{
    int a;
    int b;
    int c[10];
};
struct s1 h[10];
int f1(int a, int b, int c) {
    int res =  a;
    h[2].c[2] = a;
    h[2].c[2] = h[2].c[2] + 1;
    res = h[2].c[2];
//    res = res * a / b % c;
    return res;
}
int main(int a){
    int array[5];
    int res = f1(4,3,3);
    return res;
}


//int f2() {
//    return 0;
//}
//
//int main() {
//    int a = 1;
//    struct s1 b;
//    float c, d, e;
//
//
//
//    while (-a != 1) {
//
//    }
//
//    f2();
//    f1(1,2,3);
//
//    for (a = 1;a > 1;a = a + 1;) {}
//
//    if (a > 1)
//        a = 1;
//    else
//        a = 0;
//
//    return b.a;
//}
