// Check Function call, done
int f1(int a){
    return a;
}
// Check Binary op, done
int f2(int a){
    int res = a + 1;
    //res = a + 1
    res = res * 2;
    //res = 2*a +2
    res = res - 2;
    //res = 2*a
    res = res / 2;
    //res = a
    return res;
}
// Check if statement, done
int f3(int a){
    int res;
    if(a>1){
        res = 1;
    }else{
        res = 0;
    }
    return res;
}
// Check while statement, done
int f4(int a){
    int b = 0;
    while (a>0){
        a = a - 1;
        b = b + 1;
    }
    return b;
}
// Check for statement, done
int f5(int a){
    int i,b;
    b = 0;
    for(i = 0; i < a; i = i + 1;) {
        b = b + 1;
    }
    return b;
}
// Check Array, done
int arr[5];
int f6(int a){
    arr[1] = a;

    return arr[1];
}
// Check Struct, done
struct s1{
    int a;
    int b;
};

int f7(int a){
    int b;
    struct s1 ss;

    ss.a = a;
    a = ss.a;
    b = ss.a;
    return b;
}

// Check recursive, done
int f8(int a){
    int res;
    if(a == 0){
        res = 0;
    }else{
        res = f8(a - 1) + a;
    }
    return res;
}

// Check NEG, done
int f9(int a){
    int b = -1;
    a = -b;
    return a;
}

// Check NOT, done
int f10(int a){
    int b = !a;
    return b;
}

// Check Float, done
int main(int a){
    int array[5];
    int res = f1(4);
    float fp = 0.3;
    fp = fp * 9;
    if(fp >2.9){
        res = 1;
    }else{
        res = 0;
    }
    return res;
}