#import "com_codename1_testnatives_MyNativeInterfaceImpl.h"

@implementation com_codename1_testnatives_MyNativeInterfaceImpl

-(NSData*)getDouble{
    JAVA_ARRAY_DOUBLE out[] = {1, 2, 3, -1};
    return [NSData dataWithBytes:&out length:sizeof(out)];
}

-(NSData*)getBytes{
    JAVA_ARRAY_BYTE out[] = {1, 2, 3, -1};
    return [NSData dataWithBytes:&out length:sizeof(out)];
}

-(NSData*)setInts:(NSData*)param{
    int len = param.length/sizeof(JAVA_ARRAY_INT);
    JAVA_ARRAY_INT array[len];
    memcpy(&array, param.bytes, param.length);
    for(int loop = 0; loop < len; loop++) {
        printf("%d ", array[loop]);
    }
    return [NSData dataWithBytes:&array length:sizeof(array)];
}

-(NSData*)setDoubles:(NSData*)param{
    int len = param.length/sizeof(JAVA_ARRAY_DOUBLE);
    JAVA_ARRAY_DOUBLE array[len];
    memcpy(&array, param.bytes, param.length);
    for(int loop = 0; loop < len; loop++) {
        printf("%f ", array[loop]);
    }
    return [NSData dataWithBytes:&array length:sizeof(array)];
}

-(NSData*)getInts{
    JAVA_ARRAY_INT out[] = {1, 2, 3, -1};
    return [NSData dataWithBytes:&out length:sizeof(out)];
}

-(NSData*)setBytes:(NSData*)param{
    int len = param.length/sizeof(JAVA_ARRAY_BYTE);
    JAVA_ARRAY_BYTE array[len];
    memcpy(&array, param.bytes, param.length);
    for(int loop = 0; loop < len; loop++) {
        printf("%d ", array[loop]);
    }
    return [NSData dataWithBytes:&array length:sizeof(array)];
}

-(BOOL)isSupported{
    return YES;
}

@end
